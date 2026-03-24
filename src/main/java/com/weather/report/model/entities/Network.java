package com.weather.report.model.entities;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import com.weather.report.model.Timestamped;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;

/// A _monitoring network_ that represents a logical set of system elements.
/// 
/// It may have a list of _operators_ responsible for receiving notifications.
@Entity
public class Network extends Timestamped {

   @ManyToMany(fetch = FetchType.EAGER)
    Collection<Operator> operatorList = new ArrayList<>();

    @OneToMany(mappedBy = "network", fetch = FetchType.EAGER)
    Collection<Gateway> gatewayList = new ArrayList<>();

    //variables
    @Id
    private final String networkCode;

    private String networkName;
    private String networkDesc;

    Network(){
      this.networkCode = null;
      // for db
    }

    // builder
    public Network(String networkCode, String networkName, String networkDesc, String createdBy){
      this.networkCode = networkCode;
      this.networkName = networkName;
      this.networkDesc = networkDesc;
      this.setCreatedBy(createdBy);
      this.setCreatedAt(LocalDateTime.now());
    }

  // class functions
  public Collection<Operator> getOperators() {
     return operatorList;
  }

  public Collection<Gateway> getGateways() {
    return gatewayList;
  }

  public String getCode() {
    return networkCode;
  }

  public String getName() {
    return networkName;
  }

  public String getDescription() {
    return networkDesc;
  }

  public void updateNetwork( String networkName, String networkDesc, String modifiedBy){
      this.networkName = networkName;
      this.networkDesc = networkDesc;
      this.setModifiedBy(modifiedBy);
      this.setModifiedAt(LocalDateTime.now());
  }


}
