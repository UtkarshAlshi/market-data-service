package com.tradewise.marketdataservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
      .allowedOrigins("http://localhost:3000") // Explicitly allow your frontend origin
      .allowedMethods("*")
      .allowedHeaders("*")
      .allowCredentials(true); // Needed if using cookies or Authorization header
  }
}
