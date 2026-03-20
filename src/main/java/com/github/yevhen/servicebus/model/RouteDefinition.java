package com.github.yevhen.servicebus.model;

import java.util.ArrayList;
import java.util.List;

public class RouteDefinition {

    private String prefix;
    private String target;
    private List<RuleDefinition> rules = new ArrayList<>();

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public List<RuleDefinition> getRules() { return rules; }
    public void setRules(List<RuleDefinition> rules) { this.rules = rules; }
}
