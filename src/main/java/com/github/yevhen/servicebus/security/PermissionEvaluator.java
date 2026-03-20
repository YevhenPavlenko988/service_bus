package com.github.yevhen.servicebus.security;

import com.github.yevhen.common.exception.ServiceException;
import com.github.yevhen.common.security.CallerInfo;
import com.github.yevhen.common.security.JwtHelper;
import com.github.yevhen.servicebus.config.GatewayProperties;
import com.github.yevhen.servicebus.model.RouteDefinition;
import com.github.yevhen.servicebus.model.RuleDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Optional;

@Component
public class PermissionEvaluator {

    private final GatewayProperties props;
    private final JwtHelper jwtHelper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PermissionEvaluator(GatewayProperties props, JwtHelper jwtHelper) {
        this.props = props;
        this.jwtHelper = jwtHelper;
    }

    public RouteDefinition findRoute(String path) {
        return props.getRoutes().stream()
                .filter(r -> path.startsWith(r.getPrefix()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(
                        "No route configured for path: " + path, HttpStatus.NOT_FOUND));
    }

    /**
     * Finds the first matching rule, validates JWT if not public, checks role.
     * Returns CallerInfo if authenticated, empty if public route.
     */
    public Optional<CallerInfo> evaluate(RouteDefinition route, String method, String path, String authHeader) {
        RuleDefinition matchedRule = route.getRules().stream()
                .filter(rule -> methodMatches(rule, method) && pathMatches(rule, path))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Access denied", HttpStatus.FORBIDDEN));

        if (matchedRule.isPublic()) {
            return Optional.empty();
        }

        CallerInfo caller = jwtHelper.extractCallerInfo(authHeader);

        if (matchedRule.getRoles() != null && !matchedRule.getRoles().isEmpty()) {
            if (!matchedRule.getRoles().contains(caller.role())) {
                throw new ServiceException("Access denied for role: " + caller.role(), HttpStatus.FORBIDDEN);
            }
        }

        return Optional.of(caller);
    }

    private boolean methodMatches(RuleDefinition rule, String method) {
        return rule.getMethods().contains("*") || rule.getMethods().contains(method.toUpperCase());
    }

    private boolean pathMatches(RuleDefinition rule, String path) {
        return rule.getPaths().stream()
                .anyMatch(pattern -> pattern.equals(path) || pathMatcher.match(pattern, path));
    }
}
