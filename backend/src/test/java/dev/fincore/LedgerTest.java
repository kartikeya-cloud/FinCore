package dev.fincore;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:ledger;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=30000","fincore.demo-seed=false"})
class LedgerTest {
 @Autowired WalletService service;@Autowired AccountService accounts;@Autowired JdbcTemplate db;
 String a,b;
 @BeforeEach void setup() {String salt=UUID.randomUUID().toString();a=accounts.register("Alice","alice"+salt+"@test.dev","LongPassword!123");b=accounts.register("Bob","bob"+salt+"@test.dev","LongPassword!123");}
 @Test void transferPostsBalancedEntries() {
  long before=total();var t=service.transfer(a,b,12345,"Lunch","Food","balance_key");
  assertEquals(5000000-12345,service.wallet(a).balanceCents());assertEquals(5000000+12345,service.wallet(b).balanceCents());assertEquals(before,total());
  assertEquals(2L,db.queryForObject("SELECT COUNT(*) FROM ledger_entries WHERE transfer_id=?",Long.class,t.id()));
  assertEquals(0L,db.queryForObject("SELECT SUM(delta_cents) FROM ledger_entries WHERE transfer_id=?",Long.class,t.id()));
 }
 @Test void repeatReturnsOriginalAndChangedPayloadConflicts() {
  var t=service.transfer(a,b,100,"Test","Transfer","repeat_key");
  assertEquals(t.id(),service.transfer(a,b,100,"Test","Transfer","repeat_key").id());assertEquals(4999900,service.wallet(a).balanceCents());
  assertThrows(ApiError.class,()->service.transfer(a,b,101,"Test","Transfer","repeat_key"));
  assertThrows(ApiError.class,()->service.transfer(a,b,100,"Different","Transfer","repeat_key"));
 }
 @Test void invalidAndInsufficientPaymentsDoNotWrite() {
  long before=total();int count=db.queryForObject("SELECT COUNT(*) FROM transfers",Integer.class);
  assertThrows(ApiError.class,()->service.transfer(a,b,5000001,"Test","Transfer","poor_funds"));
  assertThrows(ApiError.class,()->service.transfer(a,a,1,"Test","Transfer","self_payment"));
  assertThrows(ApiError.class,()->service.transfer(a,b,-1,"Test","Transfer","negative_amount"));
  assertThrows(ApiError.class,()->service.transfer(a,UUID.randomUUID().toString(),1,"Test","Transfer","missing_target"));
  assertEquals(before,total());assertEquals(5000000,service.wallet(a).balanceCents());assertEquals(count,db.queryForObject("SELECT COUNT(*) FROM transfers",Integer.class));
 }
 @Test void concurrentSpendingNeverOverdraws() throws Exception {
  long before=total();AtomicInteger success=new AtomicInteger();List<Callable<Void>> work=new ArrayList<>();
  for(int i=0;i<20;i++){final int n=i;work.add(()->{try{service.transfer(a,b,1000000,"Stress","Transfer","stress_key_"+n);success.incrementAndGet();}catch(ApiError e){assertEquals(409,e.status.value());}return null;});}
  parallel(work);assertEquals(5,success.get());assertEquals(0,service.wallet(a).balanceCents());assertEquals(10000000,service.wallet(b).balanceCents());assertEquals(before,total());
 }
 @Test void concurrentIdenticalRequestsPostExactlyOnce() throws Exception {
  List<Callable<String>> work=new ArrayList<>();for(int i=0;i<16;i++)work.add(()->service.transfer(a,b,25000,"Retry","Transfer","same_concurrent_key").id());
  assertEquals(1,new HashSet<>(parallel(work)).size());assertEquals(4975000,service.wallet(a).balanceCents());
 }
 @Test void oppositeDirectionTransfersAvoidDeadlocks() throws Exception {
  List<Callable<Void>> work=new ArrayList<>();for(int i=0;i<20;i++){final int n=i;work.add(()->{service.transfer(n%2==0?a:b,n%2==0?b:a,1000,"Both directions","Transfer","opposite_"+n);return null;});}
  parallel(work);assertEquals(5000000,service.wallet(a).balanceCents());assertEquals(5000000,service.wallet(b).balanceCents());
 }
 @Test void ledgerInsertionFailureRollsBackBalancesAndTransfer() {
  long source=service.wallet(a).balanceCents();int transfers=db.queryForObject("SELECT COUNT(*) FROM transfers",Integer.class);
  db.execute("ALTER TABLE ledger_entries ADD CONSTRAINT test_limit CHECK(delta_cents <> 777)");
  try{assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->service.transfer(a,b,777,"Rollback","Transfer","rollback_test"));}
  finally{db.execute("ALTER TABLE ledger_entries DROP CONSTRAINT test_limit");}
  assertEquals(source,service.wallet(a).balanceCents());assertEquals(transfers,db.queryForObject("SELECT COUNT(*) FROM transfers",Integer.class));
 }
 long total(){return db.queryForObject("SELECT SUM(balance_cents) FROM wallets",Long.class);}
 <T> List<T> parallel(List<Callable<T>> work) throws Exception {
  ExecutorService pool=Executors.newFixedThreadPool(8);
  try{List<T> results=new ArrayList<>();for(Future<T> f:pool.invokeAll(work,30,TimeUnit.SECONDS))results.add(f.get());return results;}finally{pool.shutdownNow();}
 }
}
