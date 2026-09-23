package com.keycloak.oauth2_demo.config.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod().name();
        String uri = request.getURI().toString();

        log.info("[HTTP-OUT ▶ RestTemplate] {} {}", method, uri);

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[HTTP-OUT ◀ RestTemplate] {} {} | Status: {} | Duration: {}ms",
                    method, uri, response.getStatusCode(), duration);
            return response;
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[HTTP-OUT ✖ RestTemplate] {} {} | Error: {} | Duration: {}ms",
                    method, uri, e.getMessage(), duration);
            throw e;
        }
    }
}
