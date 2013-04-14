package me.itzgeoff.vidsync.domain.client;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.context.annotation.Profile;

@Entity
@Profile("client")
public class LocalClientProperty implements Serializable {
    
    public static final String CLIENT_ID_KEY = "UUID";

    private static final long serialVersionUID = 1L;
    
    @Id
    private String key;
    
    private String value;

    public LocalClientProperty() {
    }

    public LocalClientProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
