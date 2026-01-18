package com.song.my_pim.common.exception;

public class ExportWriteException extends RuntimeException {
    public ExportWriteException(String message) {
        super(message);
    }

    public ExportWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
