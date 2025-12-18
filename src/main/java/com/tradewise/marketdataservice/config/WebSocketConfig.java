package com.tradewise.marketdataservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final AuthHandshakeInterceptor authHandshakeInterceptor; // Inject the interceptor

  @Autowired // Autowire the interceptor
  public WebSocketConfig(AuthHandshakeInterceptor authHandshakeInterceptor) {
    this.authHandshakeInterceptor = authHandshakeInterceptor;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
      .setAllowedOriginPatterns("http://localhost:3000") // allow dev origin
      .withSockJS()
      .setInterceptors(authHandshakeInterceptor); // Register the interceptor
  }
}
