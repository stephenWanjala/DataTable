#!/usr/bin/env python3
"""Teach an older tag's mkdocs.yml about the version selector.

Tags released before the docs were versioned were built by a configuration that knows nothing
about mike, so rebuilding one as-is would produce a page with no way to reach any other version.
This overlays only the theme wiring: the docs/ content and the nav of that release are left
exactly as they were written.

Run from the repository root, against a checked-out older ref. Doing nothing is the correct
outcome for any ref that already has the selector.
"""

import pathlib
import re
import sys

VERSION_SELECTOR = """
extra:
  version:
    provider: mike
    default: latest
    alias: true
  channel: !ENV [DOCS_CHANNEL, "release"]
"""

path = pathlib.Path("mkdocs.yml")
config = path.read_text()

if "provider: mike" in config:
    print("mkdocs.yml already configures the version selector; nothing to do.")
    sys.exit(0)

# main.html fills in Material's `outdated` banner, which is what tells a reader they have landed
# on something other than the current release.
config = config.replace(
    "theme:\n  name: material\n",
    "theme:\n  name: material\n  custom_dir: overrides\n",
    1,
)
config += VERSION_SELECTOR

# The release predates versioned URLs, so its site_url still points at the unversioned root and
# would hand every page a canonical link that no longer resolves.
config = re.sub(
    r"^site_url:.*$",
    'site_url: !ENV [DOCS_SITE_URL, "https://stephenwanjala.github.io/DataTable/latest/"]',
    config,
    count=1,
    flags=re.MULTILINE,
)

# Keyboard shortcuts in the older guides were written as ++ctrl++ before the extension that
# renders them was enabled, and would otherwise show up as literal plus signs.
if "pymdownx.keys" not in config:
    config = config.replace(
        "markdown_extensions:\n",
        "markdown_extensions:\n  - pymdownx.keys\n",
        1,
    )

path.write_text(config)
print("Overlaid the version selector onto mkdocs.yml.")
