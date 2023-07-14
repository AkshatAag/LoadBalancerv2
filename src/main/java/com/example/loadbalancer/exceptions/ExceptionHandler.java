package com.example.loadbalancer.exceptions;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        Map<String, List<String>> body = new HashMap<>();
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList());
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = NoFreeMediaServerException.class)
    public ResponseEntity<Object> handleNoFreeMediaServerException(NoFreeMediaServerException e) {
        return new ResponseEntity<>("There are no servers which can take more calls", HttpStatus.BAD_REQUEST);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = NoSuchObjectInDatabaseException.class)
    public ResponseEntity<Object> handleNoSuchObjectInDatabaseException(NoSuchObjectInDatabaseException e) {
        return new ResponseEntity<>(e.toString(), e.getHttpStatus());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = CallCannotBeAddedAgainException.class)
    public ResponseEntity<Object> handleCallCannotBeAddedAgainException(CallCannotBeAddedAgainException e) {
        return new ResponseEntity<>(e.toString(), HttpStatus.BAD_REQUEST);
    }
}
