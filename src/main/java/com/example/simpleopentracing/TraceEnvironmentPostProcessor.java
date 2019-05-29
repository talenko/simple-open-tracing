package com.example.simpleopentracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Order(Ordered.LOWEST_PRECEDENCE)
public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        List<String> whitelistedMdcKeys = new ArrayList<>();

        // Only when property defined in `bootstrap.yaml` does the environment return a value for the following
        // property string, it does not get loaded if defined only in   application.yaml`.
        whitelistedMdcKeys.add(environment.getProperty("spring.sleuth.log.slf4j.whitelisted-mdc-keys[0]"));

        Map<String, Object> map = new HashMap<>();
        // This doesn't work with all logging systems but it's a useful default so you see
        // traces in logs without having to configure it.
        if (Boolean
                .parseBoolean(environment.getProperty("spring.sleuth.enabled", "true"))) {
            map.put("logging.pattern.level", "%5p [${spring.zipkin.service.name:"
                    + "${spring.application.name:-}},%X{X-B3-TraceId:-},%X{X-B3-SpanId:-},%X{X-Span-Export:-}"
                    + (whitelistedMdcKeys.get(0) != null ? ",%X{" + whitelistedMdcKeys.get(0) + ":-}" : "")
                    + "]");
        }
        addOrReplace(environment.getPropertySources(), map);
    }

    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}