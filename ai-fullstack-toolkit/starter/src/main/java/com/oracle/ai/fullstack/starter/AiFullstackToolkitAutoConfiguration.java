package com.oracle.ai.fullstack.starter;

import com.oracle.ai.fullstack.registry.ToolRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** Adds the common registry to an existing Spring Boot application. */
@AutoConfiguration
public class AiFullstackToolkitAutoConfiguration {
    @Bean public ToolRegistry toolRegistry() { return new ToolRegistry(); }
}
