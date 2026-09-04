package com.erp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Jackson 配置：统一金额精度，杜绝浮点误差外泄到前端。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.modules(new JavaTimeModule());
            com.fasterxml.jackson.databind.module.SimpleModule module =
                    new com.fasterxml.jackson.databind.module.SimpleModule();
            module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
            builder.modules(module);
        };
    }

    /**
     * 金额统一保留 2 位小数输出（四舍五入）。
     */
    static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        }
    }
}
