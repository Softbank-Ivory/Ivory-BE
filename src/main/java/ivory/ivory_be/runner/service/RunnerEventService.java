package ivory.ivory_be.runner.service;

import ivory.ivory_be.invocation.service.InvocationStreamService;
import ivory.ivory_be.runner.domain.RunnerEventMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunnerEventService {

    private final InvocationStreamService invocationStreamService;

    public void handle(RunnerEventMessageDto msg) {

        String invocationId = msg.getInvocationId();
        RunnerEventMessageDto.Payload p = msg.getPayload();
        log.debug("Handling Type: {} invocationId: {}", msg.getType(), msg.getInvocationId());

        switch (msg.getType()) {
            case STATUS -> handleStatus(invocationId, p);
            case LOG -> handleLog(invocationId, p);
            case COMPLETE -> handleComplete(invocationId, p);
        }
    }

    private void handleStatus(String invocationId, RunnerEventMessageDto.Payload p) {
        String phase = p.getStatus();
        invocationStreamService.sendStatus(invocationId, phase);
    }

    private void handleLog(String invocationId, RunnerEventMessageDto.Payload p) {
        String line = p.getLine();
        invocationStreamService.sendLog(invocationId, line);
    }

    private void handleComplete(String invocationId, RunnerEventMessageDto.Payload p) {
        String finalStatus = p.getStatus();
        long durationMs = p.getDurationMs();

        // COMPLETED
        if ("COMPLETED".equalsIgnoreCase(finalStatus)) {
            RunnerEventMessageDto.Result result = p.getResult();
            if (result != null) {
                invocationStreamService.sendCompleteSuccess(
                        invocationId,
                        durationMs,
                        result.getStatusCode(),
                        result.getBody()
                );
            }
        } else { // FAILED
            invocationStreamService.sendCompleteFailed(
                    invocationId,
                    durationMs,
                    p.getErrorMessage()
            );
        }
    }
}
