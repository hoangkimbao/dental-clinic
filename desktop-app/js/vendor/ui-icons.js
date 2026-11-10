/**
 * DentalCare Clinic Local Vector Icon Engine
 * Pure SVG replacement for FontAwesome icons with 0 external network requests
 * Prevents tofu box glyph errors in offline clinic environments
 */
(function (global) {
    'use strict';

    const NS = 'http://www.w3.org/2000/svg';

    const iconPaths = {
        home: ['M3 10.8 12 3l9 7.8', 'M5.5 9.5V21h13V9.5', 'M9.5 21v-6h5v6'],
        person: ['M20 21a8 8 0 0 0-16 0', 'M12 13a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Z'],
        calendar: ['M7 3v4', 'M17 3v4', 'M4 9h16', 'M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z', 'M8 13h3', 'M8 17h3'],
        clock: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z', 'M12 6v6l4 2'],
        shield: ['M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z', 'm9 12 2 2 4-4'],
        card: ['M3 6h18v12H3z', 'M3 10h18', 'M7 15h3'],
        sparkle: ['m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7L12 3Z'],
        star: ['m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z'],
        tooth: ['M7.5 3.5c-2.4 0-4 1.8-4 4.4 0 3.2 1.7 4.8 2.4 7.2.6 2.1.7 5.4 2.2 5.4 1.4 0 1.5-5.2 3.9-5.2s2.5 5.2 3.9 5.2c1.5 0 1.6-3.3 2.2-5.4.7-2.4 2.4-4 2.4-7.2 0-2.6-1.6-4.4-4-4.4-1.7 0-2.8 1-4.5 1s-2.8-1-4.5-1Z'],
        arrow: ['M5 12h14', 'm13 6 6 6-6 6'],
        arrowLeft: ['M19 12H5', 'm11 18-6-6 6-6'],
        chevron: ['m7 10 5 5 5-5'],
        plus: ['M12 5v14', 'M5 12h14'],
        minus: ['M5 12h14'],
        square: ['M4 4h16v16H4z'],
        close: ['M18 6 6 18', 'M6 6l12 12'],
        question: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z', 'M9.6 9a2.5 2.5 0 1 1 4.6 1.3c-1 1.1-2.2 1.4-2.2 3.2', 'M12 17.5h.01'],
        check: ['m5 12 4 4L19 6'],
        phone: ['M7 3h10v18H7z', 'M10 6h4', 'M11 18h2'],
        location: ['M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z', 'M12 10a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z'],
        chart: ['M4 19V5', 'M4 19h17', 'm7 15 4-4 3 2 5-6'],
        bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9', 'M10 21h4'],
        file: ['M6 2h8l5 5v15H6z', 'M14 2v6h5', 'M9 13h7', 'M9 17h7'],
        search: ['M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z', 'm17 17 4 4'],
        chat: ['M21 11.5a8.4 8.4 0 0 1-9 8.4 9.5 9.5 0 0 1-4-.9L3 21l1.7-4.5a8 8 0 1 1 16.3-5Z'],
        refresh: ['M20 7v5h-5', 'M4 17v-5h5', 'M5.6 9a7 7 0 0 1 12-2L20 12', 'M18.4 15a7 7 0 0 1-12 2L4 12'],
        warning: ['M12 3 2.7 20h18.6L12 3Z', 'M12 9v4', 'M12 17h.01'],
        lock: ['M5 10h14v11H5z', 'M8 10V7a4 4 0 0 1 8 0v3'],
        map: ['M3 6 8 3l8 3 5-3v15l-5 3-8-3-5 3V6Z', 'M8 3v15', 'M16 6v15'],
        box: ['M3 7 12 2l9 5-9 5-9-5Z', 'M3 7v10l9 5 9-5V7', 'M12 12v10'],
        menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
        users: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z'],
        hospital: ['M3 21h18', 'M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16', 'M9 10h6', 'M12 7v6'],
        windows: ['M3 5.5L10.5 4.5v6.5H3V5.5z', 'M11.5 4.3l9.5-1.3v8H11.5V4.3z', 'M3 13h7.5v6.5L3 18.5V13z', 'M11.5 13H21v8l-9.5-1.3V13z'],
        download: ['M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4', 'm7 10 5 5 5-5', 'M12 15V3'],
        volume: ['M11 5 6 9H2v6h4l5 4V5Z', 'M15.5 8.5a5 5 0 0 1 0 7', 'M19 5a9.5 9.5 0 0 1 0 14'],
        paperPlane: ['m22 2-7 20-4-9-9-4Z', 'M22 2 11 13'],
        trash: ['M3 6h18', 'M8 6V4h8v2', 'M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6', 'M10 11v6', 'M14 11v6'],
        sliders: ['M4 6h16', 'M4 12h16', 'M4 18h16', 'M8 4v4', 'M16 10v4', 'M10 16v4'],
        server: ['M3 4h18v6H3z', 'M3 14h18v6H3z', 'M7 7h.01', 'M7 17h.01'],
        satellite: ['m17 7-5 5', 'M19 11a4.2 4.2 0 0 0-4-4', 'M22 14a8.5 8.5 0 0 0-8-8', 'M2 22l6-6']
    };

    const explicit = {
        'fa-tooth': 'tooth',
        'fa-teeth-open': 'tooth',
        'fa-minus': 'minus',
        'fa-square': 'square',
        'fa-xmark': 'close',
        'fa-close': 'close',
        'fa-chart-line': 'chart',
        'fa-gauge-high': 'chart',
        'fa-arrow-up-right-dots': 'chart',
        'fa-user-doctor': 'person',
        'fa-user-plus': 'person',
        'fa-user': 'person',
        'fa-boxes-stacked': 'box',
        'fa-boxes-packing': 'box',
        'fa-box-open': 'box',
        'fa-box': 'box',
        'fa-map-location-dot': 'map',
        'fa-location-dot': 'location',
        'fa-sliders': 'sliders',
        'fa-bell': 'bell',
        'fa-bell-slash': 'bell',
        'fa-file-excel': 'file',
        'fa-file-csv': 'file',
        'fa-file': 'file',
        'fa-floppy-disk': 'file',
        'fa-shield-halved': 'shield',
        'fa-shield': 'shield',
        'fa-rotate': 'refresh',
        'fa-rotate-left': 'refresh',
        'fa-rotate-right': 'refresh',
        'fa-hospital': 'hospital',
        'fa-bolt': 'sparkle',
        'fa-plus': 'plus',
        'fa-scale-balanced': 'check',
        'fa-bars': 'menu',
        'fa-volume-high': 'volume',
        'fa-windows': 'windows',
        'fa-paper-plane': 'paperPlane',
        'fa-trash-can': 'trash',
        'fa-calendar-check': 'calendar',
        'fa-calendar': 'calendar',
        'fa-users-viewfinder': 'users',
        'fa-users': 'users',
        'fa-download': 'download',
        'fa-table': 'card',
        'fa-check': 'check',
        'fa-circle-dot': 'sparkle',
        'fa-star': 'star',
        'fa-satellite-dish': 'satellite',
        'fa-circle-info': 'question',
        'fa-triangle-exclamation': 'warning',
        'fa-id-card': 'card',
        'fa-server': 'server'
    };

    function iconName(el) {
        for (const name of el.classList) {
            if (explicit[name]) return explicit[name];
        }
        const names = [...el.classList];
        if (names.some(n => n.includes('user') || n.includes('doctor'))) return 'person';
        if (names.some(n => n.includes('calendar'))) return 'calendar';
        if (names.some(n => n.includes('shield'))) return 'shield';
        if (names.some(n => n.includes('tooth'))) return 'tooth';
        if (names.some(n => n.includes('chart') || n.includes('gauge'))) return 'chart';
        if (names.some(n => n.includes('map') || n.includes('location'))) return 'map';
        if (names.some(n => n.includes('bell'))) return 'bell';
        if (names.some(n => n.includes('box'))) return 'box';
        if (names.some(n => n.includes('file') || n.includes('excel') || n.includes('csv'))) return 'file';
        if (names.some(n => n.includes('trash'))) return 'trash';
        if (names.some(n => n.includes('download'))) return 'download';
        if (names.some(n => n.includes('check'))) return 'check';
        if (names.some(n => n.includes('minus'))) return 'minus';
        if (names.some(n => n.includes('square'))) return 'square';
        if (names.some(n => n.includes('xmark') || n.includes('close'))) return 'close';
        if (names.some(n => n.includes('plus'))) return 'plus';
        if (names.some(n => n.includes('server'))) return 'server';
        return 'sparkle';
    }

    function drawIcon(svg, key) {
        const paths = iconPaths[key] || iconPaths.sparkle;
        while (svg.firstChild) {
            svg.removeChild(svg.firstChild);
        }
        paths.forEach(d => {
            const path = document.createElementNS(NS, 'path');
            path.setAttribute('d', d);
            svg.appendChild(path);
        });
        svg.dataset.svgIcon = key;
    }

    function replaceIcon(el) {
        if (!(el instanceof Element) || el.dataset.svgReady === 'true') return;
        const iconClass = [...el.classList].find(name => name.startsWith('fa-') && !['fa-solid', 'fa-regular', 'fa-brands', 'fa-fw', 'fa-spin'].includes(name));
        if (!iconClass && !el.classList.contains('tooth-icon')) return;

        const svg = document.createElementNS(NS, 'svg');
        const keptClasses = [...el.classList].filter(name => !name.startsWith('fa-') && !['fas', 'far', 'fab'].includes(name));
        svg.setAttribute('class', [...keptClasses, 'ui-icon'].join(' '));
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('width', '1em');
        svg.setAttribute('height', '1em');
        svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '1.8');
        svg.setAttribute('stroke-linecap', 'round');
        svg.setAttribute('stroke-linejoin', 'round');
        svg.setAttribute('focusable', 'false');
        svg.setAttribute('aria-hidden', el.getAttribute('aria-label') ? 'false' : 'true');

        if (el.hasAttribute('aria-label')) svg.setAttribute('aria-label', el.getAttribute('aria-label'));
        if (el.hasAttribute('title')) svg.setAttribute('title', el.getAttribute('title'));

        drawIcon(svg, iconName(el));
        if (el.style.cssText) svg.style.cssText = el.style.cssText;
        svg.dataset.svgReady = 'true';
        el.replaceWith(svg);
    }

    function scan(root) {
        if (root instanceof Element && (root.matches('i[class*="fa-"]') || root.matches('.tooth-icon'))) {
            replaceIcon(root);
        }
        if (root.querySelectorAll) {
            root.querySelectorAll('i[class*="fa-"], .tooth-icon').forEach(replaceIcon);
        }
    }

    // Auto-init on DOM readiness
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => scan(document));
    } else {
        scan(document);
    }

    // Live MutationObserver for dynamically injected icons
    new MutationObserver(records => {
        records.forEach(record => {
            record.addedNodes.forEach(node => {
                if (node.nodeType === Node.ELEMENT_NODE) {
                    scan(node);
                }
            });
        });
    }).observe(document.documentElement, { childList: true, subtree: true });

    global.DentalCareUiIcons = {
        scan: scan,
        drawIcon: drawIcon
    };
})(typeof window !== 'undefined' ? window : this);
