package com.library.controller;

import com.library.common.AlipayConfig;
import com.library.common.AlipayService;
import com.library.common.Result;
import com.library.controller.UserController;
import com.library.entity.User;
import com.library.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/alipay")
public class AlipayController {

    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);

    @Autowired
    private AccountService accountService;

    /**
     * 通用下单入口：前端传入 type + 业务参数
     * type = fine   → 罚款缴纳，需要 fineId
     * type = deposit → 押金充值，需要 amount
     * 返回支付宝支付页面 HTML（前端 document.write 即可跳转）
     */
    @PostMapping("/create")
    public Result create(@RequestBody Map<String, Object> param,
                         HttpSession session) {
        User user = (User) session.getAttribute(UserController.SESSION_USER);
        if (user == null) {
            return Result.fail("请先登录");
        }
        String type = (String) param.get("type");
        if (type == null || type.isBlank()) {
            return Result.fail("支付类型不能为空");
        }

        String outTradeNo;
        String amount;
        String subject;
        String body;

        if ("fine".equals(type)) {
            Long fineId = Long.valueOf(param.get("fineId").toString());
            Result check = accountService.prepareFinePay(user.getId(), fineId);
            if (!check.isSuccess()) {
                return check;
            }
            BigDecimal fineAmount = (BigDecimal) check.getData();
            outTradeNo = "FINE_" + user.getId() + "_" + fineId + "_" + System.currentTimeMillis();
            amount = fineAmount.toPlainString();
            subject = "超期罚款缴纳";
            body = "读者ID:" + user.getId() + " 罚款ID:" + fineId;

        } else if ("deposit".equals(type)) {
            BigDecimal amt = new BigDecimal(param.get("amount").toString());
            Result check = accountService.prepareDepositPay(amt);
            if (!check.isSuccess()) {
                return check;
            }
            outTradeNo = "DEP_" + user.getId() + "_" + System.currentTimeMillis();
            amount = amt.toPlainString();
            subject = "图书馆押金充值";
            body = "读者ID:" + user.getId() + " 押金充值";

        } else {
            return Result.fail("未知的支付类型: " + type);
        }

        log.info("[支付宝] 创建订单: type={}, outTradeNo={}, amount={}, userId={}",
                type, outTradeNo, amount, user.getId());

        String payHtml = AlipayService.tradePagePay(outTradeNo, amount, subject, body);
        Map<String, Object> data = new HashMap<>();
        data.put("payHtml", payHtml);
        data.put("outTradeNo", outTradeNo);
        data.put("amount", amount);
        data.put("mockMode", !AlipayConfig.isRealMode());
        return Result.ok(data);
    }

    /**
     * 同步回调 —— 用户付完款跳回这里
     * 真正的生产环境应该校验签名 + 查单确认，本地教学环境简化处理
     */
    @GetMapping("/return")
    public void returnUrl(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String outTradeNo = req.getParameter("out_trade_no");
        String totalAmount = req.getParameter("total_amount");
        String tradeStatus = req.getParameter("trade_status");
        String tradeNo = req.getParameter("trade_no");

        log.info("[支付宝] 同步回调: outTradeNo={}, totalAmount={}, tradeStatus={}, tradeNo={}",
                outTradeNo, totalAmount, tradeStatus, tradeNo);

        String msg;
        boolean success = false;
        try {
            if (outTradeNo != null && outTradeNo.startsWith("FINE_")) {
                String[] parts = outTradeNo.split("_");
                Long userId = Long.valueOf(parts[1]);
                Long fineId = Long.valueOf(parts[2]);
                Result r = accountService.confirmFinePaid(userId, fineId, outTradeNo, tradeNo);
                msg = r.getMsg();
                success = r.isSuccess();
            } else if (outTradeNo != null && outTradeNo.startsWith("DEP_")) {
                String[] parts = outTradeNo.split("_");
                Long userId = Long.valueOf(parts[1]);
                BigDecimal amt = new BigDecimal(totalAmount);
                Result r = accountService.confirmDepositPaid(userId, amt, outTradeNo, tradeNo);
                msg = r.getMsg();
                success = r.isSuccess();
            } else {
                msg = "未知的订单类型: " + outTradeNo;
            }
        } catch (Exception e) {
            log.error("[支付宝] 回调处理异常", e);
            msg = "支付回调处理失败: " + e.getMessage();
        }

        String redirect = AlipayConfig.DOMAIN + "/alipay/result?success=" + success
                + "&msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8)
                + "&outTradeNo=" + (outTradeNo == null ? "" : URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8))
                + "&amount=" + (totalAmount == null ? "" : URLEncoder.encode(totalAmount, StandardCharsets.UTF_8));
        resp.sendRedirect(redirect);
    }

    /**
     * 支付结果展示页
     */
    @GetMapping("/result")
    public void result(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String success = req.getParameter("success");
        String msg = req.getParameter("msg");
        String outTradeNo = req.getParameter("outTradeNo");
        String amount = req.getParameter("amount");
        boolean ok = "true".equals(success);

        String html = """
<!DOCTYPE html><html><head><meta charset="UTF-8"><title>支付结果</title>
<style>
  body{margin:0;background:#f5f5f5;font-family:PingFang SC,Microsoft YaHei,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;}
  .box{max-width:440px;width:90%%;background:#fff;border-radius:8px;box-shadow:0 4px 24px rgba(0,0,0,.08);padding:36px;text-align:center;}
  .icon{width:72px;height:72px;border-radius:50%%;margin:0 auto 16px;display:flex;align-items:center;justify-content:center;font-size:40px;color:#fff;}
  .ok .icon{background:#52c41a;}
  .fail .icon{background:#ff4d4f;}
  .title{font-size:20px;font-weight:600;margin-bottom:12px;}
  .detail{background:#fafafa;border-radius:6px;padding:14px;text-align:left;margin-bottom:20px;font-size:13px;color:#666;}
  .detail .row{display:flex;justify-content:space-between;margin:4px 0;}
  .detail .row .v{color:#333;font-family:monospace;}
  .btn{display:inline-block;padding:10px 28px;background:#1677ff;color:#fff;text-decoration:none;border-radius:6px;font-size:14px;}
  .btn:hover{background:#0958d9;}
  .tip{color:#999;font-size:12px;margin-top:16px;}
</style></head><body>
<div class="box %s">
  <div class="icon">%s</div>
  <div class="title">%s</div>
  <div class="detail">
    <div class="row"><span>订单号</span><span class="v">%s</span></div>
    <div class="row"><span>金额</span><span class="v">¥%s</span></div>
    <div class="row"><span>状态</span><span class="v">%s</span></div>
  </div>
  <a href="%s" class="btn">返回我的借阅</a>
  <div class="tip">%s</div>
</div></body></html>
                """.formatted(
                ok ? "ok" : "fail",
                ok ? "✓" : "✕",
                ok ? "支付成功" : "支付未完成",
                outTradeNo == null ? "-" : outTradeNo,
                amount == null ? "-" : amount,
                msg == null ? (ok ? "交易已完成" : "交易未成功") : msg,
                AlipayConfig.DOMAIN + "/borrow/toMyBorrow",
                AlipayConfig.isRealMode() ? "支付宝真实沙箱模式" : "Mock 模拟支付模式"
        );
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter pw = resp.getWriter();
        pw.write(html);
        pw.flush();
        pw.close();
    }
}