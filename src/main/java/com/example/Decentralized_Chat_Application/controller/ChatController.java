package com.example.Decentralized_Chat_Application.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Decentralized_Chat_Application.config.signalling.SignallingServer;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/chat")
public class chatController {
    private final SignallingServer signallingServer;
    public chatController(SignallingServer signallingServer){
        this.signallingServer = signallingServer;
    }

    @GetMapping("/getUser")
    public List<String> getOnlineUsers() {
        return signallingServer.getOnlineUsers();
    }

    @GetMapping("/status")
    public ObjectNode getUsersStatus() {
        return signallingServer.getUsersStatus();
    }    
    
}
