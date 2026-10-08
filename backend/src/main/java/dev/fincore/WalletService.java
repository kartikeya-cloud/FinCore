package dev.fincore;
import java.util.*;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class WalletService {
 public static final String TREASURY="00000000-0000-0000-0000-000000000000";
 private final JdbcTemplate db;
 public WalletService(JdbcTemplate db) { this.db=db; }
 public record Wallet(String id,String name,long balanceCents,String currency) {}
 public record Transfer(String id,String sourceId,String targetId,long amountCents,String note,String category,Instant createdAt) {}
 public Wallet wallet(String id) {
  return db.query("SELECT id,label,balance_cents,currency FROM wallets WHERE id=?",(r,n) -> new Wallet(r.getString(1),r.getString(2),r.getLong(3),r.getString(4)),id).stream().findFirst()
   .orElseThrow(() -> new ApiError(HttpStatus.NOT_FOUND,"Wallet not found."));
 }
 @Transactional
 public Transfer transfer(String source,String target,long amount,String note,String category,String key) {
  if(amount <= 0 || amount > 100000000L) throw new ApiError(HttpStatus.BAD_REQUEST,"Amount must be between ₹0.01 and ₹10,00,000.");
  if(source.equals(target)) throw new ApiError(HttpStatus.BAD_REQUEST,"Choose a different recipient.");
  if(key==null || !key.matches("[A-Za-z0-9_-]{8,100}")) throw new ApiError(HttpStatus.BAD_REQUEST,"Provide a valid Idempotency-Key (8–100 characters).");
  if(note==null || note.length()>200 || category==null || !Set.of("Transfer","Shopping","Food","Travel","Bills","Income").contains(category)) throw new ApiError(HttpStatus.BAD_REQUEST,"Invalid transfer details.");
  // Every operation acquires both wallet locks in the same order, preventing AB/BA deadlocks.
  List<String> ids=new ArrayList<>(List.of(source,target));Collections.sort(ids);
  for(String id:ids) {
   if(db.queryForList("SELECT id FROM wallets WHERE id=? FOR UPDATE",id).isEmpty()) throw new ApiError(HttpStatus.NOT_FOUND,"Recipient wallet not found.");
  }
  List<Transfer> existing=db.query("SELECT * FROM transfers WHERE source_id=? AND idempotency_key=?",(r,n)->map(r),source,key);
  if(!existing.isEmpty()) {
   Transfer t=existing.get(0);
   if(!t.targetId().equals(target) || t.amountCents()!=amount || !t.note().equals(note) || !t.category().equals(category)) throw new ApiError(HttpStatus.CONFLICT,"This request key was already used for different details.");
   return t;
  }
  if(wallet(source).balanceCents()<amount) throw new ApiError(HttpStatus.CONFLICT,"Insufficient wallet balance.");
  String id=UUID.randomUUID().toString();
  db.update("INSERT INTO transfers(id,source_id,target_id,amount_cents,note,category,idempotency_key) VALUES(?,?,?,?,?,?,?)",id,source,target,amount,note,category,key);
  db.update("UPDATE wallets SET balance_cents=balance_cents-? WHERE id=?",amount,source);
  db.update("UPDATE wallets SET balance_cents=balance_cents+? WHERE id=?",amount,target);
  db.update("INSERT INTO ledger_entries(id,transfer_id,wallet_id,delta_cents) VALUES(?,?,?,?)",UUID.randomUUID().toString(),id,source,-amount);
  db.update("INSERT INTO ledger_entries(id,transfer_id,wallet_id,delta_cents) VALUES(?,?,?,?)",UUID.randomUUID().toString(),id,target,amount);
  db.update("INSERT INTO audit_events(id,actor_id,event_type,reference_id) VALUES(?,?,?,?)",UUID.randomUUID().toString(),source,"TRANSFER_POSTED",id);
  return db.queryForObject("SELECT * FROM transfers WHERE id=?",(r,n)->map(r),id);
 }
 private Transfer map(java.sql.ResultSet r) throws java.sql.SQLException {
  return new Transfer(r.getString("id"),r.getString("source_id"),r.getString("target_id"),r.getLong("amount_cents"),r.getString("note"),r.getString("category"),r.getTimestamp("created_at").toInstant());
 }
 public List<Map<String,Object>> history(String owner,int page,int size) {
  return db.query("SELECT t.*,s.label AS source_name,d.label AS target_name FROM transfers t JOIN wallets s ON s.id=t.source_id JOIN wallets d ON d.id=t.target_id WHERE t.source_id=? OR t.target_id=? ORDER BY t.created_at DESC,t.id DESC LIMIT ? OFFSET ?",(r,n) -> {
   Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getString("id"));m.put("amountCents",r.getLong("amount_cents"));m.put("direction",r.getString("source_id").equals(owner)?"OUT":"IN");m.put("counterparty",r.getString("source_id").equals(owner)?r.getString("target_name"):r.getString("source_name"));m.put("note",r.getString("note"));m.put("category",r.getString("category"));m.put("createdAt",r.getTimestamp("created_at").toInstant());return m;
  },owner,owner,size,page*size);
 }
 public Map<String,Object> receipt(String owner,String transferId) {
  List<Transfer> transfers=db.query("SELECT * FROM transfers WHERE id=? AND (source_id=? OR target_id=?)",(r,n)->map(r),transferId,owner,owner);
  if(transfers.isEmpty()) throw new ApiError(HttpStatus.NOT_FOUND,"Transaction not found.");
  List<Map<String,Object>> entries=db.query("SELECT w.label,e.delta_cents FROM ledger_entries e JOIN wallets w ON e.wallet_id=w.id WHERE e.transfer_id=? ORDER BY e.delta_cents",(r,n)->Map.of("wallet",r.getString(1),"deltaCents",r.getLong(2)),transferId);
  return Map.of("transfer",transfers.get(0),"entries",entries);
 }
 public Map<String,Object> summary(String owner) {
  Long incoming=db.queryForObject("SELECT COALESCE(SUM(amount_cents),0) FROM transfers WHERE target_id=?",Long.class,owner);
  Long outgoing=db.queryForObject("SELECT COALESCE(SUM(amount_cents),0) FROM transfers WHERE source_id=?",Long.class,owner);
  List<Map<String,Object>> categories=db.query("SELECT category,SUM(amount_cents) AS total FROM transfers WHERE source_id=? GROUP BY category ORDER BY total DESC",(r,n)->Map.of("category",r.getString(1),"amountCents",r.getLong(2)),owner);
  return Map.of("incomingCents",incoming,"outgoingCents",outgoing,"categories",categories);
 }
}
