package com.github.yevhen.servicebus.model;

import java.util.List;

public class RuleDefinition {

    private List<String> methods = List.of("*");
    private List<String> paths = List.of("/**");
    private List<String> roles;
    private boolean isPublic;

    public List<String> getMethods() { return methods; }
    public void setMethods(List<String> methods) { this.methods = methods; }

    public List<String> getPaths() { return paths; }
    public void setPaths(List<String> paths) { this.paths = paths; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
}
