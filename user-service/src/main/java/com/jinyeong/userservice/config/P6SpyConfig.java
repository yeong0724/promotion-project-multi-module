package com.jinyeong.userservice.config;

import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().setLogMessageFormat(
                P6SpyPrettySqlFormatter.class.getName()
        );
    }

    public static class P6SpyPrettySqlFormatter implements MessageFormattingStrategy {
        @Override
        public String formatMessage(int connectionId, String now, long elapsed,
                                    String category, String prepared, String sql, String url) {
            if (sql == null || sql.trim().isEmpty()) {
                return "";
            }
            String formatted = FormatStyle.BASIC.getFormatter().format(sql);
            return "\n" + formatted + "\n[" + category + "] " + elapsed + "ms";
        }
    }
}
