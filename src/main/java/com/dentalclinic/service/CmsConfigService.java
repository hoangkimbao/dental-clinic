package com.dentalclinic.service;

import com.dentalclinic.dto.CmsConfigDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CmsConfigService {

    private final AtomicReference<CmsConfigDto> configStore = new AtomicReference<>(CmsConfigDto.createDefault());

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    public CmsConfigDto getCmsConfig() {
        return configStore.get();
    }

    public CmsConfigDto updateCmsConfig(CmsConfigDto newConfig, String updatedBy) {
        if (newConfig == null) {
            throw new IllegalArgumentException("Cấu hình CMS không được để trống!");
        }

        newConfig.setUpdatedAt(LocalDateTime.now());
        newConfig.setUpdatedBy(updatedBy != null ? updatedBy : "ADMIN");
        configStore.set(newConfig);

        // Broadcast realtime update to notification hub & desktop apps
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/notifications", java.util.Map.of(
                    "type", "CMS_CONFIG_UPDATED",
                    "title", "🛠️ CẬP NHẬT CẤU HÌNH CMS",
                    "message", "Cấu hình Menu & Footer phòng khám vừa được cập nhật bởi " + newConfig.getUpdatedBy() + ".",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }

        return newConfig;
    }

    public CmsConfigDto resetToDefault() {
        CmsConfigDto defaultConfig = CmsConfigDto.createDefault();
        configStore.set(defaultConfig);
        return defaultConfig;
    }
}
