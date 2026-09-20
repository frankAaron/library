package com.library.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Value("${alipay.appId:}")
    private String appId;

    @Value("${alipay.privateKey:}")
    private String privateKey;

    @Value("${alipay.publicKey:}")
    private String alipayPublicKey;

    @Value("${alipay.gateway:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gateway;

    @Value("${alipay.charset:UTF-8}")
    private String charset;

    @Value("${alipay.signType:RSA2}")
    private String signType;

    @Value("${alipay.format:json}")
    private String format;

    @Value("${alipay.domain:http://localhost:8080/library-ssm}")
    private String domain;

    public String getAppId() {
        return appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAlipayPublicKey() {
        return alipayPublicKey;
    }

    public String getGateway() {
        return gateway;
    }

    public String getCharset() {
        return charset;
    }

    public String getSignType() {
        return signType;
    }

    public String getFormat() {
        return format;
    }

    public String getDomain() {
        return domain;
    }

    public String getReturnUrl() {
        return domain + "/alipay/return";
    }

    public String getNotifyUrl() {
        return domain + "/alipay/notify";
    }

    public boolean isRealMode() {
        return appId != null && !appId.isEmpty()
                && privateKey != null && !privateKey.isEmpty()
                && alipayPublicKey != null && !alipayPublicKey.isEmpty();
    }
}