package com.example.knittdaserver.config;

import com.example.knittdaserver.filter.CustomSentryFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<CustomSentryFilter> customSentryFilter() {
        FilterRegistrationBean<CustomSentryFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomSentryFilter());
        registrationBean.addUrlPatterns("/api/v1/records/*", "/api/v1/projects/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
