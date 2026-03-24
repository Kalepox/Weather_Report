package com.weather.report.reports;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.weather.report.model.entities.Measurement;

public class GatewayReportImplement implements GatewayReport {
    private String gatewayCode;
    private String startDate;
    private String endDate;
    private List<Measurement> measurementsList;
    private Double expectedMean;
    private Double expectedStdDev;
    private Double batteryCharge;   
    
    public GatewayReportImplement(String gatewayCode, String startDate, String endDate, List<Measurement> measurementsList, Double expectedMean, Double expectedStdDev, Double batteryCharge){
        this.gatewayCode = gatewayCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.measurementsList = measurementsList;
        this.expectedMean = expectedMean;
        this.expectedStdDev = expectedStdDev;
        this.batteryCharge = batteryCharge;
    }

    @Override
    public String getCode(){
        return gatewayCode;
    }

    @Override
    public String getStartDate(){
        return startDate;
    }

    @Override
    public String getEndDate(){
        return endDate;
    }

    @Override
    public long getNumberOfMeasurements(){
        return measurementsList.size();
    }





    @Override
    public Collection<String> getMostActiveSensors(){
        Map<String, Long> outputMost = measurementsList.stream()
        .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.counting()));
        
        if (outputMost.isEmpty()){
            return new ArrayList<>();
        }
        long maxValue = outputMost.values().stream().max(Long::compareTo).orElse(0L);
        return outputMost.entrySet().stream()
        .filter(entry -> entry.getValue() == maxValue)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }
    @Override
    public Collection<String> getLeastActiveSensors(){
        Map<String, Long> outputLeast = measurementsList.stream()
        .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.counting()));
        if (outputLeast.isEmpty()){
            return new ArrayList<>();
        }
        long minValue = outputLeast.values().stream().min(Long::compareTo).orElse(0L);
        return outputLeast.entrySet().stream()
        .filter(entry -> entry.getValue() == minValue)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    }
    @Override
    public Map<String, Double> getSensorsLoadRatio(){
        if (measurementsList.isEmpty()){
            return new HashMap<>();
        }
        Map<String, Long> sensorCounts = measurementsList.stream()
        .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.counting()));
        long totalMeasurements = measurementsList.size();
        Map<String, Double> loadRatios = new HashMap<>();
        for (Map.Entry<String, Long> entry : sensorCounts.entrySet()) {
            double ratio = (entry.getValue() * 100.0) / totalMeasurements;
            loadRatios.put(entry.getKey(), ratio);
        }
        return loadRatios;
    }

    @Override
    public Collection<String> getOutlierSensors(){
        // Return empty if no measurements or missing parameters
        if (measurementsList.isEmpty() || expectedMean == null || expectedStdDev == null) {
            return new ArrayList<>();
        }
        
        Map<String, Double> sensorMeans = measurementsList.stream() 
        .collect(Collectors.groupingBy(Measurement::getSensorCode, Collectors.averagingDouble(Measurement::getValue)));
        double threshold = 2 * expectedStdDev;       
    
        return sensorMeans.entrySet().stream()
        .filter(entry -> Math.abs(entry.getValue() - expectedMean) >= threshold)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    }

    @Override
    public double getBatteryChargePercentage() {
        return batteryCharge!= null ? batteryCharge : 0.0;
    }


    private static class DurationRange implements Report.Range<Duration> {
    private final Duration start;
    private final Duration end;

    DurationRange(Duration start, Duration end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Duration getStart() { return start; }

    @Override
    public Duration getEnd() { return end; }

    @Override
    public boolean contains(Duration v) {
        return v.compareTo(start) >= 0 && v.compareTo(end) < 0;
    }
}





    @Override
public SortedMap<Report.Range<Duration>, Long> getHistogram() {
    if (measurementsList.size() < 2) {
        return new TreeMap<>(Comparator.comparing(Report.Range::getStart));
    }

    // Sort measurements by timestamp
    List<Measurement> sorted = measurementsList.stream()
        .sorted(Comparator.comparing(Measurement::getTimestamp))
        .collect(Collectors.toList());

    // Calculate inter-arrival times (duration between consecutive measurements)
    List<Duration> interArrivalTimes = new ArrayList<>();
    for (int i = 0; i < sorted.size() - 1; i++) {
        Duration gap = Duration.between(sorted.get(i).getTimestamp(), sorted.get(i + 1).getTimestamp());
        interArrivalTimes.add(gap);
    }

    if (interArrivalTimes.isEmpty()) {
        return new TreeMap<>(Comparator.comparing(Report.Range::getStart));
    }

    // Find min and max durations
    Duration minDuration = interArrivalTimes.stream().min(Duration::compareTo).get();
    Duration maxDuration = interArrivalTimes.stream().max(Duration::compareTo).get();

    // Create 20 buckets
    int numBuckets = 20;
    long totalNanos = maxDuration.toNanos() - minDuration.toNanos();
    long bucketWidth = Math.max(1, totalNanos / numBuckets);

    SortedMap<Report.Range<Duration>, Long> histogram = new TreeMap<>(Comparator.comparing(Report.Range::getStart));

    // Create buckets and count
    for (int i = 0; i < numBuckets; i++) {
        Duration bucketStart = minDuration.plusNanos(i * bucketWidth);
        Duration bucketEnd = (i == numBuckets - 1) 
            ? maxDuration.plusNanos(1)  // include max in last bucket
            : minDuration.plusNanos((i + 1) * bucketWidth);
        
        DurationRange range = new DurationRange(bucketStart, bucketEnd);
        
        final int bucketIndex = i;
        final Duration bStart = bucketStart;
        final Duration bEnd = bucketEnd;
        
        long count = interArrivalTimes.stream()
            .filter(d -> {
                if (bucketIndex == numBuckets - 1) {
                    // Last bucket: [start, end]
                    return d.compareTo(bStart) >= 0 && d.compareTo(maxDuration) <= 0;
                } else {
                    // Other buckets: [start, end)
                    return d.compareTo(bStart) >= 0 && d.compareTo(bEnd) < 0;
                }
            })
            .count();
        
        histogram.put(range, count);
    }

    return histogram;
}

}