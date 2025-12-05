package ivory.ivory_be.invocation.service;

import ivory.ivory_be.invocation.repository.InvocationRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvocationStreamService {

    private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

    private final InvocationRepository invocationRepository;

    public SseEmitter streamInvocation(String invocationId) {

        SseEmitter emitter = new SseEmitter(0L);

        emitterMap
                .computeIfAbsent(invocationId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(invocationId, emitter));
        emitter.onTimeout(() -> removeEmitter(invocationId, emitter));
        emitter.onError(e->{
            log.warn("SSE error for invocationId={}", invocationId, e);
            removeEmitter(invocationId, emitter);
        });

        return emitter;
    }

    private void removeEmitter(String invocationId, SseEmitter emitter) {
        List<SseEmitter> emitters = emitterMap.get(invocationId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterMap.remove(invocationId);
        }
    }

    private void sendEvent(String invocationId, String eventName, Object data) {
        List<SseEmitter> emitters = emitterMap.get(invocationId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name(eventName)
                        .data(data);

                emitter.send(event);
            } catch (IOException e) {
                emitter.completeWithError(e);
                removeEmitter(invocationId, emitter);
            }
        }
    }

    // Websocket 에서 호출하는 함수들

    // STATUS 이벤트: CODE_FETCHING / SANDBOX_PREPARING / EXECUTING
    public void sendStatus(String invocationId, String status) {
        // DB 업데이트
        invocationRepository.updateStatusByInvocationId(invocationId, status, LocalDateTime.now());
        log.info("status!!: {}",status);
        Map<String, Object> payload = Map.of("status", status);
        sendEvent(invocationId, "STATUS", payload);
    }

    // LOG 이벤트: 함수 실행 중 로그 한 줄 (EXECUTING 진행중)
    public void sendLog(String invocationId, String line) {
        Map<String, Object> payload = Map.of("line", line);
        sendEvent(invocationId, "LOG", payload);
    }

    // COMPLETE (SUCCESS)
    public void sendCompleteSuccess(String invocationId, long durationMs, int statusCode,
            String body) {
        // DB 업데이트
        invocationRepository.updateStatusByInvocationId(invocationId, "COMPLETED",
                LocalDateTime.now());

        Map<String, Object> result = Map.of("statusCode", statusCode, "body", body);

        Map<String, Object> payload = Map.of("status", "COMPLETED", "durationMs", durationMs,
                "result", result);

        sendEvent(invocationId, "COMPLETE", payload);
        complete(invocationId);
    }

    // COMPLETE (FAILED)
    public void sendCompleteFailed(String invocationId, long durationMs, String errorMessage) {
        // DB 업데이트
        invocationRepository.updateStatusByInvocationId(invocationId, "FAILED",
                LocalDateTime.now());

        Map<String, Object> payload = Map.of("status", "FAILED",
                "durationMs", durationMs,
                "errorMessage", errorMessage);

        sendEvent(invocationId, "COMPLETE", payload);
        complete(invocationId);
    }

    private void complete(String invocationId) {
        List<SseEmitter> emitters = emitterMap.remove(invocationId);
        if (emitters != null) {
            emitters.forEach(SseEmitter::complete);
        }
    }


}
