package com.neusoft.cloudbrain.service.ai.speech;

import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class AudioPcmUtils {

    private AudioPcmUtils() {
    }

    static byte[] toPcm16kMono(byte[] fileBytes, String filename) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("音频文件为空");
        }
        if (isWav(fileBytes)) {
            return extractWavPcm(fileBytes);
        }
        if (StringUtils.hasText(filename) && filename.toLowerCase().endsWith(".pcm")) {
            return fileBytes;
        }
        throw new IllegalArgumentException("请上传 16kHz 单声道 WAV 或 PCM 文件");
    }

    private static boolean isWav(byte[] data) {
        return data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
    }

    private static byte[] extractWavPcm(byte[] wav) {
        int offset = 12;
        Integer audioFormat = null;
        Integer channels = null;
        Integer sampleRate = null;
        Integer bitsPerSample = null;
        byte[] data = null;

        while (offset + 8 <= wav.length) {
            String chunkId = new String(wav, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
            int chunkSize = readInt32LE(wav, offset + 4);
            int chunkDataStart = offset + 8;
            if ("fmt ".equals(chunkId) && chunkSize >= 16) {
                audioFormat = readInt16LE(wav, chunkDataStart);
                channels = readInt16LE(wav, chunkDataStart + 2);
                sampleRate = readInt32LE(wav, chunkDataStart + 4);
                bitsPerSample = readInt16LE(wav, chunkDataStart + 14);
            } else if ("data".equals(chunkId)) {
                data = new byte[chunkSize];
                System.arraycopy(wav, chunkDataStart, data, 0, Math.min(chunkSize, wav.length - chunkDataStart));
            }
            offset = chunkDataStart + chunkSize + (chunkSize % 2);
        }

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("WAV 文件缺少音频数据");
        }
        if (audioFormat == null || audioFormat != 1) {
            throw new IllegalArgumentException("仅支持 PCM 编码的 WAV 文件");
        }
        if (bitsPerSample == null || bitsPerSample != 16) {
            throw new IllegalArgumentException("仅支持 16bit WAV 文件");
        }
        if (channels == null || channels < 1) {
            throw new IllegalArgumentException("WAV 声道信息无效");
        }

        byte[] mono = channels == 1 ? data : mixToMono(data, channels);
        if (sampleRate == null || sampleRate == 16000) {
            return mono;
        }
        return resampleTo16k(mono, sampleRate);
    }

    private static byte[] mixToMono(byte[] interleaved, int channels) {
        int frameCount = interleaved.length / (2 * channels);
        byte[] mono = new byte[frameCount * 2];
        ByteBuffer src = ByteBuffer.wrap(interleaved).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer dst = ByteBuffer.wrap(mono).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            int sum = 0;
            for (int ch = 0; ch < channels; ch++) {
                sum += src.getShort();
            }
            dst.putShort((short) (sum / channels));
        }
        return mono;
    }

    private static byte[] resampleTo16k(byte[] pcm, int sourceRate) {
        short[] samples = bytesToShorts(pcm);
        int targetLength = (int) Math.round(samples.length * 16000.0 / sourceRate);
        short[] target = new short[targetLength];
        for (int i = 0; i < targetLength; i++) {
            double srcIndex = i * (double) sourceRate / 16000.0;
            int idx = (int) srcIndex;
            double frac = srcIndex - idx;
            short a = samples[Math.min(idx, samples.length - 1)];
            short b = samples[Math.min(idx + 1, samples.length - 1)];
            target[i] = (short) Math.round(a + (b - a) * frac);
        }
        return shortsToBytes(target);
    }

    private static short[] bytesToShorts(byte[] pcm) {
        ByteBuffer buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
        short[] samples = new short[pcm.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = buffer.getShort();
        }
        return samples;
    }

    private static byte[] shortsToBytes(short[] samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : samples) {
            buffer.putShort(sample);
        }
        return buffer.array();
    }

    static byte[] wrapPcmAsWav(byte[] pcm) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        int byteRate = 16000 * 2;
        writeAscii(out, "RIFF");
        writeInt32LE(out, 36 + pcm.length);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeInt32LE(out, 16);
        writeInt16LE(out, (short) 1);
        writeInt16LE(out, (short) 1);
        writeInt32LE(out, 16000);
        writeInt32LE(out, byteRate);
        writeInt16LE(out, (short) 2);
        writeInt16LE(out, (short) 16);
        writeAscii(out, "data");
        writeInt32LE(out, pcm.length);
        out.writeBytes(pcm);
        return out.toByteArray();
    }

    private static int readInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static int readInt32LE(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeInt16LE(ByteArrayOutputStream out, short value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    private static void writeInt32LE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }
}
