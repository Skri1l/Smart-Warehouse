package com.skr1l.backend.exception;

import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.exception.demandPlanExceptions.DemandPlanIsNotFoundException;
import com.skr1l.backend.exception.deviceExceptions.DeviceAlreadyExistsException;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductAlreadyExistsException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderAlreadyExistsException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderIsNotFoundException;
import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.recognitionResult.RecognitionResultIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.shelfStockExceptions.ShelfStockIsNotFoundException;
import com.skr1l.backend.exception.stockMovement.StockMovementIsNotFoundException;
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
            ProductIsNotFoundException.class,
            ShelfStockIsNotFoundException.class,
            StockMovementIsNotFoundException.class,
            ImageEventIsNotFoundException.class,
            RecognitionResultIsNotFoundException.class,
            DemandPlanIsNotFoundException.class,
            ReplenishmentRecommendationIsNotFoundException.class,
            PurchaseOrderIsNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler({UserAlreadyExistsException.class,
            WarehouseAlreadyExistsException.class,
            DeviceAlreadyExistsException.class,
            ProductAlreadyExistsException.class,
            PurchaseOrderAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleAlreadyExists(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }
}
