package dev.fincore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="fincore.demo-seed=false")
@Testcontainers
class PostgresLedgerIT {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired AccountService accounts;@Autowired WalletService wallets;
 @Test void migrationsAndLocksWorkOnPostgres(){String a=accounts.register("Alice","alice@postgres.dev","LongPassword!123"),b=accounts.register("Bob","bob@postgres.dev","LongPassword!123");var t=wallets.transfer(a,b,10000,"Postgres","Transfer","postgres_key");assertEquals(t.id(),wallets.transfer(a,b,10000,"Postgres","Transfer","postgres_key").id());assertEquals(4990000,wallets.wallet(a).balanceCents());assertEquals(5010000,wallets.wallet(b).balanceCents());}
}
