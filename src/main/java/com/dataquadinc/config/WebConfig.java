package com.dataquadinc.config;


import com.dataquadinc.tenant.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    public WebConfig(TenantInterceptor tenantInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // Allow CORS for all endpoints
                .allowedOrigins(
                        "http://35.188.150.92",  // First IP
                        "http://192.168.0.140:3000",  // Second IP
                        "http://192.168.0.139:3000", // Third IP
                        "https://mymulya.com", // Forth IP
                        "https://portal.aventrainc.ai",
                        "http://localhost:3000", // Fifth IP
                        "http://192.168.0.135/",
                        "http://192.168.0.135:80","http://182.18.177.16:443",
                        "http://mymulya.com:443",
                        "http://localhost/",
                        "http://154.210.288.26",
                        "http://192.168.0.203:3000",
                        "http://192.168.0.167:3000" // Sixth IP
                )

                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Allowed HTTP methods
                .allowedHeaders("*")                      // Allow all headers
                .allowCredentials(true);                  // Allow credentials (cookies, headers, etc.)
    }
}