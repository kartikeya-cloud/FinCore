package dev.fincore;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AccountService {
 private final JdbcTemplate db;private final PasswordEncoder passwords;private final WalletService wallets;
 public AccountService(JdbcTemplate db,PasswordEncoder passwords,WalletService wallets) {this.db=db;this.passwords=passwords;this.wallets=wallets;}
 @Transactional
 public String register(String name,String email,String password) {
  email=email.toLowerCase().trim();name=name.trim();
  if(name.isBlank() || name.length()>80 || email.length()>254 || password.length()<10 || password.length()>64 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ApiError(HttpStatus.BAD_REQUEST,"Use a name and a password of 10–64 characters (maximum 72 UTF-8 bytes).");
  if(!db.queryForList("SELECT id FROM app_users WHERE email=?",email).isEmpty()) throw new ApiError(HttpStatus.CONFLICT,"This email is already registered.");
  String id=UUID.randomUUID().toString();
  try {db.update("INSERT INTO app_users(id,email,name,password_hash) VALUES(?,?,?,?)",id,email,name,passwords.encode(password));}
  catch(org.springframework.dao.DuplicateKeyException e) {throw new ApiError(HttpStatus.CONFLICT,"This email is already registered.");}
  db.update("INSERT INTO wallets(id,owner_id,label,balance_cents) VALUES(?,?,?,0)",id,id,name);
  wallets.transfer(WalletService.TREASURY,id,5000000,"Welcome to FinCore · simulated opening funds","Income","opening_"+id);
  db.update("INSERT INTO audit_events(id,actor_id,event_type,reference_id) VALUES(?,?,?,?)",UUID.randomUUID().toString(),id,"ACCOUNT_CREATED",id);
  return id;
 }
 public String id(String email) {return db.queryForObject("SELECT id FROM app_users WHERE email=?",String.class,email);}
 public Map<String,Object> profile(String email) { return db.queryForObject("SELECT id,name,email FROM app_users WHERE email=?",(r,n)->Map.of("id",r.getString(1),"name",r.getString(2),"email",r.getString(3)),email); }
 public Map<String,Object> recipient(String email) {
  return db.query("SELECT id,name,email FROM app_users WHERE email=?",(r,n)->Map.<String,Object>of("id",r.getString(1),"name",r.getString(2),"email",r.getString(3)),email.toLowerCase().trim()).stream().findFirst().orElseThrow(()->new ApiError(HttpStatus.NOT_FOUND,"No account with that email. Ask the recipient to register first."));
 }
}
