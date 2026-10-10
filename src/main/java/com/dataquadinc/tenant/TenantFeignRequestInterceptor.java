package com.dataquadinc.tenant;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantFeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        template.header(TenantContext.HEADER_NAME, TenantContext.getTenantId());
    }
}
