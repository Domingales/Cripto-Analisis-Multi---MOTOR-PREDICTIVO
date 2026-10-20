"""SQLite checks for the additive V11 table; Android compilation runs separately."""
import pathlib
import re
import sqlite3
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
DATA = ROOT / 'app/src/main/java/com/domingales/criptoanalisis/multi/data'


class ForecastSchemaTest(unittest.TestCase):
    def test_additive_schema_preserves_existing_signal_and_is_idempotent(self):
        db = sqlite3.connect(':memory:')
        db.execute('CREATE TABLE signal(uid TEXT PRIMARY KEY, status TEXT)')
        db.execute("INSERT INTO signal VALUES('old-signal','FAIL')")
        code = (DATA / 'ForecastCriterionStore.kt').read_text()
        table = re.search(r'db.execSQL\("""(CREATE TABLE.*?)"""\)', code, re.S).group(1)
        index = re.search(r'db.execSQL\("(CREATE INDEX.*?)"\)', code).group(1)
        for _ in range(2):
            db.execute(table)
            db.execute(index)
        self.assertEqual([('old-signal', 'FAIL')], db.execute('SELECT * FROM signal').fetchall())
        self.assertEqual(0, db.execute('SELECT COUNT(*) FROM forecast_criterion').fetchone()[0])

    def test_new_contract_is_atomic_with_signal_and_unique(self):
        db = sqlite3.connect(':memory:')
        code = (DATA / 'ForecastCriterionStore.kt').read_text()
        db.execute(re.search(r'db.execSQL\("""(CREATE TABLE.*?)"""\)', code, re.S).group(1))
        db.execute('CREATE TABLE signal(uid TEXT PRIMARY KEY)')
        values = ('new', 'MAGNITUDE_TIME_V1', 'ADA', '1h', 1, 100, 'BUY', 4, 86400000, 172800000, 432000000, .75, 'PENDING', 'WAIT')
        insert = '''INSERT INTO forecast_criterion(signal_uid,profile,symbol,timeframe,issued_ts,entry,direction,target_pct,horizon_ms,grace_ms,expiry_ms,minimum_fraction,status,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)'''
        with self.assertRaises(sqlite3.IntegrityError):
            with db:
                db.execute("INSERT INTO signal VALUES('new')")
                db.execute(insert, values)
                db.execute(insert, values)
        self.assertEqual(0, db.execute('SELECT COUNT(*) FROM signal').fetchone()[0])
        self.assertEqual(0, db.execute('SELECT COUNT(*) FROM forecast_criterion').fetchone()[0])


if __name__ == '__main__':
    unittest.main()
