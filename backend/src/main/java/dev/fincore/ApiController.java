package dev.fincore;
import java.util.*;
import java.security.Principal;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api")
public class ApiController {
 private final AccountService accounts;private final WalletService wallets;
 public ApiController(AccountService accounts,WalletService wallets) {this.accounts=accounts;this.wallets=wallets;}
 public record Registration(@NotBlank @Size(max=80) String name,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=10,max=64) String password) {}
 public record Send(@NotBlank @Email String recipientEmail,@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer=7,fraction=2) BigDecimal amount,@NotNull @Size(max=200) String note,@NotBlank String category) {}
 @GetMapping("/csrf") Object csrf(CsrfToken token) {return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) Object register(@Valid @RequestBody Registration r) {return Map.of("id",accounts.register(r.name(),r.email(),r.password()));}
 @GetMapping("/me") Object me(Principal p) {return accounts.profile(p.getName());}
 @GetMapping("/wallet") Object wallet(Principal p) {return wallets.wallet(accounts.id(p.getName()));}
 @GetMapping("/summary") Object summary(Principal p) {return wallets.summary(accounts.id(p.getName()));}
 @GetMapping("/recipient") Object recipient(@RequestParam @Email @NotBlank String email) {return accounts.recipient(email);}
 @GetMapping("/transactions") Object history(Principal p,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {return wallets.history(accounts.id(p.getName()),page,size);}
 @GetMapping("/transactions/{id}/receipt") Object receipt(Principal p,@PathVariable String id) {return wallets.receipt(accounts.id(p.getName()),id);}
 @PostMapping("/transfers") Object send(Principal p,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Send r) {
  String target=(String)accounts.recipient(r.recipientEmail()).get("id");
  return wallets.transfer(accounts.id(p.getName()),target,r.amount().movePointRight(2).longValueExact(),r.note(),r.category(),key);
 }
}
