package com.example.demo.config;

import com.example.demo.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 将 Redis 传来的二进制消息转成字符串
        String msg = new String(message.getBody());
        log.info("【Redis订阅】成功收到底层消息: {}", msg);

        // 清理一下可能因为序列化产生的多余双引号
        msg = msg.replace("\"", "");

        if ("PROJECT_DATA_CHANGED".equals(msg)) {
            log.info("【Redis订阅】指令匹配成功，准备唤醒 WebSocket 广播给所有前端...");
            // 呼叫 WebSocket 群发！
            WebSocketServer.broadcast("PROJECT_DATA_CHANGED");
        }
    }
}