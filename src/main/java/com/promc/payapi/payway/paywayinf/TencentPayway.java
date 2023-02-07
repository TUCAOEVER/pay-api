package com.promc.payapi.payway.paywayinf;

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
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TencentPayway implements Payway {

    protected static final Pattern COMPILE = Pattern.compile("<code_url>.*</code_url>");

    /**
     * 微信公众平台中创建的公众号/小程序的APPID
     * QQ钱包不需要
     */
    protected final String appId;

    /**
     * 微信/QQ钱包商户平台中的商户号
     */
    protected final String merchantId;

    /**
     * 在微信/QQ钱包商户平台由自己上传的随机32位私钥
     */
    protected final String merchantKey;

    protected TencentPayway(String appId, String merchantId, String merchantKey) {
        this.appId = appId;
        this.merchantId = merchantId;
        this.merchantKey = merchantKey;
    }

    /**
     * 获取统一下单接口Url
     *
     * @return 统一下单接口Url
     */
    protected abstract String getUnifiedOrderUrl();

    @Override
    @Nullable
    public String nativePay(@NotNull Order order) {
        // 参数要排序 这里手动排序用个有序的Map
        Map<String, Object> params = new LinkedHashMap<>();
        if (appId != null && !appId.isEmpty()) {
            params.put("appid", appId);
        }
        params.put("body", order.getSubject());
        params.put("fee_type", "CNY");
        params.put("mch_id", merchantId);
        params.put("nonce_str", UUID.randomUUID().toString().replace("-", ""));
        params.put("notify_url", PayAPI.getAPI().getNotifyUrl() + "/" + getIdentifier());
        params.put("out_trade_no", order.getId());
        // 这里最好是传入玩家IP 但是挺麻烦的其实
        params.put("spbill_create_ip", "127.0.0.1");
        params.put("total_fee", order.getTotalFee().multiply(new BigDecimal(100)).intValue());
        params.put("trade_type", "NATIVE");

        // 拼接参数
        String paramsString = String.join("&", params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new)
        );
        // 参数最后要放上 商户密钥 进行md5加密 得到签名
        String sign = md5(paramsString + "&key=" + merchantKey);
        // 最后把签名也插入进去
        params.put("sign", sign);

        LoggerUtil.debug("正在请求URL: "+ getUnifiedOrderUrl());
        String body = HttpUtil.post(getUnifiedOrderUrl(), mapToXml(params));
        LoggerUtil.debug("返回结果: "+ body);

        Matcher matcher = COMPILE.matcher(body);
        if (matcher.find()) {
            String s = matcher.group();
            return s.substring(19, s.length() - 14);
        }
        return null;
    }

    @Override
    public void notify(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request) {
        String body = request.content().toString(StandardCharsets.UTF_8);
        Map<String, String> params = xmlToMap(body);

        if (verify(params)) {
            if ("SUCCESS".equals(params.get("trade_state")) || "SUCCESS".equals(params.get("return_code"))) {
                PayAPI.getAPI().markPay(params);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        request.protocolVersion(),
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer("<xml>" +
                                "<return_code>SUCCESS</return_code>" +
                                "<return_msg>OK</return_msg>" +
                                "</xml>", StandardCharsets.UTF_8)
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 将xml字符串转为Map
     *
     * @param xml xml
     * @return map
     */
    @NotNull
    public Map<String, String> xmlToMap(@NotNull String xml) {
        try {
            // 其实我想用 正则解析的... xml不好用
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document document = documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
            Element element = document.getDocumentElement();
            NodeList childNodes = element.getChildNodes();
            Map<String, String> map = new HashMap<>(childNodes.getLength());
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                map.put(node.getNodeName(), node.getTextContent());
            }
            map.remove("#text");
            return map;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 验证
     *
     * @param params 参数
     * @return 验证成功
     */
    public boolean verify(@NotNull Map<String, String> params) {
        String sign = params.remove("sign");
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder content = new StringBuilder();

        for (String key : keys) {
            content.append(key).append("=").append(params.get(key)).append("&");
        }
        if (content.length() > 0) {
            content.deleteCharAt(content.length() - 1);
            content.append("&key=").append(merchantKey);
        }
        String localSign = md5(content.toString());
        return sign.equals(localSign);
    }

    /**
     * MD5加密
     *
     * @param content 内容
     * @return 密文
     */
    public static String md5(String content) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            byte[] digest = md5.digest(content.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, digest).toString(16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("不支持的加密方式", e);
        }
    }

    /**
     * 将MAP转成XML
     *
     * @return XML字符串
     */
    public static String mapToXml(Map<String, Object> map) {
        // 简单的拼接一下字符串即可
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<xml>");
        map.forEach((k, v) -> stringBuilder
                .append('<').append(k).append('>')
                .append(v)
                .append("</").append(k).append('>')
        );
        stringBuilder.append("</xml>");

        return stringBuilder.toString();
    }
}
