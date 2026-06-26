package com.coduel.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded media from the filesystem and raises the multipart limits for image uploads. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Covers images (≤6 MB, enforced in MediaApi) and voice notes (≤12 MB) — the per-type guards are
    // in MediaApi; this is just the servlet ceiling, set above the larger of the two.
    private static final long MAX_FILE_BYTES = 13L * 1024 * 1024;
    private static final long MAX_REQUEST_BYTES = 14L * 1024 * 1024; // headroom for the multipart envelope

    @Autowired
    private AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = appProperties.getUploadsDir();
        String location = "file:" + (dir.endsWith("/") ? dir : dir + "/");
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // (tempLocation, maxFileSize, maxRequestSize, fileSizeThreshold)
        return new MultipartConfigElement("", MAX_FILE_BYTES, MAX_REQUEST_BYTES, 0);
    }
}
