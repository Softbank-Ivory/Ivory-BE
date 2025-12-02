package ivory.ivory_be.invocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ivory.ivory_be.invocation.domain.InvocationRequestDto;
import ivory.ivory_be.invocation.domain.InvocationResponseDto;
import ivory.ivory_be.invocation.domain.RunnerInvokeRequestDto;
import ivory.ivory_be.invocation.entity.Invocation;
import ivory.ivory_be.invocation.repository.InvocationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@RequiredArgsConstructor
@Transactional
public class InvocationService {

    private final S3Service s3Service;
    private final InvocationRepository invocationRepository;
    private final RestClient restClient;

    private static final String RUNNER_URL = "http://";

    public InvocationResponseDto createInvocation(InvocationRequestDto req) {

        // invocationId 생성
        String invocationId = generateInvocationId();
        // 확장자 생성
        String ext = resolveFileExtension(req.getRuntime());

        // S3 저장
        String key = "invocations/" + invocationId + "/code." + ext;
        s3Service.uploadCode(key, req.getCode());

        // DB 저장
        Invocation invocation = Invocation.builder()
                .invocationId(invocationId)
                .runtime(req.getRuntime())
                .handler(req.getHandler())
                .payload(toJson(req.getPayload()))
                .status("REQUEST_RECEIVED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        invocationRepository.save(invocation);

        // Runner 요청 - restClient
        RunnerInvokeRequestDto runnerInvokeRequestDto = RunnerInvokeRequestDto.builder()
                .invocationId(invocationId)
                .codeKey(key)
                .runtime(req.getRuntime())
                .handler(req.getHandler())
                .payload(req.getPayload())
                .build();

        sendInvocationToRunner(runnerInvokeRequestDto);

        // 응답 반환
        return InvocationResponseDto.builder()
                .invocationId(invocationId)
                .status("REQUEST_RECEIVED")
                .build();
    }

    public String generateInvocationId() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String rand = UUID.randomUUID().toString().substring(0, 6);
        return "inv-" + today + "-" + rand;
    }

    public String resolveFileExtension(String runtime) {
        if (runtime.startsWith("python")) {
            return "py";
        }
        if (runtime.startsWith("node")) {
            return "js";
        }
        if (runtime.startsWith("java")) {
            return "java";
        }
        return "txt";
    }

    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendInvocationToRunner(RunnerInvokeRequestDto req) {
        try {
            restClient.post()
                    .uri(RUNNER_URL + "/test")
                    .body(req)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Runner 호출 실패", e);
        }
    }

}
