package com.dentalclinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class VnPayConfig {

    @Value("${vnpay.tmn-code:DENTAL01}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret:DENTALCARE_VNPAY_SECRET_KEY_2026_ENTERPRISE}")
    private String vnp_HashSecret;

    @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.return-url:http://localhost:8080/api/payments/vnpay-callback}")
    private String vnp_ReturnUrl;

    public String getVnp_TmnCode() {
        return vnp_TmnCode;
    }

    public void setVnp_TmnCode(String vnp_TmnCode) {
        this.vnp_TmnCode = vnp_TmnCode;
    }

    public String getVnp_HashSecret() {
        return vnp_HashSecret;
    }

    public void setVnp_HashSecret(String vnp_HashSecret) {
        this.vnp_HashSecret = vnp_HashSecret;
    }

    public String getVnp_Url() {
        return vnp_Url;
    }

    public void setVnp_Url(String vnp_Url) {
        this.vnp_Url = vnp_Url;
    }

    public String getVnp_ReturnUrl() {
        return vnp_ReturnUrl;
    }

    public void setVnp_ReturnUrl(String vnp_ReturnUrl) {
        this.vnp_ReturnUrl = vnp_ReturnUrl;
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
