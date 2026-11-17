package com.neusoft.cloudbrain.config;

import com.neusoft.cloudbrain.common.constant.ApiConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /** OpenAPI 安全方案名；实际 HTTP 头发送为 Authorization */
    public static final String AUTH_SCHEME = "Authorization";

    private static final String[] PUBLIC_PATHS = {
            ApiConstants.BASE_URL + "/auth/login"
    };

    private static final String[] PUBLIC_GET_PREFIXES = {
            ApiConstants.BASE_URL + "/departments",
            ApiConstants.BASE_URL + "/doctors",
            ApiConstants.BASE_URL + "/schedules"
    };

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("东软智慧云脑诊疗平台 API")
                        .description("需登录接口：先「统一登录」，再在 Authorize 的 Authorization 中填写：Bearer {data.token}")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("完整格式：Bearer 空格 token，例如 Bearer eyJhbGci...")))
                .addSecurityItem(new SecurityRequirement().addList(AUTH_SCHEME));
    }

    @Bean
    public OpenApiCustomizer authenticatedOperationsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            SecurityRequirement requirement = new SecurityRequirement().addList(AUTH_SCHEME);
            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (!requiresAuthentication(path, method) || operation.getSecurity() != null) {
                        return;
                    }
                    operation.addSecurityItem(requirement);
                });
            });
        };
    }

    private static boolean requiresAuthentication(String path, PathItem.HttpMethod method) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath)) {
                return false;
            }
        }
        if (method == PathItem.HttpMethod.GET) {
            for (String prefix : PUBLIC_GET_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return false;
                }
            }
        }
        return path.startsWith(ApiConstants.BASE_URL + "/");
    }
}
