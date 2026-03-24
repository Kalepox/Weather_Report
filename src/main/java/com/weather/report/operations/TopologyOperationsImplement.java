package com.weather.report.operations;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Sensor;
import com.weather.report.repositories.CRUDRepository;
import java.util.Collection;

public class TopologyOperationsImplement extends BaseOperations implements TopologyOperations {

  private CRUDRepository<Network, String> networkRepository;
  private CRUDRepository<Gateway, String> gatewayRepository;
  private CRUDRepository<Sensor, String> sensorRepository;

  public TopologyOperationsImplement(){
    this.networkRepository = new CRUDRepository<>(Network.class);
    this.gatewayRepository = new CRUDRepository<>(Gateway.class);
    this.sensorRepository = new CRUDRepository<>(Sensor.class);
  }
 
  @Override
  public Collection<Gateway> getNetworkGateways(String networkCode)
    throws InvalidInputDataException, ElementNotFoundException{
    validateMandatory(networkCode, "Network code");
    Network network = validateExists(networkRepository,networkCode, "Network");
    return network.getGateways();
    }

  @Override
  public Network connectGateway(String networkCode, String gatewayCode, String username)
    throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException{
    validateMandatory(networkCode, "Network code");
    validateMandatory(gatewayCode, "Gateway code");
    validateMaintainer(username);
    Network network = validateExists(networkRepository, networkCode, "Network");
    Gateway gateway = validateExists(gatewayRepository, gatewayCode, "Gateway");
    
    // Check if gateway is already connected to a network
    if (gateway.getNetwork() != null) {
      if (gateway.getNetwork().getCode().equals(networkCode)) {
        // Already connected to the same network
        throw new InvalidInputDataException("Gateway is already connected to this network");
      } else {
        // Connected to a different network
        throw new InvalidInputDataException("Gateway is already connected to another network");
      }
    }
    gateway.setNetwork(network);
    network.getGateways().add(gateway);
    gatewayRepository.update(gateway);
    return network;
    }

  @Override
  public Network disconnectGateway(String networkCode, String gatewayCode, String username)
    throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException{
    validateMandatory(networkCode, "Network code");
    validateMandatory(gatewayCode, "Gateway code");
    validateMaintainer(username);
    Network network = validateExists(networkRepository, networkCode, "Network");
    Gateway gateway = validateExists(gatewayRepository, gatewayCode, "Gateway");
    
    // Check if gateway is connected to the network
    if (gateway.getNetwork() == null || !gateway.getNetwork().getCode().equals(networkCode)) {
      throw new InvalidInputDataException("Gateway is not connected to the specified network");
    }
    gateway.setNetwork(null);
    network.getGateways().remove(gateway);
    gatewayRepository.update(gateway);
    return network;
    }

  @Override
  public Collection<Sensor> getGatewaySensors(String gatewayCode)
    throws InvalidInputDataException, ElementNotFoundException{
    validateMandatory(gatewayCode, "Gateway code");
    Gateway gateway = validateExists(gatewayRepository,gatewayCode, "Gateway");
    return gateway.getSensors(); 
    }

  @Override
  public Gateway connectSensor(String sensorCode, String gatewayCode, String username)
    throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException{
    validateMandatory(gatewayCode, "Gateway code");
    validateMandatory(sensorCode, "Sensor code");
    validateMaintainer(username);
    Gateway gateway = validateExists(gatewayRepository, gatewayCode, "Gateway");
    Sensor sensor = validateExists(sensorRepository, sensorCode, "Sensor");
    // Check if sensor is already connected to a gateway
    if (sensor.getGateway() != null) {
      if (sensor.getGateway().getCode().equals(gatewayCode)) {
        // Already connected to the same gateway
        throw new InvalidInputDataException("Sensor is already connected to this gateway");
      } else {
        // Connected to a different gateway
        throw new InvalidInputDataException("Sensor is already connected to another gateway");
      }
    }
    gateway.getSensors().add(sensor);
    sensor.setGateway(gateway);
    sensorRepository.update(sensor);
    return gateway;
    }

  @Override
  public Gateway disconnectSensor(String sensorCode, String gatewayCode, String username)
    throws ElementNotFoundException, UnauthorizedException, InvalidInputDataException{
    validateMandatory(gatewayCode, "Gateway code");
    validateMandatory(sensorCode, "Sensor code");
    validateMaintainer(username);
    Gateway gateway = validateExists(gatewayRepository, gatewayCode, "Gateway");
    Sensor sensor = validateExists(sensorRepository, sensorCode, "Sensor");
    // Check if sensor is connected to the gateway
    if (sensor.getGateway() == null || !sensor.getGateway().getCode().equals(gatewayCode)) {
      throw new InvalidInputDataException("Sensor is not connected to the specified gateway");
    } 
    gateway.getSensors().remove(sensor);
    sensor.setGateway(null);
    sensorRepository.update(sensor);
    return gateway;
    }
}

