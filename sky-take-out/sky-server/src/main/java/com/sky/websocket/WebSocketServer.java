package com.sky.websocket;

import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {
    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap();
    //存放客户端类型，key为sid，value为clientType（1:商家，2:用户）
    private static Map<String, Integer> clientTypeMap = new HashMap();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        System.out.println("客户端：" + sid + "建立连接");
        sessionMap.put(sid, session);
        // 解析sid，获取客户端类型（格式：type_userId，如1_1表示商家ID为1）
        if (sid.contains("_")) {
            String[] parts = sid.split("_");
            try {
                int clientType = Integer.parseInt(parts[0]);
                clientTypeMap.put(sid, clientType);
            } catch (NumberFormatException e) {
                // 默认为用户类型
                clientTypeMap.put(sid, 2);
            }
        } else {
            // 默认为用户类型
            clientTypeMap.put(sid, 2);
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自客户端：" + sid + "的信息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("连接断开:" + sid);
        sessionMap.remove(sid);
        clientTypeMap.remove(sid);
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 向特定类型的客户端发送消息
     * @param message 消息内容
     * @param clientType 客户端类型：1-商家，2-用户
     */
    public void sendToClientType(String message, int clientType) {
        for (Map.Entry<String, Integer> entry : clientTypeMap.entrySet()) {
            if (entry.getValue() == clientType) {
                String sid = entry.getKey();
                Session session = sessionMap.get(sid);
                if (session != null && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 向商家发送消息
     * @param message 消息内容
     */
    public void sendToMerchants(String message) {
        sendToClientType(message, 1);
    }

    /**
     * 向用户发送消息
     * @param message 消息内容
     */
    public void sendToUsers(String message) {
        sendToClientType(message, 2);
    }
}
