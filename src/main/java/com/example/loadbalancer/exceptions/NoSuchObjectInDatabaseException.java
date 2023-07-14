package com.example.loadbalancer.exceptions;

import org.springframework.http.HttpStatus;

public class NoSuchObjectInDatabaseException extends RuntimeException {
    private final Class<?> entityClass;
    private final String id;
    private final HttpStatus httpStatus;

    public NoSuchObjectInDatabaseException(Class<?> entityClass, String id, HttpStatus httpStatus) {
        this.entityClass = entityClass;
        this.id = id;
        this.httpStatus = httpStatus;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getId() {
        return id;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String toString() {
        return "No Such Object In Database Exception{\n" +
                "\t\tentityClass=\n" + entityClass.getName() +
                ",\n\t\t id='" + id + "'\n" +
                '}';
    }
}
