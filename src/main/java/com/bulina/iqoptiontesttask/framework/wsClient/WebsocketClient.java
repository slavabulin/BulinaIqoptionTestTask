package com.bulina.iqoptiontesttask.framework.wsClient;

import com.bulina.iqoptiontesttask.framework.dataModel.Message;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.websocket.*;

@ClientEndpoint
public class WebsocketClient {

    public enum SNIFFER_MODE {
        ALL_SYNC("unknown"),
        SYNC_AFTER_AUTH("timeSync");

        private final String id;

        SNIFFER_MODE(String id) {
            this.id = id;
        }
    }

    private static final String wsApiHost = "wss://iqoption.com/echo/websocket";

    @Getter
    private AtomicBoolean isAuthorized = new AtomicBoolean(false);

    @Getter
    private final ConcurrentLinkedQueue<Message> syncMsgQueue = new ConcurrentLinkedQueue<>();

    @Getter
    private final SNIFFER_MODE mode;

    private Session session = null;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    @OnOpen
    public void onOpen(Session session) throws Exception {
        logger.info("Opening websocket...");
        this.session = session;
        logger.info("Websocket opened at:" + new Date().getTime());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("Message from ws: " + message);
        Message msg = Message.Builder().buildFromString(message);

        if (msg == null) {
            logger.info(String.format("Message is null"));

        } else if (msg.getType() != Message.MESSAGE_TYPE.SYNC) {
            logger.info(String.format("Message was not queued, message type is %s, queue size %s",
                    msg.getType(), syncMsgQueue.size()));

        } else if ((mode == SNIFFER_MODE.SYNC_AFTER_AUTH) && !isAuthorized.get()) {
            logger.info(String.format("SyncMessage was not queued, sync before auth: %s, queue size %s",
                    msg.getInitial(), syncMsgQueue.size()));
        } else {
            syncMsgQueue.add(msg);
            System.out.println(String.format("SyncMessage added to he queue: %s, queue size %s",
                    msg.getInitial(), syncMsgQueue.size()));
        }

        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(msg);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s close because of %s at %s", session.getId(), closeReason, new Date().getTime()));
    }

    public boolean isSessionOpen() {
        boolean status = false;
        if (session != null) {
            status = session.isOpen();
        }
        return status;
    }


    public void sendMessage(String message) {
        logger.info(message);
        if (session != null && session.isOpen()) {
            this.session.getAsyncRemote().sendText(message);
            logger.info("SyncMessage send at: " + new Date().getTime());
        } else {
            logger.info("Session was closed. SyncMessage wasn't send");
        }
    }

    public void sendMessage(Message message) {
        sendMessage(message.toJsonString());
    }

    public void sendAuthMessage(Message message) {
        sendMessage(message);
        isAuthorized.getAndSet(true);
    }

    public void closeConnection() throws IOException {
        logger.info(String.format("Closing session ..."));
        if (session != null) {
            session.close();
        }
        logger.info("Session closed at: " + new Date().getTime());
        this.session = null;
    }


    private MessageHandler messageHandler;

    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public static interface MessageHandler {
        public void handleMessage(Message msg);
    }


    public WebsocketClient(SNIFFER_MODE mode) {
        try {
            URI serverEndpointUri = new URI(wsApiHost);
            this.mode = mode;
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, serverEndpointUri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}