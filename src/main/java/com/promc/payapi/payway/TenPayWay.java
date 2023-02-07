package com.promc.payapi.payway;

import com.promc.payapi.payway.paywayinf.TencentPayway;
import org.jetbrains.annotations.NotNull;

public class TenPayWay extends TencentPayway {

    public TenPayWay(String appId, String merchantId, String merchantKey) {
        super(appId, merchantId, merchantKey);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "tenpay";
    }

    @Override
    public String getUnifiedOrderUrl() {
        return "https://qpay.qq.com/cgi-bin/pay/qpay_unified_order.cgi";
    }
}
