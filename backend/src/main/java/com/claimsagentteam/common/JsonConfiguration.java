package com.claimsagentteam.common;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfiguration {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder.postConfigurer(objectMapper -> {
            var textualCoercion = objectMapper.coercionConfigFor(LogicalType.Textual);
            textualCoercion.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            textualCoercion.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            textualCoercion.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}
