package com.weather.report.services;

import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.WeatherReport;
import com.weather.report.model.ThresholdType;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
/**
 * Service responsible for importing measurements from CSV files and validating
 * them
 * against sensor thresholds, triggering notifications when needed (see README).
 */
public class DataImportingService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);

  private DataImportingService(){
    // utility class
  }

  
  /**
   * Reads measurements from CSV files, persists them through repositories and
   * invokes {@link #checkMeasurement(Measurement)} after each insertion. 
   * The time window format and CSV location are defined in the README.
   *
   * @param filePath path to the CSV file to import
   */
  public static void storeMeasurements(String filePath) {
      MeasurementRepository dataRepository = new MeasurementRepository();
      //DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT); not used rn
      Set<String> notifiedSensors = new HashSet<>(); // stores the notified sensors
      String normalizedPath = filePath;

      if (filePath.startsWith("/") && filePath.length() > 2 && filePath.charAt(2) == ':') {
          normalizedPath = filePath.substring(1); // strip leading slash from /C:/...
      } 
      normalizedPath = URLDecoder.decode(normalizedPath, StandardCharsets.UTF_8);
      try(
        BufferedReader fileReader = new BufferedReader(new FileReader(normalizedPath))) {

          String newLine;
          boolean isFirstLine = true;
          
          while((newLine = fileReader.readLine())!= null){
            if(isFirstLine){// skips the first line
            isFirstLine = false;
            continue;
          }

          //data here has been formatted based on .csv fileds
          String[] fields = newLine.split(",");
          if(fields.length >=5){
            String date = fields[0].trim();
            String networkCode = fields[1].trim();
            String gatewayCode = fields[2].trim();
            String sensorCode = fields[3].trim();
            double value = Double.parseDouble(fields[4].trim());


            LocalDateTime timeStamp = LocalDateTime.parse(date,DATE_FORMATTER);
            Measurement newMeasurement = new Measurement(networkCode,gatewayCode,sensorCode,value,timeStamp);
            Measurement measurement = dataRepository.create(newMeasurement);
            checkMeasurement(measurement, notifiedSensors);
            } 
          }
        }
        catch(IOException e){

          e.printStackTrace();
        }
  }

  /**
   * Validates the saved measurement against the threshold of the corresponding
   * sensor
   * and notifies operators when the value is out of bounds. To be implemented in
   * R1.
   *
   * @param measurement newly stored measurement
   */
  private static void checkMeasurement(Measurement measurement, Set<String> notifiedSensors) {
    /***********************************************************************/
    /* Do not change these lines, use currentSensor to check for possible */
    /* threshold violation, tests mocks this db interaction */
    /***********************************************************************/
    CRUDRepository<Sensor, String> sensorRepository = new CRUDRepository<>(Sensor.class);
    Sensor currentSensor = sensorRepository.read().stream()
        .filter(s -> measurement.getSensorCode().equals(s.getCode()))
        .findFirst()
        .orElse(null);
    /***********************************************************************/
      if(currentSensor ==   null || currentSensor.getThreshold() ==null){
      return ; // check for sensor existing 
    }

    Threshold threshold = currentSensor.getThreshold();
    double measurementValue = measurement.getValue();
    double thresholdValue = threshold.getValue();
    ThresholdType thresholdType = threshold.getType();

    boolean isViolated = false;
    switch(thresholdType){
      case LESS_THAN:
        isViolated = measurementValue >= thresholdValue;
        break;
      case LESS_OR_EQUAL:
        isViolated = measurementValue > thresholdValue;
        break;
      case EQUAL:
        isViolated = measurementValue != thresholdValue;
        break;
      case GREATER_OR_EQUAL:
        isViolated = measurementValue < thresholdValue;
        break;
      case GREATER_THAN:
        isViolated = measurementValue <= thresholdValue;
        break;
      case NOT_EQUAL:
        isViolated = measurementValue == thresholdValue;
        break;
    }

    if(isViolated){
      if (notifiedSensors.add(currentSensor.getCode())) {  // also checks if a violation already happened to the sensor with that code
      CRUDRepository<Network,String > networkRepository = new CRUDRepository<>(Network.class);
      // repo used here to avoid extra lookups, we check the DB only when there is violation
      // if it was declared in start we had lookup for each entry regerdless of violation

      Network network = networkRepository.read()
                                          .stream()
                                          .filter(n-> measurement.getNetworkCode().equals(n.getCode()))
                                          .findFirst()
                                          .orElse(null);
        if(network !=null && !network.getOperators().isEmpty()){
            AlertingService.notifyThresholdViolation(network.getOperators(), currentSensor.getCode());
        }
      }
    }
  }

}
