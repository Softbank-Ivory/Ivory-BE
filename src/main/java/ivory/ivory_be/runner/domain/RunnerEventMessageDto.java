package ivory.ivory_be.runner.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RunnerEventMessageDto {

    public enum Type {
        STATUS,
        LOG,
        COMPLETE
    }

    private Type type;
    private String invocationId;
    private Payload payload;

    @Getter
    @Setter
    public static class Payload {
        private String status;

        private String line;

        private Integer durationMs;

        private Result result;

        private String errorType;
        private String errorMessage;
    }

    @Getter
    @Setter
    public static class Result {
        private Integer statusCode;
        private String body;
    }
}