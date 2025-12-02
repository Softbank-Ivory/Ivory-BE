package ivory.ivory_be.invocation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class InvocationResponseDto {

    private String invocationId;
    private String status;

}
