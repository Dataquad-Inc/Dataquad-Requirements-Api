package com.dataquadinc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TenantBusinessBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantBusinessBackfill.class);

    private final JdbcTemplate jdbcTemplate;

    public TenantBusinessBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        backfill("requirements_model");
        backfill("bdm_client");
    }

    private void backfill(String table) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE " + table + " SET tenant_id = 'mymulya' WHERE tenant_id IS NULL OR tenant_id = ''");
            if (updated > 0) {
                log.info("Backfilled tenant_id=mymulya on {} rows in {}", updated, table);
            }
        } catch (Exception e) {
            log.warn("Could not backfill tenant_id on {} (column may not exist yet): {}", table, e.getMessage());
        }
    }
}
