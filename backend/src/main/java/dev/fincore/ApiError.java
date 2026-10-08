package dev.fincore;
import org.springframework.http.HttpStatus;
public class ApiError extends RuntimeException {
 public final HttpStatus status;
 public ApiError(HttpStatus status,String message) { super(message);this.status=status; }
}
