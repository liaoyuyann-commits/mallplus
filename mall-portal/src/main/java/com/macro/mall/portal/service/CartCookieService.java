package com.macro.mall.portal.service;

import com.macro.mall.portal.domain.CartMergeItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 未登录购物车 Cookie 服务。
 * 为什么存在：访客没有 memberId，先把最小购物车字段保存在浏览器，登录后再提交合并。
 */
public interface CartCookieService {

    List<CartMergeItem> read(HttpServletRequest request);

    void write(HttpServletResponse response, List<CartMergeItem> items);

    void clear(HttpServletResponse response);
}
