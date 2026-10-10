package com.dataquadinc.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContext.setTenantId(resolveTenantId(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private static String resolveTenantId(HttpServletRequest request) {
        String fromHeader = request.getHeader(TenantContext.HEADER_NAME);
        if (isPresent(fromHeader)) {
            return normalize(fromHeader);
        }
        String fromQuery = request.getParameter("tenant");
        if (isPresent(fromQuery)) {
            return normalize(fromQuery);
        }
        String fromHost = TenantResolver.fromHost(request.getServerName());
        if (fromHost != null) {
            return normalize(fromHost);
        }
        return TenantContext.DEFAULT_TENANT;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String raw) {
        String code = raw.trim().toLowerCase();
        if (TenantResolver.AVENTRA.equals(code) || TenantContext.DEFAULT_TENANT.equals(code)) {
            return code;
        }
        return TenantContext.DEFAULT_TENANT;
    }
}
