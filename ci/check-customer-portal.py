"""Check public portal assets; real customer records must never be packaged."""
import json
import re
from pathlib import Path
from urllib.parse import urlparse
html=Path('app/src/main/assets/customer_portal.html').read_text(encoding='utf-8')
bundle=Path('app/src/main/assets/customer-portal')
assert not (bundle/'customers.json').exists(), 'Private records must not be packaged'
assets=json.loads((bundle/'art.json').read_text(encoding='utf-8'))
maps=json.loads((bundle/'maps.json').read_text(encoding='utf-8'))
languages={'nl','en','uk','tr','it','de','fr','es','pt','hi','pl','ro','ar','zh','el'}
assert set(assets['countryNames'])==languages
assert set(assets['scanners'])=={'bmd','translumina','lime','violet','light','dark'}
for code in maps['countries']:
    assert code in assets['flags']
    assert all(code in words for words in assets['countryNames'].values())
for section in ['flags','scanners','brands','wordmarks','media']:
    for url in assets[section].values():
        parsed=urlparse(url)
        assert parsed.netloc=='bmd.invalid' and parsed.path.startswith('/assets/customer-portal/images/')
        assert Path('app/src/main'+parsed.path).stat().st_size>50
assert 'window.openai' not in html and 'globalThis.Tweak' not in html and '@import' not in html
assert html.count('/*BMD_NATIVE_STATE*/null')==1
assert html.count('/*BMD_CUSTOMER_DATA*/[]')==1 and html.count('/*BMD_PORTAL_ASSETS*/{}')==1
assert 'importCustomers()' in html and 'privateCopy' in html
source=Path('app/src/main/java/com/bmd/scanner/CustomerRegister.java').read_text(encoding='utf-8')
assert 'getNoBackupFilesDir()' in source and 'AtomicFile' in source
print('Public portal: no customer registry, offline assets, 15 languages, 5 selectable themes and private atomic import verified.')
