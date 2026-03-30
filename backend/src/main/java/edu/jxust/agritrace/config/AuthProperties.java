package edu.jxust.agritrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret = "TraceabilityJwtSecretKeyForAdminLogin1234567890";
    private long tokenExpireHours = 12;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireHours() {
        return tokenExpireHours;
    }

    public void setTokenExpireHours(long tokenExpireHours) {
        this.tokenExpireHours = tokenExpireHours;
    }
}
