package com.skr1l.backend.exception;

import com.skr1l.backend.exception.deviceExceptions.DeviceAlreadyExistsException;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductAlreadyExistsException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.userExceptions.UserAlreadyExistsException;
import com.skr1l.backend.exception.userExceptions.UserNotFoundException;
import com.skr1l.backend.exception.warehouseExceptions.WarehouseAlreadyExistsException;
import com.skr1l.backend.exception.warehouseExceptions.WarehouseIsNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class,
            WarehouseIsNotFoundException.class,
            RackIsNotFoundException.class,
            ShelfIsNotFoundException.class,
            DeviceIsNotFoundException.class,
            ProductIsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler({UserAlreadyExistsException.class,
            WarehouseAlreadyExistsException.class,
            DeviceAlreadyExistsException.class,
            ProductAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleAlreadyExists(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }
}
