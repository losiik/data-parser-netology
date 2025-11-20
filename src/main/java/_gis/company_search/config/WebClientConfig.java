package _gis.company_search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient dgClient() {
        return WebClient.builder()
                .baseUrl("https://catalog.api.2gis.com/3.0")
                .build();
    }
}