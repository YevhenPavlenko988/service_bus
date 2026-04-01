package com.github.yevhen.servicebus.proxy;

import com.github.yevhen.common.exception.ServiceException;
import com.github.yevhen.servicebus.model.RouteDefinition;
import com.github.yevhen.servicebus.security.PermissionEvaluator;
import java.io.IOException;
import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Enumeration;
import java.util.List;

@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private static final List<String> HOP_BY_HOP_HEADERS = List.of(
            "transfer-encoding", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization", "te", "trailer", "upgrade"
    );

    private final PermissionEvaluator permissionEvaluator;
    private final RestClient restClient;

    public ProxyController(PermissionEvaluator permissionEvaluator, RestClient restClient) {
        this.permissionEvaluator = permissionEvaluator;
        this.restClient = restClient;
    }

    @RequestMapping(value = "/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String queryString = request.getQueryString();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        log.debug("Gateway: {} {}", method, path);

        RouteDefinition route = permissionEvaluator.findRoute(path);
        permissionEvaluator.evaluate(route, method, path, authHeader);

        String targetUrl = route.getTarget() + path;
        if (queryString != null && !queryString.isBlank()) {
            targetUrl += "?" + queryString;
        }

        // Read raw body from input stream (works for JSON, binary, and multipart)
        byte[] body = null;
        try {
            body = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            log.warn("Failed to read request body: {}", e.getMessage());
        }

        return forward(method, targetUrl, copyRequestHeaders(request), body);
    }

    private ResponseEntity<byte[]> forward(String method, String targetUrl,
                                           HttpHeaders headers, byte[] body) {
        try {
            // Use URI.create() to preserve existing percent-encoding without double-encoding
            var spec = restClient
                    .method(HttpMethod.valueOf(method))
                    .uri(URI.create(targetUrl))
                    .headers(h -> h.addAll(headers));

            if (body != null && body.length > 0) {
                spec.body(body);
            }

            return spec.exchange((req, resp) -> {
                byte[] responseBody = resp.getBody().readAllBytes();
                HttpHeaders responseHeaders = filterResponseHeaders(resp.getHeaders());
                return ResponseEntity.status(resp.getStatusCode())
                        .headers(responseHeaders)
                        .body(responseBody);
            });

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Proxy error forwarding to {}: {}", targetUrl, e.getMessage());
            throw new ServiceException("Service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (!isHopByHop(name) && !name.equalsIgnoreCase("host")) {
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    headers.add(name, values.nextElement());
                }
            }
        }
        return headers;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders source) {
        HttpHeaders filtered = new HttpHeaders();
        source.forEach((name, values) -> {
            if (!isHopByHop(name)) {
                filtered.put(name, values);
            }
        });
        return filtered;
    }

    private boolean isHopByHop(String headerName) {
        return HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase());
    }
}
