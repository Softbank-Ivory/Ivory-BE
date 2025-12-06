package ivory.ivory_be.invocation.service;

import ivory.ivory_be.invocation.domain.RunnerMetricsDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@Service
@RequiredArgsConstructor
public class RunnerMetricsService {

    private final CloudWatchClient cloudWatchClient;

    @Value("${runner.instance-id}")
    private String instanceId;

    private static final int PERIOD_SECONDS = 60;
    private static final int LOOKBACK_MINUTES = 5;

    public RunnerMetricsDto getCurrentMetrics() {
        Double cpu = fetchCpuUtilization();
        Double mem = fetchMemoryUtilization();

        return RunnerMetricsDto.builder()
                .cpu(cpu)
                .memory(mem)
                .build();
    }

    private Double fetchCpuUtilization() {
        Instant end = Instant.now().minusSeconds(60);
        Instant start = end.minus(LOOKBACK_MINUTES, ChronoUnit.MINUTES);

        GetMetricStatisticsRequest req = GetMetricStatisticsRequest.builder()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .dimensions(Dimension.builder()
                        .name("InstanceId")
                        .value(instanceId)
                        .build())
                .startTime(start)
                .endTime(end)
                .period(PERIOD_SECONDS)
                .statistics(Statistic.AVERAGE)
                .build();

        GetMetricStatisticsResponse res = cloudWatchClient.getMetricStatistics(req);
        List<Datapoint> points = res.datapoints();

        if (points == null || points.isEmpty()) {
            return null;
        }

        // 가장 최신 datapoint 기준
        return points.stream()
                .max(Comparator.comparing(Datapoint::timestamp))
                .map(Datapoint::average)
                .orElse(null);
    }

    private Double fetchMemoryUtilization() {
        Instant end = Instant.now();
        Instant start = end.minus(LOOKBACK_MINUTES, ChronoUnit.MINUTES);

        GetMetricStatisticsRequest req = GetMetricStatisticsRequest.builder()
                .namespace("SoftbankHackathon/Runner")
                .metricName("MemUsedPercent")
                .dimensions(
                        Dimension.builder()
                                .name("InstanceId")
                                .value(instanceId)
                                .build()
                )
                .startTime(start)
                .endTime(end)
                .period(PERIOD_SECONDS)
                .statistics(Statistic.AVERAGE)
                .build();

        GetMetricStatisticsResponse res = cloudWatchClient.getMetricStatistics(req);
        List<Datapoint> points = res.datapoints();

        if (points == null || points.isEmpty()) {
            return null;
        }

        return points.stream()
                .max(Comparator.comparing(Datapoint::timestamp))
                .map(Datapoint::average)
                .orElse(null);
    }

}
