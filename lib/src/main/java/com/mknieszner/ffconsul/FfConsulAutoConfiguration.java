package com.mknieszner.ffconsul;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(FfConsulProperties.class)
@ConditionalOnClass(FeatureFlags.class)
@ConditionalOnProperty(prefix = "ff.consul", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FfConsulAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FeatureFlags.class)
    public FeatureFlags featureFlags(FfConsulProperties props) {
        return new ConsulFeatureFlags(props);
    }
}
