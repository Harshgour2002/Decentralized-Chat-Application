package com.example.Decentralized_Chat_Application.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.Decentralized_Chat_Application.signalling.SignallingServer;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/chat")
public class chatController {

    private final SignallingServer signalingHandler;

    public chatController(SignallingServer signalingHandler) {
        this.signalingHandler = signalingHandler;
    }

    @GetMapping("/online-users")
    public List<String> onlineUsers() {
        return signalingHandler.getOnlineUsers();
    }

    @GetMapping("/status")
    public ObjectNode status() {
        return signalingHandler.getUsersStatus();
    }
}