package com.weather.report.operations;
import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.entities.Measurement;
import com.weather.report.model.entities.Network;
import com.weather.report.model.entities.Operator;
import com.weather.report.reports.NetworkReport;
import com.weather.report.repositories.CRUDRepository;
import com.weather.report.repositories.MeasurementRepository;
import com.weather.report.services.AlertingService;
import com.weather.report.reports.NetworkReportImplement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.stream.Collectors;

// we implement all inherited Abstract functions as its on par with NetworkOperations.java
public class NetworkOperationsImplement extends BaseOperations implements NetworkOperations{

    private CRUDRepository<Network, String> networkRepository;
    private CRUDRepository<Operator, String> operatorRepository;
    private MeasurementRepository measurementRepository;

    public NetworkOperationsImplement(){
        this.networkRepository = new CRUDRepository<>(Network.class);
        this.operatorRepository = new CRUDRepository<>(Operator.class);
        this.measurementRepository = new MeasurementRepository();
    }

    @Override
    public Network createNetwork(String code, String name, String description, String username)
        throws IdAlreadyInUseException, InvalidInputDataException,UnauthorizedException {

        validateMandatory(code, "Code");
        validateMandatory(username, "Username");
        validateCodeFormat(code, "NETWORK");
        validateMaintainer(username);
        validateNotExists(networkRepository, code, "Network");

        Network newNetwork= new Network(code, name, description, username);
        return networkRepository.create(newNetwork);
    }

    @Override
    public Network updateNetwork(String code, String name, String description, String username)
        throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException{

        validateMandatory(code, "Code");
        validateMandatory(username, "Username");
        validateMaintainer(username);
        
        Network network = validateExists(networkRepository, code, "Network");
        network.updateNetwork(name, description, username);
        return networkRepository.update(network);
    }

    @Override
    public Network deleteNetwork(String code, String username)
        throws InvalidInputDataException, ElementNotFoundException, UnauthorizedException{

        validateMandatory(code, "Code");
        validateMandatory(username,"Username" );
        validateMaintainer(username);
        validateExists(networkRepository, code, "Network");

        Network deletedNetwork = networkRepository.delete(code);
        AlertingService.notifyDeletion(username,code, Network.class);

        return deletedNetwork;
    }

    @Override
    public Collection<Network> getNetworks(String ... codes){// this is a variable arguement
        if(codes == null || codes.length == 0){
            return networkRepository.read();
        }
        List<Network> output = new ArrayList<>();
        for(String code : codes){
            Network network = networkRepository.read(code);
            if(network != null){
                output.add(network);
            }
        }
        return output;
    }

    @Override
    public Operator createOperator(String firstName, String lastName, String email,String phonenumber,String username)
        throws InvalidInputDataException,IdAlreadyInUseException, UnauthorizedException{
          
        validateMandatory(firstName, "First name");
        validateMandatory(lastName, "Last name");
        validateMandatory(email, "Email");
        validateMandatory(username, "Username");
        validateMaintainer(username);
        validateNotExists(operatorRepository, email, "Operator");

        Operator newOperator = new Operator(email, firstName, lastName, phonenumber);
        return operatorRepository.create(newOperator);
    }

     @Override
    public Network addOperatorToNetwork(String networkCode, String operatorEmail, String username)
        throws ElementNotFoundException, InvalidInputDataException, UnauthorizedException {
        
        validateMandatory(networkCode, "Network code");
        validateMandatory(operatorEmail, "Operator email");
        validateMandatory(username, "Username");
        validateMaintainer(username);
        
        Network network = validateExists(networkRepository, networkCode, "Network");
        Operator operator = validateExists(operatorRepository, operatorEmail, "Operator");
        
        if (!network.getOperators().contains(operator)) {
            network.getOperators().add(operator);
            return networkRepository.update(network);
        }
        return network;
    }

    @Override 
    public NetworkReport getNetworkReport(String code, String startDate, String endDate)
        throws InvalidInputDataException, ElementNotFoundException{

        validateMandatory(code, "Network code");
        validateExists(networkRepository, code, "Network");

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);
        LocalDateTime startTime = (startDate != null && !startDate.isEmpty()) ? 
            LocalDateTime.parse(startDate, timeFormatter) : null;
        LocalDateTime endTime = (endDate != null && !endDate.isEmpty()) ? 
            LocalDateTime.parse(endDate, timeFormatter) : null;
        
            // streams the data that are in the given timestamps for the correct Coded network
        List<Measurement> measurementList = measurementRepository.read().stream()
            .filter(m-> m.getNetworkCode().equals(code))
            .filter(m-> startTime == null || !m.getTimestamp().isBefore(startTime))
            .filter(m-> endTime == null || !m.getTimestamp().isAfter(endTime))
            .collect(Collectors.toList());

        return new NetworkReportImplement(code, startDate, endDate, measurementList);
    }
}