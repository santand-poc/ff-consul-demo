package com.mknieszner.ffconsul.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model wpisu z /v1/kv/... bez ?raw (Value = base64)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsulKvEntry {

    @JsonProperty("Key")
    private String key;

    @JsonProperty("Value")
    private String valueBase64;

    @JsonProperty("ModifyIndex")
    private long modifyIndex;

    // getters/setters

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValueBase64() { return valueBase64; }
    public void setValueBase64(String valueBase64) { this.valueBase64 = valueBase64; }

    public long getModifyIndex() { return modifyIndex; }
    public void setModifyIndex(long modifyIndex) { this.modifyIndex = modifyIndex; }
}
