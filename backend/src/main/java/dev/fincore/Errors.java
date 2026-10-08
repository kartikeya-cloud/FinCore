package dev.fincore;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
@RestControllerAdvice
public class Errors {
 @ExceptionHandler(ApiError.class)
 ResponseEntity<?> business(ApiError e) { return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage())); }
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,HandlerMethodValidationException.class})
 ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("message","Please check the request fields.")); }
}
