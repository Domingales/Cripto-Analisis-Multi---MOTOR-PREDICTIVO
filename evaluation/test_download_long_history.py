import hashlib
import io
import json
import unittest
import zipfile
from unittest.mock import patch
import download_long_history as history


def candle(t=history.START, interval='1h'):
    return [t, '1.0', '2.0', '.5', '1.5', '20', t + history.STEPS[interval] - 1]


class HistoryTest(unittest.TestCase):
    def test_microseconds_and_cutoff(self):
        row = candle()
        row[0] *= 1000
        row[6] *= 1000
        data = {}
        history.insert(data, row, '1h')
        self.assertEqual(list(data), [history.START])
        history.insert(data, candle(history.END), '1h')
        self.assertEqual(len(data), 1)

    def test_invalid_and_conflicting_values(self):
        for index, value in [(2, '.9'), (3, '1.8'), (5, '-1'), (4, 'nan'), (6, history.START + 1)]:
            bad = candle()
            bad[index] = value
            with self.assertRaises(ValueError):
                history.insert({}, bad, '1h')
        data = {}
        history.insert(data, candle(), '1h')
        same = candle()
        same[1] = '1.000'
        history.insert(data, same, '1h')
        different = candle()
        different[4] = '1.6'
        with self.assertRaises(ValueError):
            history.insert(data, different, '1h')

    def test_gap_ranges_and_open_final_candle(self):
        start = history.START
        step = history.STEPS['4h']
        self.assertEqual(history.missing_ranges({start: candle(start)}, '4h', start, start + 2*step + step//2),
                         [[start + step, start + 2*step]])

    def test_checksum_failure_does_not_accept_any_rows(self):
        b = io.BytesIO()
        with zipfile.ZipFile(b, 'w') as z:
            z.writestr('test.csv', ','.join(map(str, candle())))
        data, manifest = {}, dict(sources=[], errors=[])
        with patch.object(history, 'get', side_effect=[b.getvalue(), b'wrong test.zip']):
            with self.assertRaises(ValueError):
                history.archive('ADAUSDT', '1h', 'daily', '2020-10-02', data, manifest)
        self.assertFalse(data)
        self.assertFalse(manifest['sources'])

    def test_api_empty_gap_stays_missing(self):
        data, manifest = {}, dict(sources=[], errors=[])
        with patch.object(history, 'get', return_value=b'[]'):
            history.api_range('ADAUSDT', '1h', history.START, history.START + 3600000, data, manifest)
        self.assertFalse(data)
        self.assertEqual(len(manifest['sources']), 1)

    def test_api_range_filters_and_hash(self):
        blob = json.dumps([candle(), candle(history.START + 3600000)]).encode()
        data, manifest = {}, dict(sources=[], errors=[])
        with patch.object(history, 'get', return_value=blob):
            history.api_range('ADAUSDT', '1h', history.START, history.START + 3600000, data, manifest)
        self.assertEqual(len(data), 1)
        self.assertEqual(manifest['sources'][0]['sha256'], hashlib.sha256(blob).hexdigest())


if __name__ == '__main__':
    unittest.main()
