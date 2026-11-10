package com.dentalclinic.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("DentalCare Luxury RESTful API & AI Clinical Ecosystem")
                        .version("2.0.0")
                        .description("Tài liệu đặc tả OpenAPI 3.0 cho Hệ thống Quản Trị Phòng Khám Nha Khoa Thông Minh (Inventory, VNPay Gateway, AI X-Ray Analysis, EMR, Appointments & RBAC).")
                        .contact(new Contact().name("Hoàng Kim Bảo & Trương Hoàng Gia Bảo - STU").email("dev@dentalcare.vn"))
                        .license(new License().name("Apache 2.0")))
                .tags(List.of(
                        new Tag().name("Dental Materials & Inventory").description("Kịch bản 1: Quản lý kho vật tư tiêu hao nha khoa, cảnh báo tồn kho và hết hạn"),
                        new Tag().name("Payments & VNPay Gateway").description("Kịch bản 2: Cổng thanh toán trực tuyến VNPay HMAC-SHA512 & IPN Webhook Callback"),
                        new Tag().name("Dental X-Ray AI Analysis").description("Kịch bản 3: Trợ lý AI phân tích ảnh X-quang nha khoa tích hợp NineRouterAiClient")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
