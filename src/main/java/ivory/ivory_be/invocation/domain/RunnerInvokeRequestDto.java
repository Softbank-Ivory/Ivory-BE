package ivory.ivory_be.invocation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerInvokeRequestDto {

    private String invocationId;
    private String codeKey;
    private String runtime;
    private String handler;
    private Object payload;
}
