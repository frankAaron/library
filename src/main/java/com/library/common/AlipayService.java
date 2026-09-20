package com.library.common;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayService.class);

    @Autowired
    private AlipayConfig config;

    private volatile AlipayClient client;

    private AlipayClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new DefaultAlipayClient(
                            config.getGateway(),
                            config.getAppId(),
                            config.getPrivateKey(),
                            config.getFormat(),
                            config.getCharset(),
                            config.getAlipayPublicKey(),
                            config.getSignType()
                    );
                }
            }
        }
        return client;
    }

    public String tradePagePay(String outTradeNo, String totalAmount, String subject, String body) {
        if (config.isRealMode()) {
            return realTradePagePay(outTradeNo, totalAmount, subject, body);
        }
        return mockTradePagePay(outTradeNo, totalAmount, subject, body);
    }

    private String realTradePagePay(String outTradeNo, String totalAmount, String subject, String body) {
        try {
            AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
            req.setReturnUrl(config.getReturnUrl());
            req.setNotifyUrl(config.getNotifyUrl());

            Map<String, String> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("total_amount", totalAmount);
            bizContent.put("subject", subject);
            bizContent.put("body", body);
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            String json = new ObjectMapper().writeValueAsString(bizContent);
            req.setBizContent(json);

            AlipayTradePagePayResponse resp = getClient().pageExecute(req);
            if (resp.isSuccess()) {
                log.info("[支付宝] 真沙箱模式生成支付页成功: outTradeNo={}, total={}", outTradeNo, totalAmount);
                return resp.getBody();
            } else {
                log.warn("[支付宝] 真沙箱调用失败，降级到 mock: {}", resp.getSubMsg());
                return mockTradePagePay(outTradeNo, totalAmount, subject, body);
            }
        } catch (Exception e) {
            log.warn("[支付宝] 真沙箱调用异常，降级到 mock: {}", e.getMessage());
            return mockTradePagePay(outTradeNo, totalAmount, subject, body);
        }
    }

    private String mockTradePagePay(String outTradeNo, String totalAmount, String subject, String body) {
        log.info("[支付宝] Mock 模式生成模拟支付页: outTradeNo={}, total={}", outTradeNo, totalAmount);
        String returnUrl = config.getReturnUrl() + "?out_trade_no=" + outTradeNo
                + "&total_amount=" + totalAmount + "&trade_status=TRADE_SUCCESS";
        return """
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>模拟支付宝收银台</title>
<style>
  body{margin:0;background:#f5f5f5;font-family:PingFang SC,Microsoft YaHei,sans-serif;}
  .wrap{max-width:520px;margin:40px auto;background:#fff;border-radius:8px;box-shadow:0 4px 24px rgba(0,0,0,.08);padding:32px;}
  .logo{font-size:22px;font-weight:600;color:#1677ff;margin-bottom:8px;}
  .logo span{color:#1677ff;}
  .sub{color:#888;font-size:13px;margin-bottom:24px;}
  .box{border-top:1px solid #eee;border-bottom:1px solid #eee;padding:20px 0;margin-bottom:24px;}
  .row{display:flex;justify-content:space-between;margin:8px 0;font-size:14px;}
  .row .k{color:#666;}
  .amount{font-size:36px;font-weight:700;color:#ff4d4f;margin:16px 0 4px;}
  .yuan{font-size:16px;font-weight:400;margin-right:4px;}
  .mock-tip{background:#fff7e6;border:1px solid #ffd591;color:#d46b08;border-radius:6px;padding:10px 14px;font-size:13px;margin-bottom:20px;}
  .btn-pay{display:block;width:100%;height:48px;background:#1677ff;color:#fff;border:none;border-radius:6px;font-size:16px;cursor:pointer;font-weight:600;}
  .btn-pay:hover{background:#0958d9;}
  .btn-cancel{display:block;width:100%;height:36px;background:transparent;color:#888;border:none;font-size:13px;cursor:pointer;margin-top:12px;}
</style></head><body>
<div class="wrap">
  <div class="logo"><span>支付宝</span> Alipay</div>
  <div class="sub">安全支付 / Sandbox Mock</div>
  <div class="box">
    <div class="amount"><span class="yuan">¥</span>%s</div>
    <div class="row"><span class="k">商品名称</span><span>%s</span></div>
    <div class="row"><span class="k">商户订单号</span><span style="font-family:monospace;font-size:12px;">%s</span></div>
    <div class="row"><span class="k">订单描述</span><span style="max-width:260px;text-align:right;">%s</span></div>
  </div>
  <div class="mock-tip">⚠ 当前为模拟支付模式（未配置支付宝沙箱密钥），点击"确认付款"将直接完成支付。</div>
  <button class="btn-pay" onclick="pay()">确认付款</button>
  <button class="btn-cancel" onclick="cancel()">取消支付</button>
</div>
<script>
  function pay(){ window.location.href = "%s"; }
  function cancel(){ window.close(); history.back(); }
</script></body></html>
                """.formatted(totalAmount, subject, outTradeNo, body, returnUrl);
    }

    public boolean verifySign(Map<String, String> params) {
        if (!config.isRealMode()) {
            log.warn("[支付宝] Mock 模式跳过签名校验，生产环境必须验证！");
            return true;
        }
        if (params == null || params.isEmpty()) {
            return false;
        }
        TreeMap<String, String> sorted = new TreeMap<>(params);
        try {
            boolean ok = AlipaySignature.rsaCheckV1(sorted, config.getAlipayPublicKey(),
                    config.getCharset(), config.getSignType());
            if (!ok) {
                log.warn("[支付宝] 签名校验失败");
            }
            return ok;
        } catch (AlipayApiException e) {
            log.error("[支付宝] 签名校验异常", e);
            return false;
        }
    }
}