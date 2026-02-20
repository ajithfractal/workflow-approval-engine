package com.fractalhive.workflowcore.tenant;

import com.fractalhive.workflowcore.tenant.SchemaRoutingDataSource;
import com.fractalhive.workflowcore.tenant.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenancyConfig {

    private final SchemaRoutingDataSource schemaRoutingDataSource;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.fractalhive.workflowcore");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(hibernateProperties());

        return em;
    }

    private Map<String, Object> hibernateProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", schemaRoutingDataSource);
        properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        properties.put("hibernate.show_sql", false);
        properties.put("hibernate.format_sql", true);
        return properties;
    }
}
