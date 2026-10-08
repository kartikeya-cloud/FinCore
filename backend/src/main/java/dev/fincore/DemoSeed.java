package dev.fincore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="fincore.demo-seed",havingValue="true")
public class DemoSeed implements CommandLineRunner {
 private final AccountService accounts;private final WalletService wallets;private final JdbcTemplate db;
 public DemoSeed(AccountService accounts,WalletService wallets,JdbcTemplate db) {this.accounts=accounts;this.wallets=wallets;this.db=db;}
 @org.springframework.transaction.annotation.Transactional
 public void run(String... args) {
  if(!db.queryForList("SELECT id FROM app_users WHERE email=?","demo@fincore.dev").isEmpty())return;
  String demo=accounts.register("Kartikeya","demo@fincore.dev","FinCoreDemo!2026");
  String maya=accounts.register("Maya Shah","maya@fincore.dev","FinCoreDemo!2026");
  String arjun=accounts.register("Arjun Mehta","arjun@fincore.dev","FinCoreDemo!2026");
  wallets.transfer(demo,maya,125000,"Weekend groceries","Shopping","seed_shopping");
  wallets.transfer(demo,arjun,45000,"Dinner at the café","Food","seed_dinner");
  wallets.transfer(maya,demo,240000,"Project reimbursement","Transfer","seed_reimbursement");
  wallets.transfer(demo,arjun,189900,"Monthly internet","Bills","seed_internet");
  wallets.transfer(demo,maya,320000,"Train tickets","Travel","seed_tickets");
 }
}
