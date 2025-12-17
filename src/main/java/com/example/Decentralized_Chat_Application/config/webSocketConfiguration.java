package com.example.Decentralized_Chat_Application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.example.Decentralized_Chat_Application.config.signalling.SignallingServer;

@EnableWebSocket
@Configuration
public class webSocketConfiguration implements WebSocketConfigurer {
    private final SignallingServer signallingServer;
    public webSocketConfiguration(SignallingServer signallingServer){
        this.signallingServer = signallingServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signallingServer, "/ws/signalling");
    }

}
