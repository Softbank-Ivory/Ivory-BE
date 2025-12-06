package ivory.ivory_be.invocation.controller;

import ivory.ivory_be.invocation.domain.RunnerMetricsDto;
import ivory.ivory_be.invocation.service.RunnerMetricsService;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RunnerMetricsController {

    private final RunnerMetricsService runnerMetricsService;

    private static final long TIMEOUT = 0L;
    private static final long INTERVAL_MS = 1000*60;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping(value = "/api/runner/metrics/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRunnerMetrics() {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        executor.submit(() -> {
            try {
                while (true) {
                    RunnerMetricsDto metrics = runnerMetricsService.getCurrentMetrics();

                    if (metrics != null) {
                        emitter.send(
                                SseEmitter.event()
                                        .name("METRICS")
                                        .data(metrics)
                        );
                    }

                    Thread.sleep(INTERVAL_MS);
                }
            } catch (IOException e) {
                log.warn("[RunnerMetrics] SSE send error: {}", e.getMessage());
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("[RunnerMetrics] Unexpected error", e);
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.info("[RunnerMetrics] SSE completed"));
        emitter.onTimeout(() -> {
            log.info("[RunnerMetrics] SSE timeout");
            emitter.complete();
        });
        emitter.onError(e -> log.warn("[RunnerMetrics] SSE error: {}", e.getMessage()));

        return emitter;
    }

}
