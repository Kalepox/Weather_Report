package com.weather.report.model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import com.weather.report.model.Timestamped;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/// A _gateway_ groups multiple devices that monitor the same physical quantity.  
/// 
/// It can be configured through parameters that provide information about its state or values needed for interpreting the measurements.
@Entity
public class Gateway extends Timestamped {

  @OneToMany(mappedBy = "gateway", fetch = FetchType.EAGER, cascade =  CascadeType.ALL)
  Collection<Parameter> parameterList = new ArrayList<>();
  
  @ManyToOne( fetch = FetchType.EAGER)
  @JoinColumn(name = "network_code")
  private Network network;

  @OneToMany(mappedBy = "gateway", fetch = FetchType.EAGER, cascade =  CascadeType.ALL)
  Collection<Sensor> sensorList = new ArrayList<>();

  @Id
  private final String gatewayCode;
  private String gatewayName;
  private String gatewayDesc;

  Gateway(){
    this.gatewayCode = null;
  }

  //GatewayBuilder
  public Gateway(String gatewayCode, String gatewayName, String gatewayDesc, String createdBy){
    this.gatewayCode = gatewayCode;
    this.gatewayName = gatewayName;
    this.gatewayDesc = gatewayDesc;
    this.setCreatedBy(createdBy);
    this.setCreatedAt(LocalDateTime.now());
  }
  
  
  

  public Collection<Parameter> getParameters() {
    return parameterList;
  }

  public Network getNetwork() {
    return network;
  }

  public Collection<Sensor> getSensors() {
    return sensorList;
  }
  
  public String getCode() {
    return gatewayCode;
  }

  public String getName() {
    return gatewayName;
  }

  public String getDescription() {
    return gatewayDesc;
  }

  public void setNetwork(Network network) {
    this.network = network;
  }

  public void updateGateway(String GatewayName, String GatewayDesc, String modifiedBy) {
    this.gatewayName = GatewayName;
    this.gatewayDesc = GatewayDesc;
    this.setModifiedBy(modifiedBy);
    this.setModifiedAt(LocalDateTime.now());
  }
}
