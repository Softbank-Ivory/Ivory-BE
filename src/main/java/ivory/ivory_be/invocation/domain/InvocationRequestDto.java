package ivory.ivory_be.invocation.domain;

import java.util.Map;
import lombok.Data;

@Data
public class InvocationRequestDto {

    private String code;
    private String runtime;
    private String handler;
    private Map<String, Object> payload;

}
