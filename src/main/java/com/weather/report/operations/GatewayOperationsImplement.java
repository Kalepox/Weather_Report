package com.weather.report.operations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Gateway;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Parameter;
import com.weather.report.reports.GatewayReport;
import com.weather.report.reports.GatewayReportImplement;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.services.AlertingService;

public class GatewayOperationsImplement extends BaseOperations implements GatewayOperations {

    private CRUDRepository<Gateway, String> gatewayRepository;
    private CRUDRepository<Parameter, String> parameterRepository;
    private MeasurementRepository measurementRepository;

    public GatewayOperationsImplement() {
        this.gatewayRepository = new CRUDRepository<>(Gateway.class);
        this.parameterRepository = new CRUDRepository<>(Parameter.class);
        this.measurementRepository = new MeasurementRepository();
    }


    @Override
    public Gateway createGateway(String code, String name, String description, String username) 
    throws IdAlreadyInUseException, InvalidInputDataException, UnauthorizedException {
        validateMandatory(code, "Code");
        validateMandatory(username, "Username");
        validateCodeFormat(code, "GATEWAY");
        validateNotExists(gatewayRepository, code, "Gateway");
        validateMaintainer(username); 
        Gateway newGateway = new Gateway(code, name, description, username);
        return gatewayRepository.create(newGateway);
    }
    @Override
    public Gateway updateGateway(String code, String name, String description, String username)
            throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        validateMandatory(code, "Code");
        validateMandatory(username, "Username");
        validateMaintainer(username);
        validateExists(gatewayRepository, code, "Gateway");

        Gateway existingGateway = gatewayRepository.read(code);
        existingGateway.updateGateway(name, description, username);
        return  gatewayRepository.update(existingGateway);
    }
    @Override
    public Gateway deleteGateway(String code, String username)
        throws ElementNotFoundException, UnauthorizedException {
        validateMaintainer(username);
        validateExists  (gatewayRepository, code, "Gateway");
        AlertingService.notifyDeletion(username, code, Gateway.class);
        return  gatewayRepository.delete(code);
    }
    @Override
    public Collection<Gateway> getGateways(String... gatewayCodes){
        if(gatewayCodes == null || gatewayCodes.length == 0){
            return gatewayRepository.read();
        }
        List<Gateway> output = new ArrayList<>();
        for(String code : gatewayCodes){
            Gateway gateway = gatewayRepository.read(code);
            if(gateway != null){
                output.add(gateway);
            }
        }
        return output;
    }
    @Override
     public Parameter createParameter(String gatewayCode, String code, String name, String description, double value,
      String username)
      throws IdAlreadyInUseException, InvalidInputDataException, ElementNotFoundException,UnauthorizedException {
        validateMandatory(gatewayCode, "Gateway Code");
        validateMandatory(code, "Parameter Code");
        validateMaintainer(username);
        validateExists(gatewayRepository, gatewayCode, "Gateway");
        Gateway gateway = gatewayRepository.read(gatewayCode);
        
        // Check uniqueness WITHIN the gateway (not globally)
        for (Parameter p : gateway.getParameters()) {
            if (p.getCode().equals(code)) {
        throw new IdAlreadyInUseException("Parameter with code " + code + " already exists in gateway " + gatewayCode);
            }
        }
        Parameter newParameter = new Parameter(code, name, description, value, gateway);
        return  parameterRepository.create(newParameter);
      }

        
    @Override
    public Parameter updateParameter(String gatewayCode, String code, double value, String username)
    throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException {
        validateMandatory(gatewayCode, "Gateway Code");
        validateMaintainer(username);
        validateMandatory(code, "Parameter Code");
        validateExists(gatewayRepository, gatewayCode, "Gateway");
        Gateway gateway = gatewayRepository.read(gatewayCode);
        //To find parameter within gateway we can't use anymore the repository read by code (global)
        Parameter existingParameter = null;
        for (Parameter p: gateway.getParameters()) {
            if (p.getCode().equals(code)) {
                existingParameter = p;
                break;
            }
        }
        if (existingParameter == null) {
            throw new ElementNotFoundException("Parameter with code " + code + " not found in gateway " + gatewayCode);
        }
        existingParameter.updateParameter(existingParameter.getName(), existingParameter.getDescription(), value);
        return  existingParameter;
    }
    @Override
    public GatewayReport getGatewayReport(String code, String startDate, String endDate)
      throws ElementNotFoundException, InvalidInputDataException {
        validateMandatory(code, "Gateway Code");
        validateExists(gatewayRepository, code, "Gateway");
        Gateway gateway = gatewayRepository.read(code);
        List<Measurement> allMeasurements = measurementRepository.read();
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime start = (startDate != null) ? LocalDateTime.parse(startDate, formatter) : null;
    LocalDateTime end = (endDate != null) ? LocalDateTime.parse(endDate, formatter) : null;
    
    List<Measurement> filteredMeasurements = allMeasurements.stream()
        .filter(m -> m.getGatewayCode().equals(code))
        .filter(m -> start == null || !m.getTimestamp().isBefore(start))
        .filter(m -> end == null || !m.getTimestamp().isAfter(end))
        .collect(Collectors.toList());
    
    // Extract parameters from gateway
    Double expectedMean = null;
    Double expectedStdDev = null;
    Double batteryCharge = null;
    
    for (Parameter p : gateway.getParameters()) {
        if (Parameter.EXPECTED_MEAN_CODE.equals(p.getCode())) {
            expectedMean = p.getValue();
        } else if (Parameter.EXPECTED_STD_DEV_CODE.equals(p.getCode())) {
            expectedStdDev = p.getValue();
        } else if (Parameter.BATTERY_CHARGE_PERCENTAGE_CODE.equals(p.getCode())) {
            batteryCharge = p.getValue();
        }
    }
    
    return new GatewayReportImplement(code, startDate, endDate, filteredMeasurements, 
        expectedMean, expectedStdDev, batteryCharge);
    }    
}
