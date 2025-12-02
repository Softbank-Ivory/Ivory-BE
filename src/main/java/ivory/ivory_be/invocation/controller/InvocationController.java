package ivory.ivory_be.invocation.controller;

import ivory.ivory_be.invocation.domain.InvocationRequestDto;
import ivory.ivory_be.invocation.domain.InvocationResponseDto;
import ivory.ivory_be.invocation.service.InvocationService;
import ivory.ivory_be.invocation.service.InvocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class InvocationController {

    private final InvocationService invocationService;
    private final InvocationStreamService invocationStreamService;

    @PostMapping("/api/invocations")
    public ResponseEntity<InvocationResponseDto> createInvocation(
            @RequestBody InvocationRequestDto invocationRequestDto) {

        InvocationResponseDto response = invocationService.createInvocation(invocationRequestDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/invocations/{invocationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamInvocation(@PathVariable String invocationId) {
        return invocationStreamService.streamInvocation(invocationId);
    }
}
