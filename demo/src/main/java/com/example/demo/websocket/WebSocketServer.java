package com.example.demo.websocket;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint("/ws/project") // 这里的路径必须和前端 new WebSocket 的路径对应
@Slf4j
public class WebSocketServer {

    // 线程安全集合，用来存放当前连接的所有客户端 Session
    private static final CopyOnWriteArraySet<Session> sessionPool = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        sessionPool.add(session);
        log.info("【WebSocket】新用户接入，当前在线人数：{}", sessionPool.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessionPool.remove(session);
        log.info("【WebSocket】用户断开连接，当前在线人数：{}", sessionPool.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("【WebSocket】发生错误", error);
    }

    /**
     * 群发消息核心方法
     */
    public static void broadcast(String message) {
        log.info("【WebSocket】准备向 {} 个客户端广播消息：{}", sessionPool.size(), message);
        for (Session session : sessionPool) {
            try {
                if (session.isOpen()) {
                    // 异步或同步发送均可，这里用同步发送简单粗暴
                    session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                log.error("【WebSocket】广播消息失败", e);
            }
        }
    }
}