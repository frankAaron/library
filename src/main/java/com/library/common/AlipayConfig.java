package com.library.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AlipayConfig.class);

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

    @Value("${alipay.domain:http://localhost:8080/library}")
    private String domain;

    @Override
    public void afterPropertiesSet() {
        log.info("[支付宝配置] appId={}, privateKey长度={}, publicKey长度={}, gateway={}, domain={}, isRealMode={}",
                (appId == null || appId.isEmpty()) ? "<EMPTY>" : appId,
                privateKey == null ? 0 : privateKey.length(),
                alipayPublicKey == null ? 0 : alipayPublicKey.length(),
                gateway, domain, isRealMode());
    }

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