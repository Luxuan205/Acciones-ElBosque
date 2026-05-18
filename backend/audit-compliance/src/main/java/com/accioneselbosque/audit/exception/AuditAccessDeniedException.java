package com.accioneselbosque.audit.exception;

public class AuditAccessDeniedException extends RuntimeException {
    public AuditAccessDeniedException() {
        super("Acceso denegado al log de auditoría.");
    }
}
