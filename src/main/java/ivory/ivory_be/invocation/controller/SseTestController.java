package ivory.ivory_be.invocation.controller;

import ivory.ivory_be.invocation.service.InvocationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SseTestController {

    private final InvocationStreamService streamService;

    @PostMapping("/test/send/status")
    public void sendStatusTest() {
        streamService.sendStatus("1234", "EXECUTING");
    }

    @PostMapping("/test/send/log")
    public void sendLogTest() {
        streamService.sendLog("1234", "hello from server!");
    }

    @PostMapping("/test/send/complete")
    public void sendCompleteTest() {
        streamService.sendCompleteSuccess("1234", 100, 200, "{\"msg\":\"done\"}");
    }
}