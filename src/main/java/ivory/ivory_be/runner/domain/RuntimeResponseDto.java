package ivory.ivory_be.runner.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class RuntimeResponseDto {
    private String name;
    private String version;
}
