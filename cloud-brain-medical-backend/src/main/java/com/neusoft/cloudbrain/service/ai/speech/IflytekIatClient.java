package com.neusoft.cloudbrain.service.ai.speech;

import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import com.neusoft.cloudbrain.config.SpeechProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IflytekIatClient {

    private final SpeechProperties speechProperties;

    public String transcribe(byte[] pcm16kMono) throws Exception {
        if (pcm16kMono == null || pcm16kMono.length == 0) {
            throw new IllegalArgumentException("音频内容为空");
        }
        SpeechProperties.Iflytek cfg = speechProperties.getIflytek();
        if (!StringUtils.hasText(cfg.getAppId())
                || !StringUtils.hasText(cfg.getApiKey())
                || !StringUtils.hasText(cfg.getApiSecret())) {
            throw new IllegalStateException("科大讯飞 APPID / APIKey / APISecret 未配置");
        }

        CompletableFuture<String> finished = new CompletableFuture<>();
        StringBuilder transcript = new StringBuilder();
        IatClient[] clientHolder = new IatClient[1];

        AbstractIatWebSocketListener listener = new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                if (iatResponse.getCode() != 0) {
                    finished.completeExceptionally(new IllegalStateException(
                            iatResponse.getMessage() + " (code=" + iatResponse.getCode() + ")"));
                    closeQuietly(clientHolder[0]);
                    return;
                }
                appendTranscript(iatResponse, transcript);
                if (iatResponse.getData() != null && iatResponse.getData().getStatus() == 2) {
                    finished.complete(transcript.toString().trim());
                    closeQuietly(clientHolder[0]);
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                finished.completeExceptionally(new IllegalStateException(formatFail(t, response)));
                closeQuietly(clientHolder[0]);
            }
        };

        IatClient client = new IatClient.Builder()
                .signature(cfg.getAppId(), cfg.getApiKey(), cfg.getApiSecret())
                .hostUrl("http://" + cfg.getHost() + cfg.getPath())
                .vad_eos(5000)
                .proxy(Proxy.NO_PROXY)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .build();
        clientHolder[0] = client;

        log.debug("Connecting iFlytek IAT, appId={}", mask(cfg.getAppId()));
        client.send(new ByteArrayInputStream(pcm16kMono), listener);

        String text = finished.get(90, TimeUnit.SECONDS);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("语音识别未返回有效文本");
        }
        return text;
    }

    private static void appendTranscript(IatResponse iatResponse, StringBuilder transcript) {
        if (iatResponse.getData() == null || iatResponse.getData().getResult() == null) {
            return;
        }
        IatResult result = iatResponse.getData().getResult();
        if (result.getWs() == null) {
            return;
        }
        for (IatResult.Ws word : result.getWs()) {
            if (word.getCw() == null || word.getCw().length == 0) {
                continue;
            }
            IatResult.Cw item = word.getCw()[0];
            if (item != null && StringUtils.hasText(item.getW())) {
                transcript.append(item.getW());
            }
        }
    }

    private static String formatFail(Throwable t, Response response) {
        StringBuilder message = new StringBuilder();
        if (t != null && StringUtils.hasText(t.getMessage())) {
            message.append(t.getMessage());
        }
        if (response != null) {
            if (message.length() > 0) {
                message.append("; ");
            }
            message.append("HTTP ").append(response.code());
            try {
                if (response.body() != null) {
                    String body = response.body().string();
                    if (StringUtils.hasText(body)) {
                        message.append(" ").append(body.trim());
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return message.length() > 0 ? message.toString() : "讯飞 WebSocket 连接失败";
    }

    private static void closeQuietly(IatClient client) {
        if (client != null) {
            try {
                client.closeWebsocket();
            } catch (Exception ignored) {
            }
        }
    }

    private static String mask(String value) {
        if (!StringUtils.hasText(value) || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 4) + "****";
    }
}
