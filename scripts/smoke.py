"""Live smoke test against seeded accounts. Posts a ₹1 virtual transfer."""
import json, sys, uuid, urllib.request, urllib.parse, http.cookiejar
base=sys.argv[1] if len(sys.argv)>1 else 'http://localhost:8080'
client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(path,body=None,headers=None):
    req=urllib.request.Request(base+path,data=body,headers=headers or {})
    with client.open(req,timeout=20) as r:
        data=r.read()
        return json.loads(data) if data else None
csrf=request('/api/csrf')
request('/api/login',urllib.parse.urlencode({'email':'demo@fincore.dev','password':'FinCoreDemo!2026'}).encode(),{csrf['headerName']:csrf['token'],'Content-Type':'application/x-www-form-urlencoded'})
csrf=request('/api/csrf');headers={csrf['headerName']:csrf['token'],'Content-Type':'application/json','Idempotency-Key':str(uuid.uuid4())}
assert request('/api/me')['email']=='demo@fincore.dev'
before=request('/api/wallet')['balanceCents']
body=json.dumps({'recipientEmail':'maya@fincore.dev','amount':'1.00','note':'HTTP smoke check','category':'Transfer'}).encode()
posted=request('/api/transfers',body,headers)
replayed=request('/api/transfers',body,headers)
assert posted['id']==replayed['id']
assert request('/api/wallet')['balanceCents']==before-100
receipt=request('/api/transactions/'+posted['id']+'/receipt')
assert len(receipt['entries'])==2
assert sum(x['deltaCents'] for x in receipt['entries'])==0
assert request('/api/transactions')
request('/api/logout',b'',{csrf['headerName']:csrf['token']})
print('PASS: live login, wallet, transfer, idempotent replay, balanced receipt, history, logout')
