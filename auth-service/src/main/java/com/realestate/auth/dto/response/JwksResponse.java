package com.realestate.auth.dto.response;

import java.util.List;
import java.util.Map;

public class JwksResponse {

    private List<Map<String, Object>> keys;

    public JwksResponse() {}

    public JwksResponse(List<Map<String, Object>> keys) {
        this.keys = keys;
    }

    public List<Map<String, Object>> getKeys() { return keys; }
    public void setKeys(List<Map<String, Object>> keys) { this.keys = keys; }
}
