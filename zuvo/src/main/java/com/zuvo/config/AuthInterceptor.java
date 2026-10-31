package com.zuvo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/** Blocks every /api request unless the user is logged in (session has "uid"). */
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if ("OPTIONS".equals(req.getMethod())) return true;
        HttpSession s = req.getSession(false);
        Long uid = s == null ? null : (Long) s.getAttribute("uid");
        if (uid == null) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Please log in to continue.\"}");
            return false;
        }
        req.setAttribute("uid", uid);
        return true;
    }
}
