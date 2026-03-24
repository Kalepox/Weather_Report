package com.weather.report.model.entities;

import com.weather.report.model.Timestamped;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;

/// A _sensor_ measures a physical quantity and periodically sends the corresponding measurements.
/// 
/// A sensor may have a _threshold_ defined by the user to detect anomalous behaviours.
@Entity
public class Sensor extends Timestamped {

  @Id
  private String code;
  private String name;
  private String description;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Threshold threshold;

@ManyToOne(fetch= FetchType.EAGER)
@JoinColumn(name = "gateway_code")
private Gateway gateway;

  public Sensor() {
    // JPA compliance
  }

  public Sensor(String code, String name, String description, String createdBy) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.setCreatedBy(createdBy);
    this.setCreatedAt(LocalDateTime.now());
  }

  public Threshold getThreshold() {
    return threshold;
  }

  public Gateway getGateway() {
    return gateway;
  }

  public void setThreshold(Threshold threshold) {
    this.threshold = threshold;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setGateway(Gateway gateway) {
    this.gateway = gateway;
  } 

}
