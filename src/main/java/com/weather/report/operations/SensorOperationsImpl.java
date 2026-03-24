package com.weather.report.operations;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.ThresholdType;
import com.weather.report.model.entities.Sensor;
import com.weather.report.model.entities.Threshold;
import com.weather.report.model.entities.User;
import com.weather.report.reports.SensorReport;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.services.AlertingService;

public class SensorOperationsImpl extends BaseOperations implements SensorOperations {

  private CRUDRepository<Sensor, String> sensorRepository;
  private CRUDRepository<User, String> userRepository;

  public SensorOperationsImpl() {
    this.sensorRepository = new CRUDRepository<>(Sensor.class);
    this.userRepository = new CRUDRepository<>(User.class);
  }

  @Override
  public Sensor createSensor(String code, String name, String description, String username)
      throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
    validateMaintainer(username);
    validateMandatory(code, "Code");
    validateCodeFormat(code, "SENSOR");
    validateNotExists(sensorRepository, code, "Sensor");
    User user = userRepository.read(username);
    Sensor sensor = new Sensor(code, name, description, user.getUsername());
    return sensorRepository.create(sensor);
  }

  @Override
  public Sensor updateSensor(String code, String name, String description, String username)
    throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
    validateMaintainer(username);
    User user = userRepository.read(username);
    validateMandatory(code, "Code");
    Sensor sensor = validateExists(sensorRepository, code, "Sensor");

    sensor.setName(name);
    sensor.setDescription(description);
    sensor.setModifiedBy(user.getUsername());
    sensor.setModifiedAt(LocalDateTime.now());

    return sensorRepository.update(sensor);
  }

  @Override
  public Sensor deleteSensor(String code, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {

    validateMaintainer(username);
    User user = userRepository.read(username);
    validateMandatory(code, "Code");
    Sensor sensor = validateExists(sensorRepository, code, "Sensor");

    sensorRepository.delete(code);
    AlertingService.notifyDeletion(user.getUsername(), code, Sensor.class);

    return sensor;
  }

  @Override
  public Collection<Sensor> getSensors(String... sensorCodes) {
    List<Sensor> allSensors = sensorRepository.read();

    if (sensorCodes == null || sensorCodes.length == 0) {
      return allSensors;
    }

    return allSensors.stream()
        .filter(s -> {
          for (String code : sensorCodes) {
            if (s.getCode().equals(code)) {
              return true;
            }
          }
          return false;
        })
        .collect(Collectors.toList());
  }

  @Override
  public Threshold createThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, IdAlreadyInUseException, UnauthorizedException {

    validateMaintainer(username);
    User user = userRepository.read(username);
    checkThresholdType(type);
    validateMandatory(sensorCode, "Sensor Code");
    Sensor sensor = validateExists(sensorRepository, sensorCode, "Sensor");

    if (sensor.getThreshold() != null) {
      throw new IdAlreadyInUseException("Threshold already exists for sensor " + sensorCode);
    }

    Threshold threshold = new Threshold(type, value);
    sensor.setThreshold(threshold);
    sensor.setModifiedBy(user.getUsername());
    sensor.setModifiedAt(LocalDateTime.now());

    sensorRepository.update(sensor);
    return threshold;
  }

  @Override
  public Threshold updateThreshold(String sensorCode, ThresholdType type, double value, String username)
      throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {

    validateMaintainer(username);
    User user = userRepository.read(username);
    checkThresholdType(type);
    validateMandatory(sensorCode, "Sensor Code");
    Sensor sensor = validateExists(sensorRepository, sensorCode, "Sensor");

    if (sensor.getThreshold() == null) {
      throw new ElementNotFoundException("Threshold not found for sensor " + sensorCode);
    }

    Threshold threshold = sensor.getThreshold();
    threshold.setType(type);
    threshold.setValue(value);
    sensor.setModifiedBy(user.getUsername());
    sensor.setModifiedAt(LocalDateTime.now());

    sensorRepository.update(sensor);
    return threshold;
  }

  @Override
  public SensorReport getSensorReport(String code, String startDate, String endDate)
      throws InvalidInputDataException, ElementNotFoundException {

    validateMandatory(code, "Code");
    validateExists(sensorRepository, code, "Sensor");

    LocalDateTime start = parseDate(startDate);
    LocalDateTime end = parseDate(endDate);

    return new SensorReportImpl(code, startDate, endDate, start, end);
  }

  // ------------------------------------------------------------------
  // Helper method (added at the end to minimize diff noise)
  // ------------------------------------------------------------------

  private void checkThresholdType(ThresholdType type) throws InvalidInputDataException {
    if (type == null) {
      throw new InvalidInputDataException("Threshold type is mandatory");
    }
  }
}
