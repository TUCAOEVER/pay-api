package com.promc.payapi.payway;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.promc.payapi.PayAPI;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.api.payway.Payway;
import com.promc.payapi.util.HttpUtil;
import com.promc.payapi.util.LoggerUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 支付宝 支付方式
 */
public class AliPayway implements Payway {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 支付宝网关
     */
    private static final String ALIPAY_GATEWAY = "https://openapi.alipay.com/gateway.do";

    /**
     * 支付宝开放平台中创建应用的APPID
     */
    private final String appId;

    /**
     * 支付宝商户平台中交换商户公钥即可获得支付宝公钥
     */
    private final String alipayPublicKey;

    /**
     * 商户私钥 与自己创建的商户公钥对应的商户私钥
     */
    private final String merchantPrivateKey;

    public AliPayway(String appId, String alipayPublicKey, String merchantPrivateKey) {
        this.appId = appId;
        this.alipayPublicKey = alipayPublicKey;
        this.merchantPrivateKey = merchantPrivateKey;
    }

    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("不支持的编码方式", e);
        }
    }

    public static String urlDecode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("不支持的编码方式", e);
        }
    }

    /**
     * RSA2签名算法
     *
     * @param content    内容
     * @param privateKey 密钥
     * @return 密文
     */
    @NotNull
    public static String rsaSign(@NotNull String content, @NotNull String privateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.getDecoder().decode(privateKey.getBytes(StandardCharsets.UTF_8));
            PrivateKey priKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initSign(priKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("不支持的加密方式", e);
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new IllegalArgumentException("无效的密钥", e);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("签名异常", e);
        }
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "alipay";
    }

    @Override
    public String nativePay(@NotNull Order order) {
        String time = DATE_FORMAT.format(System.currentTimeMillis());

        Map<String, Object> bizContent = new HashMap<>(3);
        bizContent.put("out_trade_no", order.getId());
        bizContent.put("total_amount", order.getTotalFee());
        bizContent.put("subject", order.getSubject());

        // 参数要排序 这里手动排序用个有序的Map
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", appId);
        params.put("biz_content", PayAPI.GSON.toJson(bizContent));
        params.put("charset", "UTF-8");
        params.put("method", "alipay.trade.precreate");
        params.put("notify_url", PayAPI.getAPI().getNotifyUrl() + "/" + getIdentifier());
        params.put("sign_type", "RSA2");
        params.put("timestamp", time);
        params.put("version", "1.0");

        // 拼接参数
        String paramsString = String.join("&", params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new)
        );

        String sign = rsaSign(paramsString, merchantPrivateKey);
        params.put("sign", sign);

        String join = String.join("&", params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + urlEncode(e.getValue()))
                .toArray(String[]::new)
        );

        LoggerUtil.debug("真正请求URL: " + ALIPAY_GATEWAY);
        String body = HttpUtil.post(ALIPAY_GATEWAY + "?" + join);
        LoggerUtil.debug("请求结果: " + body);

        try {
            JsonObject jsonObject = PayAPI.GSON.fromJson(body, JsonObject.class);
            JsonObject response = jsonObject.getAsJsonObject("alipay_trade_precreate_response");
            return response.get("qr_code").getAsString();
        } catch (JsonSyntaxException | NullPointerException e) {
            return null;
        }
    }

    @Override
    public void notify(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request) {
        String body = request.content().toString(StandardCharsets.UTF_8);
        String[] args = body.split("&");
        Map<String, String> params = new HashMap<>(args.length);
        for (String arg : args) {
            String[] split = arg.split("=");
            params.put(split[0], urlDecode(split[1]));
        }
        String sign = params.get("sign");
        String content = getSignCheckContent(params);

        // 签名验证 避免伪造请求
        boolean verify = verify(content, sign);
        if (verify) {
            PayAPI.getAPI().markPay(params);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                request.protocolVersion(),
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(verify ? "success" : "failure", StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 获取需要验证的内容
     *
     * @param params 所有的参数
     * @return 需要验证的内容
     */
    @NotNull
    public String getSignCheckContent(@NotNull Map<String, String> params) {
        // 去掉这两个属性后 排序好 再拼接
        params.remove("sign");
        params.remove("sign_type");
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder content = new StringBuilder();

        for (String key : keys) {
            content.append(key).append("=").append(params.get(key)).append("&");
        }
        if (content.length() > 0) {
            content.deleteCharAt(content.length() - 1);
        }

        return content.toString();
    }

    /**
     * 验证
     *
     * @param content 需要验证的内容
     * @param sign    签名
     * @return 是否通过
     */
    public boolean verify(@NotNull String content, @NotNull String sign) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.getDecoder().decode(alipayPublicKey.getBytes());
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("签名验证出错", e);
        }
    }
}
