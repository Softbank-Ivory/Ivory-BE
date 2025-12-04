package ivory.ivory_be.runner.api;

import ivory.ivory_be.runner.domain.RuntimeResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
public class RuntimeController {

    @GetMapping("/api/runtimes")
    public List<RuntimeResponseDto> getRuntimes() {
        return Arrays.asList(
                new RuntimeResponseDto("python", "Python 3.10"),
                new RuntimeResponseDto("nodejs", "Node.js 18.x"),
                new RuntimeResponseDto("java", "Java 11")
        );
    }
}
