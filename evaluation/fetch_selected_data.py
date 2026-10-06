"""Download closed native bars for the registered hourly prospective cohort."""
import argparse
import csv
import datetime as dt
import hashlib
import json
from pathlib import Path
import time
import download_long_history as history
from expanded_history import INTERVALS


def collect(root, protocol, now=None):
    now = now or dt.datetime.now(dt.timezone.utc)
    end = int(now.timestamp()*1000)//300000*300000
    root.mkdir(parents=True,exist_ok=True)
    symbols = sorted({s for s,tf in protocol['selections']} | {'BTC'})
    inputs = {}
    for symbol in symbols:
        intervals = ['4h'] if symbol == 'BTC' else [*INTERVALS, '5m']
        for tf in intervals:
            step = 300000 if tf == '5m' else INTERVALS[tf]
            days = protocol['daily_context_days'] if tf == '1d' else protocol['four_hour_context_days'] if tf == '4h' else protocol['outcome_five_minute_days'] if tf == '5m' else protocol['primary_intraday_days']
            finish = end//step*step
            begin = finish-days*86400000
            cursor, rows = begin, {}
            while cursor < finish:
                url = ('https://data-api.binance.vision/api/v3/klines?symbol='+symbol+'USDT&interval='+tf+
                       '&startTime='+str(cursor)+'&endTime='+str(finish-1)+'&limit=1000')
                page = json.loads(history.get(url))
                if not isinstance(page,list) or not page: raise ValueError('Empty native API page: '+symbol+' '+tf)
                for row in page:
                    history.insert(rows,row,tf,start=begin,end=finish)
                next_cursor = history.timestamp(page[-1][0])+step
                if next_cursor<=cursor: raise ValueError('Stopped pagination')
                cursor=next_cursor
                time.sleep(.12)
            if sorted(rows)!=list(range(begin,finish,step)):
                raise ValueError('Incomplete closed context: '+symbol+' '+tf)
            name=symbol+'USDT_'+tf+'.csv'; path=root/name
            with path.open('w',newline='') as f:
                w=csv.writer(f); w.writerow(['openTime','open','high','low','close','volume','closeTime'])
                w.writerows(rows[t] for t in sorted(rows))
            inputs[name]=dict(sha256=hashlib.sha256(path.read_bytes()).hexdigest(),count=len(rows),first=begin,last=finish-1)
            print(symbol,tf,len(rows),flush=True)
    (root/'manifest.json').write_text(json.dumps(dict(source='OFFICIAL_BINANCE_NATIVE_API',cutoff=end,inputs=inputs),indent=2)+'\n')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True)
    a=p.parse_args()
    protocol=json.loads(Path(__file__).with_name('selected_validation_protocol.json').read_text())
    history.STEPS.update(INTERVALS)
    collect(a.output,protocol)
