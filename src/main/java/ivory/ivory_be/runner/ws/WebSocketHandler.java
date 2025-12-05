package ivory.ivory_be.runner.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivory.ivory_be.runner.domain.RunnerEventMessageDto;
import ivory.ivory_be.runner.service.RunnerEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final RunnerEventService eventService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connected: sessionId={}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket disconnected: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String raw = message.getPayload();
            RunnerEventMessageDto msg = new ObjectMapper().readValue(raw, RunnerEventMessageDto.class);

            log.info("[Runner WS] Received message DTO: {}", new ObjectMapper().writeValueAsString(msg));

            eventService.handle(msg);
        } catch (Exception e) {
            log.error("WebSocket message processing error: {}. Payload: {}", e.getMessage(), message.getPayload(), e);
        }
    }
}
