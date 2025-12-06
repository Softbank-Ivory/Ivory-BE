package ivory.ivory_be.invocation.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RunnerMetricsDto {

    private Double cpu;
    private Double memory;

}
