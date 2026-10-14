package com.skr1l.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
public class VisionConfig {

  @Bean
  public RestClient visionRestClient(
    @Value("${vision.base-url}") String baseUrl) {

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofMinutes(3));

    return RestClient.builder()
      .baseUrl(baseUrl)
      .requestFactory(factory)
      .build();
  }
}
