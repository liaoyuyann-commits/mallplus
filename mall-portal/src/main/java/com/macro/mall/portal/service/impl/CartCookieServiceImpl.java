package com.macro.mall.portal.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macro.mall.portal.domain.CartMergeItem;
import com.macro.mall.portal.service.CartCookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 访客购物车 Cookie 实现。
 */
@Service
public class CartCookieServiceImpl implements CartCookieService {

    private static final String COOKIE_NAME = "mallplus_cart_guest";
    private static final int COOKIE_MAX_BYTES = 3 * 1024;
    private static final int COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<CartMergeItem> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return new ArrayList<>();
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
                    return objectMapper.readValue(decoded, new TypeReference<List<CartMergeItem>>() { });
                } catch (Exception ignored) {
                    return new ArrayList<>();
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void write(HttpServletResponse response, List<CartMergeItem> items) {
        try {
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    objectMapper.writeValueAsString(items).getBytes(StandardCharsets.UTF_8));
            if (encoded.getBytes(StandardCharsets.UTF_8).length > COOKIE_MAX_BYTES) {
                throw new IllegalArgumentException("guest cart cookie exceeds 3KB");
            }
            Cookie cookie = new Cookie(COOKIE_NAME, encoded);
            cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
            cookie.setPath("/");
            response.addCookie(cookie);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize guest cart cookie", exception);
        }
    }

    @Override
    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
