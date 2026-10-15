"""Fetch a public upstream artifact into an ignored Maven cache; verify its pinned hash."""
from pathlib import Path
import hashlib, json, urllib.request

ROOT = Path(__file__).resolve().parent.parent
source = json.loads((ROOT / 'scripts/badpackets-provenance.json').read_text(encoding='utf-8'))
directory = ROOT / 'local-maven/lol/bai/badpackets/neo-0.8.2'
directory.mkdir(parents=True, exist_ok=True)
jar = directory / 'badpackets-neo-0.8.2.jar'
if not jar.exists() or hashlib.sha512(jar.read_bytes()).hexdigest() != source['hashes']['sha512']:
    with urllib.request.urlopen(source['download'], timeout=90) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != source['hashes']['sha512']:
        raise RuntimeError('BadPackets SHA-512 verification failed')
    jar.write_bytes(data)
(directory / 'badpackets-neo-0.8.2.pom').write_text('''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
<groupId>lol.bai</groupId><artifactId>badpackets</artifactId><version>neo-0.8.2</version>
</project>
''', encoding='utf-8')
print('Verified BadPackets NeoForge 0.8.2 (public Modrinth artifact)')
