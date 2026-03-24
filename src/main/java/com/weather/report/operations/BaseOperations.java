package com.weather.report.operations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.weather.report.WeatherReport;
import com.weather.report.exceptions.ElementNotFoundException;
import com.weather.report.exceptions.IdAlreadyInUseException;
import com.weather.report.exceptions.InvalidInputDataException;
import com.weather.report.exceptions.UnauthorizedException;
import com.weather.report.model.UserType;
import com.weather.report.model.entities.User;
import com.weather.report.repositories.CRUDRepository;

public abstract class BaseOperations {
    
    protected final CRUDRepository<User, String> userRepository;
    //Common code patterns made as constants
    public static final String NETWORK_CODE_PATTERN = "NET_\\d{2}";
    public static final String GATEWAY_CODE_PATTERN = "GW_\\d{4}";
    public static final String SENSOR_CODE_PATTERN = "S_\\d{6}";

    protected BaseOperations() {
        this.userRepository = new CRUDRepository<>(User.class);
    }

    //Input validation method
    //It throws InvalidInputDataException if the input is null or empty
    protected void validateMandatory(String value, String fieldName) throws InvalidInputDataException {
        if (value == null || value.isEmpty()) {
            throw new InvalidInputDataException(fieldName + " is mandatory");
        }
    }

    //Validates that a user exists and has the required type
    // It throws UnauthorizedException if validation fails
    protected void validateUser(String username, UserType requiredType) throws UnauthorizedException {
        if (username == null || username.isEmpty()) {
            throw new UnauthorizedException("Username is mandatory");
        }
        User user = userRepository.read(username);
        if (user == null) {
            throw new UnauthorizedException("User " + username + " not found");
        }
        if (user.getType() != requiredType) {
            throw new UnauthorizedException(
                "User " + username + " is not authorized for this operation");
        }
    }

    // Validates that a user is a MAINTAINER
    protected void validateMaintainer(String username) throws UnauthorizedException {
        validateUser(username, UserType.MAINTAINER);
    }

    //Validate that an entity exists given also its repository
    // It throws ElementNotFoundException if entity is not found
    protected <T, ID> T validateExists(CRUDRepository<T, ID> repository, ID id, String entityName) throws ElementNotFoundException {
        T entity = repository.read(id);
        if (entity == null) {
            throw new ElementNotFoundException(entityName + " with code " + id + " not found");
        }
        return entity;
    }

    //Validate that an entity does not exist given also its repository
    // It throws IdAlreadyInUseException if entity is found
    protected <T, ID> void validateNotExists(CRUDRepository<T, ID> repository, ID id, String entityName) throws IdAlreadyInUseException {
        if (repository.read(id) != null) {
            throw new IdAlreadyInUseException(entityName + " with code " + id + " already exists");
        }
    }

    // Parse date from string
    protected LocalDateTime parseDate(String dateStr) {
        if (dateStr != null && !dateStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WeatherReport.DATE_FORMAT);
            return LocalDateTime.parse(dateStr, formatter);
        }
        return null;
    }

    //Validate a code format 
    // It throws InvalidInputDataException if the code does not match the expected pattern
    //entityString can be "SENSOR", "GATEWAY, "NETWORK"
    protected void validateCodeFormat(String code, String entityString) throws InvalidInputDataException {
        String pattern;
        switch (entityString) {
        case "SENSOR":
            pattern = SENSOR_CODE_PATTERN;
            break;
        case "GATEWAY":
            pattern = GATEWAY_CODE_PATTERN;
            break;  
        case "NETWORK":
            pattern = NETWORK_CODE_PATTERN;
            break;
        default:
            throw new InvalidInputDataException("Unknown entity type: " + entityString);
        }
        if (!code.matches(pattern)) {
            throw new InvalidInputDataException(
                "Invalid code format. Expected: " + pattern);
        }
    }    
}

