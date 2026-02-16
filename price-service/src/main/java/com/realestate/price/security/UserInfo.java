package com.realestate.price.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private String id;
    private String email;
    private String name;
    private String rolesString;

    public List<String> getRoles() {
        if (rolesString == null || rolesString.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(rolesString.split(","));
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public boolean isAuthenticated() {
        return id != null && !id.isEmpty();
    }

    public String getFullName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return email;
    }
}
