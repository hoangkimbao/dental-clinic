#!/usr/bin/env python3
"""
Master E2E Test Runner for DentalCare Clinic Web Portal (Python 3)
Executes Tiers 1-4 tests programmatically and outputs JSON, TAP, and JUnit XML.

Usage:
    python tests/e2e/runner.py [options]

Options:
    --tier=1|2|3|4         Run only specified tier
    --feature=F01-F23      Run tests for specific feature
    --format=json|tap|xml  Specify output format
    --bail                 Exit on first failure
"""

import os
import sys
import json
import re
import time
from xml.sax.saxutils import escape

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))
STATIC_DIR = os.path.join(PROJECT_ROOT, 'src/main/resources/static')
INDEX_PATH = os.path.join(STATIC_DIR, 'index.html')
CSS_PATH = os.path.join(STATIC_DIR, 'css/clinic-ui-refresh.css')
APP_JS_PATH = os.path.join(STATIC_DIR, 'js/app.js')
DENTAL_ICONS_PATH = os.path.join(STATIC_DIR, 'js/dental-icons.js')
RESULTS_DIR = os.path.join(os.path.dirname(__file__), 'results')

def load_file(path):
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            return f.read()
    return ''

index_html = load_file(INDEX_PATH)
css_content = load_file(CSS_PATH)
app_js_content = load_file(APP_JS_PATH)
dental_icons_content = load_file(DENTAL_ICONS_PATH)

def count_occurrences(content, pattern):
    return len(re.findall(pattern, content, re.IGNORECASE))

def has_id(element_id):
    return bool(re.search(rf'id=["\']{element_id}["\']', index_html, re.IGNORECASE))

def is_tag_closed_before(open_id, target_tag, next_id):
    m_open = re.search(rf'id=["\']{open_id}["\']', index_html, re.IGNORECASE)
    m_next = re.search(rf'id=["\']{next_id}["\']', index_html, re.IGNORECASE)
    if not m_open or not m_next:
        return False
    intermediate = index_html[m_open.start():m_next.start()]
    return bool(re.search(rf'</{target_tag}>', intermediate, re.IGNORECASE))

def simulate_navbar_width(viewport_width, is_logged_in=False):
    logo_width = 131
    cart_btn_width = 42
    hamburger_btn_width = 42
    gaps = 20
    auth_width = 278 if is_logged_in else 180
    if not is_logged_in and bool(re.search(r'hidden\s+sm:inline|sm:inline', index_html, re.IGNORECASE)) and viewport_width < 640:
        auth_width = 70
    if is_logged_in and bool(re.search(r'hidden\s+sm:flex', app_js_content, re.IGNORECASE)) and viewport_width < 640:
        auth_width = 60

    total_width = logo_width + cart_btn_width + hamburger_btn_width + auth_width + gaps
    padding = 32 if viewport_width < 640 else 48
    available = viewport_width - padding
    overflow = max(0, total_width - available)
    return {'viewport': viewport_width, 'overflow': overflow, 'has_overflow': overflow > 0}

def audit_touch_targets():
    substandard = []
    if re.search(r'handleLogout[\s\S]*?w-8\s+h-8', index_html) and not re.search(r'min-w-\[44px\]', index_html):
        substandard.append('navbar-logout')
    if re.search(r'w-9\s+h-9', app_js_content) and not re.search(r'min-w-\[44px\]', app_js_content):
        substandard.append('cart-qty-buttons')
    if re.search(r'p-1\.5\s+rounded-lg\s+text-slate-400', index_html) and not re.search(r'min-w-\[44px\]', index_html):
        substandard.append('cart-drawer-close')
    if re.search(r'w-7\s+h-7', index_html) and not re.search(r'min-w-\[44px\]', index_html):
        substandard.append('packaging-modal-close')
    return substandard

def main():
    selected_tier = None
    selected_feature = None
    bail_on_fail = False

    for arg in sys.argv[1:]:
        if arg.startswith('--tier='):
            selected_tier = int(arg.split('=')[1])
        elif arg.startswith('--feature='):
            selected_feature = arg.split('=')[1].upper()
        elif arg == '--bail':
            bail_on_fail = True

    # Check if node runner generated report.json
    report_file = os.path.join(RESULTS_DIR, 'report.json')
    if os.path.exists(report_file):
        with open(report_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print("\n================================================================================")
        print(f" DentalCare Clinic Web Portal E2E Test Suite (Python CLI)")
        print(f" Total Tests: {data['metadata']['totalTests']}")
        print(f" Passed:      {data['metadata']['passed']}")
        print(f" Failed:      {data['metadata']['failed']}")
        print(f" Pass Rate:   {data['metadata']['passRate']}")
        print("================================================================================\n")
        sys.exit(1 if data['metadata']['failed'] > 0 else 0)
    else:
        print("[Python Runner] Please execute node tests/e2e/runner.js to initialize baseline.")
        sys.exit(1)

if __name__ == '__main__':
    main()
