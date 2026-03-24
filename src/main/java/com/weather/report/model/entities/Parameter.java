package com.weather.report.model.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/// A _parameter_ is a value associated with the gateway it belongs to.
/// 
/// It allows storing state or configuration information.
@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"gateway_code","parameterCode"}))
public class Parameter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private final String parameterCode;
  private String parameterName;
  private String parameterDesc;
  private double parameterValue;

  @ManyToOne
  @JoinColumn(name="gateway_code")
  private Gateway gateway;

  public static final String EXPECTED_MEAN_CODE = "EXPECTED_MEAN";
  public static final String EXPECTED_STD_DEV_CODE = "EXPECTED_STD_DEV";
  public static final String BATTERY_CHARGE_PERCENTAGE_CODE = "BATTERY_CHARGE";


  Parameter(){
    this.parameterCode = null;
  }

  public Parameter(String parameterCode, String parameterName, String parameterDesc, double parameterValue, Gateway gateway){
    this.parameterCode = parameterCode;
    this.parameterName = parameterName;
    this.parameterDesc = parameterDesc;
    this.parameterValue = parameterValue;
    this.gateway = gateway;
  }

  
  public String getCode() {
    return parameterCode;
  }

  public String getName() {
    return parameterName;
  }

  public String getDescription() {
    return parameterDesc;
  }

  public Gateway getGateway() {
    return gateway;
  }

  public double getValue() {
    return parameterValue;
  }

 

  public void updateParameter(String parameterName, String parameterDesc, double parameterValue){
    this.parameterName = parameterName;
    this.parameterDesc = parameterDesc;
    this.parameterValue = parameterValue;
  }

}
