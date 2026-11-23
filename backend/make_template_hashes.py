"""Hash every file in freshly generated scaffold projects so the scanner can ignore them.

    npx create-react-app tmp-cra && laravel new tmp-laravel   # any scaffolds your users tend to start from
    python make_template_hashes.py tmp-cra tmp-laravel > templates.txt
    python scanner.py <repo> --author me --templates templates.txt
"""
import os
import sys

import filtering as F

for root_dir in sys.argv[1:]:
    for dirpath, dirnames, filenames in os.walk(root_dir):
        dirnames[:] = [d for d in dirnames if d not in F.VENDOR_DIRS]
        for fn in filenames:
            text = F.read_text(os.path.join(dirpath, fn))
            if text and text.strip():
                print(F.content_hash(text))
