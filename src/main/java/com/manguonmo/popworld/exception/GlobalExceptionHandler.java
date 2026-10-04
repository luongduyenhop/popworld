package com.manguonmo.popworld.exception;


import com.manguonmo.popworld.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(NoResourceFoundException ex, jakarta.servlet.http.HttpServletRequest request) throws NoResourceFoundException {
        if (request != null) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html") && !request.getRequestURI().startsWith("/api/")) {
                throw ex;
            }
        }
        return new ResponseEntity<>(ApiResponse.error("Không tìm thấy tài nguyên: " + ex.getResourcePath()), HttpStatus.NOT_FOUND);
    }

    public ResponseEntity<ApiResponse<?>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return handleAccessDenied(ex, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        if (request != null) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html") && !request.getRequestURI().startsWith("/api/")) {
                throw ex;
            }
        }
        String message = (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                && !ex.getMessage().equalsIgnoreCase("Access Denied")
                && !ex.getMessage().equalsIgnoreCase("Access is denied"))
                ? ex.getMessage()
                : "Bạn không có quyền thực hiện thao tác này!";
        return new ResponseEntity<>(ApiResponse.error(message), HttpStatus.FORBIDDEN);
    }

    public ResponseEntity<ApiResponse<?>> exception(Exception ex) {
        return exception(ex, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> exception(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        if (request != null) {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html") && !request.getRequestURI().startsWith("/api/")) {
                if (ex instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(ex);
            }
        }
        log.error("Lỗi hệ thống bất ngờ: ", ex);
        return new ResponseEntity<>(ApiResponse.error("Đã xảy ra lỗi nội bộ từ hệ thống. Vui lòng thử lại sau!"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
