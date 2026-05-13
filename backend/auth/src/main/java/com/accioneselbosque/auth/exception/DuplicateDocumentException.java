package com.accioneselbosque.auth.exception;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException(String documentNumber) {
        super("Document number already registered: " + documentNumber);
    }
}
