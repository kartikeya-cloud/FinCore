package dev.fincore;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","fincore.demo-seed=false"})
@AutoConfigureMockMvc
class ApiSecurityTest {
 @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired WalletService wallets;
 String email,id,target;
 @BeforeEach void setup(){email="api"+UUID.randomUUID()+"@test.dev";id=accounts.register("API User",email,"LongPassword!123");target="recipient"+UUID.randomUUID()+"@test.dev";accounts.register("Recipient",target,"LongPassword!123");}
 @Test void privateEndpointsRequireLogin() throws Exception {mvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());}
 @Test void stateChangesRequireCsrf() throws Exception {mvc.perform(post("/api/transfers").with(user(email)).contentType("application/json").header("Idempotency-Key","api_csrf_key").content(body("1.00"))).andExpect(status().isForbidden());}
 @Test void loginCreatesWorkingSession() throws Exception {
  var result=mvc.perform(post("/api/login").with(csrf()).param("email",email).param("password","LongPassword!123")).andExpect(status().isOk()).andReturn();
  mvc.perform(get("/api/me").session((MockHttpSession)result.getRequest().getSession())).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
 }
 @Test void invalidCredentialsFail() throws Exception {mvc.perform(post("/api/login").with(csrf()).param("email",email).param("password","wrong")).andExpect(status().isUnauthorized());}
 @Test void apiDerivesSenderFromSession() throws Exception {
  mvc.perform(post("/api/transfers").with(user(email)).with(csrf()).contentType("application/json").header("Idempotency-Key","api_valid_key").content(body("12.34"))).andExpect(status().isOk()).andExpect(jsonPath("$.sourceId").value(id)).andExpect(jsonPath("$.amountCents").value(1234));
 }
 @Test void strangerCannotReadReceipt() throws Exception {
  String recipientId=accounts.id(target);var transfer=wallets.transfer(id,recipientId,10,"Private","Transfer","receipt_privacy");
  String stranger="stranger"+UUID.randomUUID()+"@test.dev";accounts.register("Stranger",stranger,"LongPassword!123");
  mvc.perform(get("/api/transactions/"+transfer.id()+"/receipt").with(user(stranger))).andExpect(status().isNotFound());
  mvc.perform(get("/api/transactions/"+transfer.id()+"/receipt").with(user(email))).andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(2));
 }
 @Test void extraDecimalPlacesAreRejected() throws Exception {mvc.perform(post("/api/transfers").with(user(email)).with(csrf()).contentType("application/json").header("Idempotency-Key","api_decimal_key").content(body("1.001"))).andExpect(status().isBadRequest());}
 @Test void invalidPaginationRejected() throws Exception {mvc.perform(get("/api/transactions?page=-1").with(user(email))).andExpect(status().isBadRequest());}
 @Test void logoutInvalidatesSession() throws Exception {
  var result=mvc.perform(post("/api/login").with(csrf()).param("email",email).param("password","LongPassword!123")).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession();mvc.perform(post("/api/logout").session(session).with(csrf())).andExpect(status().isNoContent());org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
 }
 String body(String amount){return "{\"recipientEmail\":\""+target+"\",\"amount\":\""+amount+"\",\"note\":\"Test\",\"category\":\"Transfer\"}";}
}
