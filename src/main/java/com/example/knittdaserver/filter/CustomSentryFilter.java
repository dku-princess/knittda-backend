package com.example.knittdaserver.filter;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

public class CustomSentryFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String contentType = httpRequest.getContentType();

        if (contentType != null && contentType.contains("multipart/form-data")) {
            SentryEvent sentryEvent = new SentryEvent();
            Request sentryRequest = new Request();
            sentryRequest.setMethod(httpRequest.getMethod());
            sentryRequest.setUrl(httpRequest.getRequestURL().toString());
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, httpRequest.getHeader(headerName));
            }
            sentryRequest.setHeaders(headers);

            Map<String, Object> requestData = new HashMap<>();

            try {
                String project = httpRequest.getParameter("project");
                String record = httpRequest.getParameter("record");
                String updateRecordJson = httpRequest.getParameter("updateRecordJson");
                String[] deleteImageIds = httpRequest.getParameterValues("deleteImageIds");

                if (project != null) {
                    requestData.put("project", project);
                }
                if (record != null) {
                    requestData.put("record", record);
                }
                if (updateRecordJson != null) {
                    requestData.put("updateRecordJson", updateRecordJson);
                }
                if (deleteImageIds != null) {
                    requestData.put("deleteImageIds", Arrays.asList(deleteImageIds));
                }

                sentryRequest.setData(requestData);
                sentryEvent.setRequest(sentryRequest);

            } catch (Exception e) {
                Sentry.captureException(e);
            }

            Sentry.captureEvent(sentryEvent);
        }

        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            Sentry.captureException(ex);
            throw ex;
        }
    }
}
