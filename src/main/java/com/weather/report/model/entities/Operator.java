package com.weather.report.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
/// An _operator_ is an entity that receives notifications when a threshold violation is detected.  
@Entity
public class Operator {

   @Id
  private final String email;

  private String operatorName;
  private String operatorSurname;
  private String phoneNumber;

  Operator(){
    // for db
    this.email = null;
  }

  // class builder
  public Operator(String email, String operatorName,String operatorSurname,String phoneNumber){
    this.operatorName = operatorName;
    this.operatorSurname = operatorSurname;
    this.email = email;
    this.phoneNumber = phoneNumber;
  }

  // class functions
  public String getFirstName() {
    return operatorName;
  }

  public String getLastName() {
    return operatorSurname;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void updateOperator(String firstName, String lastName, String phoneNumber){
    this.operatorName = firstName;
    this.operatorSurname = lastName;
    this.phoneNumber = phoneNumber;
  }

}
