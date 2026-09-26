package com.manguonmo.popworld.exception;


import com.manguonmo.popworld.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("Dữ liệu đầu vào không hợp lệ");
        return new ResponseEntity<>(ApiResponse.error(errorMsg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>>resourceNotFoundException(ResourceNotFoundException ex){
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ApiResponse<?>>outOfStockException(OutOfStockException ex){
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>>badRequestException(BadRequestException ex){
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage()),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>>exception(Exception ex){

        log.error("Lỗi hệ thống bất ngờ: ", ex);
        return new ResponseEntity<>(ApiResponse.error("Đã xảy ra lỗi nội bộ từ hệ thống. Vui lòng thử lại sau!"),HttpStatus.INTERNAL_SERVER_ERROR);
    }




}
