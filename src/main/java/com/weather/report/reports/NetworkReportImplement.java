package com.weather.report.reports;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.time.Duration;

import com.weather.report.model.entities.Measurement;
import com.weather.report.WeatherReport;


public class NetworkReportImplement implements NetworkReport {

    private String networkCode;
    private String startDate;
    private String endDate;
    private List<Measurement> measurementList;

    public NetworkReportImplement(String networkCode, String startDate, String endDate, List<Measurement> measurementList){
        this.networkCode = networkCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.measurementList = measurementList;
    }

    @Override
    public String getCode(){
        return networkCode;
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
        return measurementList.size();
    }

    @Override
    public Collection<String> getMostActiveGateways() {
        Map<String, Long> outputMost = measurementList.stream()
                            .collect(Collectors.groupingBy(Measurement:: getGatewayCode,Collectors.counting()));
        
        if(outputMost.isEmpty()){
            return new ArrayList<>();
        }
        long maxCount = outputMost.values().stream().max(Long :: compareTo).orElse(0L);
        return outputMost.entrySet().stream().filter(entry-> entry.getValue() == maxCount)
                        .map(Map.Entry :: getKey)
                        .collect(Collectors.toList());
    }
    
    @Override
    public Collection<String> getLeastActiveGateways() {
        Map<String, Long> outputLeast = measurementList.stream()
                            .collect(Collectors.groupingBy(Measurement :: getGatewayCode, Collectors.counting()));
        if(outputLeast.isEmpty()){
            return new ArrayList<>(); // gives an empty list if there are not values
        }
        long minCount = outputLeast.values().stream().min(Long :: compareTo).orElse(0L);
        return outputLeast.entrySet().stream().filter(entry -> entry.getValue()== minCount)
                            .map(Map.Entry :: getKey)
                            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Double> getGatewaysLoadRatio() {
        if(measurementList.isEmpty()){
            return new HashMap<>();
        }
        Map<String, Long> gateCounts = measurementList.stream()
                                        .collect(Collectors.groupingBy(Measurement:: getGatewayCode, Collectors.counting()));
        long totalGates = measurementList.size();

        Map<String, Double> ratioMap = new HashMap<>();
        for( Map.Entry<String, Long> entry : gateCounts.entrySet()){
              double percentage = (entry.getValue() *100.0)/ totalGates;
              ratioMap.put(entry.getKey(), percentage);
        }

        return ratioMap;
    }
    
    // entire sub-class contains the start/end dates and units
    private static class TimeRange implements Report.Range<LocalDateTime> {
        private final LocalDateTime start;
        private final LocalDateTime end;

        TimeRange(LocalDateTime start, LocalDateTime end, String unit) { 
            this.start = start;
             this.end = end; 
            }
        @Override 
        public LocalDateTime getStart() {
             return start; 
            }
        @Override 
        public LocalDateTime getEnd() { 
            return end;
         }
        @Override 
        public boolean contains(LocalDateTime v) {
             return !v.isBefore(start) && v.isBefore(end); 
            } // last-bucket logic handled outside
}

    @Override
    public SortedMap<Report.Range<LocalDateTime>, Long> getHistogram() {
        SortedMap<Report.Range<LocalDateTime>, Long> histogram = new TreeMap<>(Comparator.comparing(Report.Range :: getStart));
        
        if(measurementList.isEmpty()){
            return histogram;
        }
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);
        LocalDateTime startTime = (startDate != null)
        ? LocalDateTime.parse(startDate, timeFormatter)
        : measurementList.stream().map(Measurement::getTimestamp).min(LocalDateTime::compareTo).orElse(null);
        
        LocalDateTime endTime = (endDate != null)
        ? LocalDateTime.parse(endDate, timeFormatter)
        : measurementList.stream().map(Measurement::getTimestamp).max(LocalDateTime::compareTo).orElse(null);
        
        if (startTime == null || endTime == null) {
            return histogram;
        }
    
        boolean hourly = Duration.between(startTime, endTime).toHours()<=48;// checks if its hourly or daily
        List<TimeRange> timeBuckets = createBuckets(startTime, endTime,hourly );

        for(int i = 0; i<timeBuckets.size(); i++){
            TimeRange b = timeBuckets.get(i);
            boolean last = (i== timeBuckets.size()-1);
            long count = measurementList.stream()
                        .map(Measurement :: getTimestamp)
                        .filter(t-> !t.isBefore(b.getStart())&&(last ? !t.isAfter(b.getEnd()): t.isBefore(b.getEnd())))
                        .count();
                    histogram.put(b,count);
        }
        return histogram;
    }

    // helper function
    private List<TimeRange> createBuckets(LocalDateTime start, LocalDateTime end, boolean hourly){
        List<TimeRange> output = new ArrayList<>();
    LocalDateTime cur = start;
    while (cur.isBefore(end)) {
        LocalDateTime bucketStart = cur;
        LocalDateTime next = hourly
            ? cur.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            : cur.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        LocalDateTime bucketEnd = next.isAfter(end) ? end : next;
        output.add(new TimeRange(bucketStart, bucketEnd, hourly ? "HOUR" : "DAY"));
        cur = bucketEnd;
    }
    if (output.isEmpty()) output.add(new TimeRange(start, end, hourly ? "HOUR" : "DAY"));
    return output;
    }


}
