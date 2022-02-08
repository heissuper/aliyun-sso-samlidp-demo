package com.aliyun.sso.samldemo.configuration;

/**
 * IOT 环境类
 */
public enum Env {
    /**
     * 线上地址
     */
    ONLINE("https://signin.aliyun.com/saml/SSO", "https://signin.aliyun.com/1610384436873353/saml/SSO");

    String acsUrl;
    String audience;

    Env(String acsUrl, String audience) {
        this.acsUrl = acsUrl;
        this.audience = audience;
    }


    public String getAcsUrl() {
        return acsUrl;
    }

    public String getAudience() {
        return audience;
    }

}
