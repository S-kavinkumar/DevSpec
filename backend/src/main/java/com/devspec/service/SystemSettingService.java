package com.devspec.service;

import com.devspec.model.SystemSetting;
import com.devspec.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
public class SystemSettingService {
    private static final Logger logger = LoggerFactory.getLogger(SystemSettingService.class);

    private final SystemSettingRepository settingRepository;

    @Autowired
    @Lazy
    @Qualifier("analysisTaskExecutor")
    private Executor analysisTaskExecutor;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String getSetting(String key, String defaultValue) {
        return settingRepository.findById(key)
                .map(SystemSetting::getValueContent)
                .orElse(defaultValue);
    }

    public int getSettingInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getSetting(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Map<String, String> getAllSettings() {
        List<SystemSetting> list = settingRepository.findAll();
        Map<String, String> settings = new HashMap<>();
        for (SystemSetting s : list) {
            settings.put(s.getKeyName(), s.getValueContent());
        }
        return settings;
    }

    public void updateSetting(String key, String value) {
        SystemSetting setting = SystemSetting.builder()
                .keyName(key)
                .valueContent(value)
                .build();
        settingRepository.save(setting);
        logger.info("Saved setting: {} = {}", key, value);

        // Apply setting side-effects dynamically
        if ("logging_level".equalsIgnoreCase(key)) {
            applyLoggingLevel(value);
        } else if ("thread_pool_size".equalsIgnoreCase(key)) {
            try {
                int size = Integer.parseInt(value);
                applyThreadPoolSize(size);
            } catch (NumberFormatException e) {
                logger.warn("Invalid thread pool size: {}", value);
            }
        }
    }

    private void applyLoggingLevel(String levelStr) {
        try {
            ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger comLogger = loggerContext.getLogger("com.devspec");
            comLogger.setLevel(ch.qos.logback.classic.Level.toLevel(levelStr, ch.qos.logback.classic.Level.INFO));
            logger.info("Com.devspec package logging level dynamically switched to {}", levelStr);
        } catch (Throwable t) {
            logger.warn("Could not dynamically update logging level. Reason: {}", t.getMessage());
        }
    }

    private void applyThreadPoolSize(int size) {
        if (analysisTaskExecutor instanceof ThreadPoolTaskExecutor poolExecutor) {
            poolExecutor.setCorePoolSize(size);
            poolExecutor.setMaxPoolSize(size * 2);
            logger.info("ThreadPool core-size dynamically scaled to {}, max-size to {}", size, size * 2);
        }
    }
}
