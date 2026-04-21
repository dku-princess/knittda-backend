package com.example.knittdaserver.config;

import com.example.knittdaserver.filter.CustomSentryFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(
                "http://localhost:*", // 모든 포트의 localhost 허용
                "https://knittda-ai-feed.vercel.app",
                "https://knittda-ai-feed.vercel.app/*"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    @Bean
    public FilterRegistrationBean<CustomSentryFilter> customSentryFilter() {
        FilterRegistrationBean<CustomSentryFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomSentryFilter());
        registrationBean.addUrlPatterns("/api/v1/records/*", "/api/v1/projects/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
