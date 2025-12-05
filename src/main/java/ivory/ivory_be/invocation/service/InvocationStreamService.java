package ivory.ivory_be.invocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivory.ivory_be.invocation.domain.RunnerInvokeRequestDto;
import ivory.ivory_be.invocation.entity.Invocation;
import ivory.ivory_be.invocation.repository.InvocationRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvocationStreamService {

    @Value("${runner.url}")
    private String RUNNER_URL;

    private final RestClient restClient;

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

        startRunnerIfNeeded(invocationId);

        return emitter;
    }


    public void startRunnerIfNeeded(String invocationId) {
        Invocation inv = invocationRepository
                .findByInvocationId(invocationId)
                .orElseThrow(() -> new IllegalArgumentException("invocation not found"));

        // 이미 실행 중이거나 끝난 애면 다시 실행 X (재연결 / 여러 탭 보호)
        if (!"REQUEST_RECEIVED".equals(inv.getStatus())) {
            log.info("[Runner] already started. invocationId={}, status={}", invocationId, inv.getStatus());
            return;
        }

        // 여기서 Runner 호출에 필요한 데이터는 DB에서 꺼내옴
        RunnerInvokeRequestDto runnerReq = RunnerInvokeRequestDto.builder()
                .invocationId(inv.getInvocationId())
                .codeKey(inv.getS3Key())
                .runtime(inv.getRuntime())
                .handler(inv.getHandler())
                .payload(fromJson(inv.getPayload(), Map.class))
                .build();

        // Runner 호출
        sendInvocationToRunner(runnerReq);
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendInvocationToRunner(RunnerInvokeRequestDto req) {
        try {
            restClient.post()
                    .uri(RUNNER_URL + "/internal/invocations")
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
//            throw new RuntimeException("Runner 호출 실패", e);
            log.warn("Runner 요청 실패: {}", e.getMessage());
        }
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
