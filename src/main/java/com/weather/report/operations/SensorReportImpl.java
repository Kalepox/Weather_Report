package com.weather.report.operations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.weather.report.model.entities.Measurement;
import com.weather.report.reports.SensorReport;
import com.weather.report.repositories.MeasurementRepository;

public class SensorReportImpl implements SensorReport {

  private String code;
  private String startDate;
  private String endDate;
  private LocalDateTime start;
  private LocalDateTime end;

  private long numberOfMeasurements;
  private double mean;
  private double variance;
  private double stdDev;
  private double minimumMeasuredValue;
  private double maximumMeasuredValue;
  private List<Measurement> outliers;
  private SortedMap<Range<Double>, Long> histogram;

  public SensorReportImpl(String code, String startDate, String endDate, LocalDateTime start, LocalDateTime end) {
    this.code = code;
    this.startDate = startDate;
    this.endDate = endDate;
    this.start = start;
    this.end = end;

    computeReport();
  }

  private void computeReport() {
    // get measurements from repository
    MeasurementRepository measurementRepository = new MeasurementRepository();
    List<Measurement> allMeasurements = measurementRepository.read();

    // filter measurements by sensor and date range
    List<Measurement> measurements = new ArrayList<>();
    for (Measurement m : allMeasurements) {
      if (!m.getSensorCode().equals(code)) {
        continue;
      }

      LocalDateTime timestamp = m.getTimestamp();
      if (start != null && timestamp.isBefore(start)) {
        continue;
      }

      if (end != null && timestamp.isAfter(end)) {
        continue;
      }

      measurements.add(m);
    }

    numberOfMeasurements = measurements.size();

    if (numberOfMeasurements == 0) {
      mean = 0;
      variance = 0;
      stdDev = 0;
      minimumMeasuredValue = 0;
      maximumMeasuredValue = 0;
      outliers = new ArrayList<>();
      histogram = new TreeMap<>();
      return;
    }

    // calculate mean
    double sum = 0;
    for (Measurement m : measurements) {
      sum += m.getValue();
    }
    mean = sum / numberOfMeasurements;

    // calculate variance and stdDev
    if (numberOfMeasurements < 2) {
      variance = 0;
      stdDev = 0;
    } else {
      double sumSquaredDiff = 0;
      for (Measurement m : measurements) {
        double diff = m.getValue() - mean;
        sumSquaredDiff += diff * diff;
      }
      variance = sumSquaredDiff / (numberOfMeasurements - 1);
      stdDev = Math.sqrt(variance);
    }

    // find outliers
    outliers = new ArrayList<>();
    List<Measurement> nonOutliers = new ArrayList<>();
    for (Measurement m : measurements) {
      double diff = Math.abs(m.getValue() - mean);
      if (diff >= 2 * stdDev) {
        outliers.add(m);
      } else {
        nonOutliers.add(m);
      }
    }

    // calculate min and max
    minimumMeasuredValue = measurements.get(0).getValue();
    maximumMeasuredValue = measurements.get(0).getValue();
    for (Measurement m : measurements) {
      if (m.getValue() < minimumMeasuredValue) {
        minimumMeasuredValue = m.getValue();
      }
      if (m.getValue() > maximumMeasuredValue) {
        maximumMeasuredValue = m.getValue();
      }
    }

    // create histogram from non-outliers
    histogram = new TreeMap<>();
    if (nonOutliers.isEmpty()) {
      return;
    }

    // find min and max of non-outliers
    double minNonOutlier = nonOutliers.get(0).getValue();
    double maxNonOutlier = nonOutliers.get(0).getValue();
    for (Measurement m : nonOutliers) {
      if (m.getValue() < minNonOutlier) {
        minNonOutlier = m.getValue();
      }
      if (m.getValue() > maxNonOutlier) {
        maxNonOutlier = m.getValue();
      }
    }

    // create 20 buckets
    double range = maxNonOutlier - minNonOutlier;
    double bucketWidth = range / 20.0;

    // special case: all values are the same
    if (range == 0) {
      DoubleRange singleBucket = new DoubleRange(minNonOutlier, maxNonOutlier, true);
      histogram.put(singleBucket, (long) nonOutliers.size());
      return;
    }

    // create buckets
    for (int i = 0; i < 20; i++) {
      double bucketStart = minNonOutlier + i * bucketWidth;
      double bucketEnd = minNonOutlier + (i + 1) * bucketWidth;
      boolean isLastBucket = (i == 19);

      DoubleRange bucket = new DoubleRange(bucketStart, bucketEnd, isLastBucket);
      histogram.put(bucket, 0L);
    }

    // count measurements in each bucket
    for (Measurement m : nonOutliers) {
      for (Range<Double> bucket : histogram.keySet()) {
        if (bucket.contains(m.getValue())) {
          histogram.put(bucket, histogram.get(bucket) + 1);
          break;
        }
      }
    }
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public String getStartDate() {
    return startDate;
  }

  @Override
  public String getEndDate() {
    return endDate;
  }

  @Override
  public long getNumberOfMeasurements() {
    return numberOfMeasurements;
  }

  @Override
  public double getMean() {
    return mean;
  }

  @Override
  public double getVariance() {
    return variance;
  }

  @Override
  public double getStdDev() {
    return stdDev;
  }

  @Override
  public double getMinimumMeasuredValue() {
    return minimumMeasuredValue;
  }

  @Override
  public double getMaximumMeasuredValue() {
    return maximumMeasuredValue;
  }

  @Override
  public List<Measurement> getOutliers() {
    return outliers;
  }

  @Override
  public SortedMap<Range<Double>, Long> getHistogram() {
    return histogram;
  }

  private static class DoubleRange implements Range<Double>, Comparable<Range<Double>> {
    private double start;
    private double end;
    private boolean isLastBucket;

    public DoubleRange(double start, double end, boolean isLastBucket) {
      this.start = start;
      this.end = end;
      this.isLastBucket = isLastBucket;
    }

    @Override
    public Double getStart() {
      return start;
    }

    @Override
    public Double getEnd() {
      return end;
    }

    @Override
    public boolean contains(Double value) {
      if (isLastBucket) {
        return value >= start && value <= end;
      } else {
        return value >= start && value < end;
      }
    }

    @Override
    public int compareTo(Range<Double> other) {
      return Double.compare(this.start, other.getStart());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null || getClass() != obj.getClass())
        return false;
      DoubleRange other = (DoubleRange) obj;
      return Double.compare(start, other.start) == 0 && Double.compare(end, other.end) == 0;
    }

    @Override
    public int hashCode() {
      long startBits = Double.doubleToLongBits(start);
      long endBits = Double.doubleToLongBits(end);
      return (int) (startBits ^ endBits);
    }
  }
}
