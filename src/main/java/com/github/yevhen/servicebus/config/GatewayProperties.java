package com.github.yevhen.servicebus.config;

import com.github.yevhen.servicebus.model.RouteDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<RouteDefinition> routes = new ArrayList<>();

    public List<RouteDefinition> getRoutes() { return routes; }
    public void setRoutes(List<RouteDefinition> routes) { this.routes = routes; }
}
