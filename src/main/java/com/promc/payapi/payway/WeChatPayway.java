package com.promc.payapi.payway;

import com.promc.payapi.payway.paywayinf.TencentPayway;
import org.jetbrains.annotations.NotNull;

public class WeChatPayway extends TencentPayway {

    public WeChatPayway(String appId, String merchantId, String merchantKey) {
        super(appId, merchantId, merchantKey);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wxpay";
    }

    @Override
    public String getUnifiedOrderUrl() {
        return "https://api.mch.weixin.qq.com/pay/unifiedorder";
    }
}
