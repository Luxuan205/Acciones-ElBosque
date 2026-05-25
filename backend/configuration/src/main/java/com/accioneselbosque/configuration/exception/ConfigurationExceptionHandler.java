package com.accioneselbosque.configuration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ConfigurationExceptionHandler {

    @ExceptionHandler(HolidayAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleHolidayAlreadyExists(HolidayAlreadyExistsException ex) {
        return Map.of("error", "HOLIDAY_ALREADY_EXISTS", "message", ex.getMessage());
    }

    @ExceptionHandler(HolidayNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleHolidayNotFound(HolidayNotFoundException ex) {
        return Map.of("error", "HOLIDAY_NOT_FOUND", "message", ex.getMessage());
    }

    @ExceptionHandler(InvalidScheduleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidSchedule(InvalidScheduleException ex) {
        return Map.of("error", "VALIDATION_ERROR", "message", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Map.of("error", "VALIDATION_ERROR", "message", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", "BAD_REQUEST", "message", ex.getMessage());
    }

    @ExceptionHandler(ParameterNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleParameterNotFound(ParameterNotFoundException ex) {
        return Map.of("error", "PARAMETER_NOT_FOUND", "message", ex.getMessage());
    }

    @ExceptionHandler(InvalidParameterValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidParameterValue(InvalidParameterValueException ex) {
        return Map.of("error", "INVALID_VALUE", "parameterKey", ex.getParameterKey(), "message", ex.getMessage());
    }
}
