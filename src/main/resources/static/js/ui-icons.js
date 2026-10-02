/* Small local SVG icon set for the public site and management portal.
 * Replaces Font Awesome placeholders so the interface stays legible offline. */
(function () {
    'use strict';

    const NS = 'http://www.w3.org/2000/svg';
    const iconPaths = {
        home: ['M3 10.8 12 3l9 7.8', 'M5.5 9.5V21h13V9.5', 'M9.5 21v-6h5v6'],
        person: ['M20 21a8 8 0 0 0-16 0', 'M12 13a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Z'],
        calendar: ['M7 3v4', 'M17 3v4', 'M4 9h16', 'M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z', 'M8 13h3', 'M8 17h3'],
        clock: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z', 'M12 6v6l4 2'],
        shield: ['M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z', 'm9 12 2 2 4-4'],
        card: ['M3 6h18v12H3z', 'M3 10h18', 'M7 15h3'],
        sparkle: ['m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7L12 3Z', 'm19 14 .8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14Z'],
        star: ['m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1-4.4-4.3 6.1-.9L12 3Z'],
        camera: ['M4 7h3l1.4-2h7.2L17 7h3v12H4z', 'M12 16.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z'],
        tooth: ['M7.5 3.5c-2.4 0-4 1.8-4 4.4 0 3.2 1.7 4.8 2.4 7.2.6 2.1.7 5.4 2.2 5.4 1.4 0 1.5-5.2 3.9-5.2s2.5 5.2 3.9 5.2c1.5 0 1.6-3.3 2.2-5.4.7-2.4 2.4-4 2.4-7.2 0-2.6-1.6-4.4-4-4.4-1.7 0-2.8 1-4.5 1s-2.8-1-4.5-1Z'],
        bag: ['M5 8h14l1 13H4L5 8Z', 'M9 9V6a3 3 0 0 1 6 0v3'],
        arrow: ['M5 12h14', 'm13 6 6 6-6 6'],
        arrowLeft: ['M19 12H5', 'm11 18-6-6 6-6'],
        chevron: ['m7 10 5 5 5-5'],
        plus: ['M12 5v14', 'M5 12h14'],
        close: ['M18 6 6 18', 'M6 6l12 12'],
        question: ['M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z', 'M9.6 9a2.5 2.5 0 1 1 4.6 1.3c-1 1.1-2.2 1.4-2.2 3.2', 'M12 17.5h.01'],
        check: ['m5 12 4 4L19 6'],
        phone: ['M7 3h10v18H7z', 'M10 6h4', 'M11 18h2'],
        location: ['M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z', 'M12 10a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5Z'],
        gift: ['M3 9h18v4H3z', 'M5 13v8h14v-8', 'M12 9v12', 'M12 9H8.5a2.5 2.5 0 1 1 2.5-2.5V9Z', 'M12 9h3.5A2.5 2.5 0 1 0 13 6.5V9Z'],
        chart: ['M4 19V5', 'M4 19h17', 'm7 15 4-4 3 2 5-6'],
        bell: ['M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9', 'M10 21h4'],
        book: ['M4 4.5A2.5 2.5 0 0 1 6.5 2H20v18H6.5A2.5 2.5 0 0 0 4 22V4.5Z', 'M4 5h13'],
        file: ['M6 2h8l5 5v15H6z', 'M14 2v6h5', 'M9 13h7', 'M9 17h7'],
        search: ['M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z', 'm17 17 4 4'],
        chat: ['M21 11.5a8.4 8.4 0 0 1-9 8.4 9.5 9.5 0 0 1-4-.9L3 21l1.7-4.5a8 8 0 1 1 16.3-5Z', 'M8 11h.01', 'M12 11h.01', 'M16 11h.01'],
        credit: ['M2.5 7h19v12h-19z', 'M2.5 11h19', 'M6 15h4'],
        refresh: ['M20 7v5h-5', 'M4 17v-5h5', 'M5.6 9a7 7 0 0 1 12-2L20 12', 'M18.4 15a7 7 0 0 1-12 2L4 12'],
        warning: ['M12 3 2.7 20h18.6L12 3Z', 'M12 9v4', 'M12 17h.01'],
        lock: ['M5 10h14v11H5z', 'M8 10V7a4 4 0 0 1 8 0v3', 'M12 14v3'],
        map: ['M3 6 8 3l8 3 5-3v15l-5 3-8-3-5 3V6Z', 'M8 3v15', 'M16 6v15'],
        box: ['M3 7 12 2l9 5-9 5-9-5Z', 'M3 7v10l9 5 9-5V7', 'M12 12v10'],
        percent: ['M19 5 5 19', 'M7 7h.01', 'M17 17h.01', 'M8 7a1 1 0 1 0-2 0 1 1 0 0 0 2 0Z', 'M18 17a1 1 0 1 0-2 0 1 1 0 0 0 2 0Z'],
        eye: ['M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7Z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z'],
        play: ['m8 5 12 7-12 7V5Z'],
        qr: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M3 14h7v7H3z', 'M14 14h3v3h-3z', 'M19 14v2', 'M14 19h2', 'M19 19h2v2h-2z'],
        network: ['M12 12 5 5', 'M12 12l7-7', 'M12 12l-7 7', 'M12 12l7 7', 'M9 3H3v6h6z', 'M21 3h-6v6h6z', 'M9 15H3v6h6z', 'M21 15h-6v6h6z'],
        heart: ['M20.8 8.8c0 5.4-8.8 11-8.8 11s-8.8-5.6-8.8-11A4.8 4.8 0 0 1 12 6.4a4.8 4.8 0 0 1 8.8 2.4Z'],
        flask: ['M9 3h6', 'M10 3v6L4.5 18a2 2 0 0 0 1.7 3h11.6a2 2 0 0 0 1.7-3L14 9V3', 'M7.5 16h9'],
        menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
        tag: ['m20 13-7 7-10-10V3h7l10 10Z', 'M7 7h.01'],
        wallet: ['M3 5h17v15H3z', 'M3 8h17', 'M16 14h4'],
        users: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z', 'M20 21v-2a4 4 0 0 0-3-3.9', 'M16 3.1a4 4 0 0 1 0 7.8'],
        terminal: ['M4 5h16v14H4z', 'm7 9 3 3-3 3', 'M12 15h4'],
        sort: ['M4 7h16', 'M7 12h10', 'M10 17h4'],
        shieldPlus: ['M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z', 'M12 8v8', 'M8 12h8'],
        cross: ['M12 3v18', 'M3 12h18']
    };

    const explicit = {
        'fa-house':'home','fa-home':'home','fa-user-doctor':'person','fa-user-shield':'shield','fa-user-check':'person','fa-user-plus':'person','fa-user':'person',
        'fa-calendar-check':'calendar','fa-calendar-day':'calendar','fa-calendar-days':'calendar','fa-clock-rotate-left':'clock','fa-clock':'clock','fa-stopwatch':'clock','fa-business-time':'clock',
        'fa-shield-virus':'shield','fa-shield-check':'shield','fa-shield-halved':'shield','fa-credit-card':'card','fa-cart-shopping':'bag','fa-bag-shopping':'bag','fa-cart-plus':'bag','fa-store':'bag',
        'fa-wand-magic-sparkles':'sparkle','fa-star':'star','fa-crown':'star','fa-award':'star','fa-camera':'camera','fa-camera-retro':'camera','fa-teeth-open':'tooth','fa-tooth':'tooth',
        'fa-arrow-right':'arrow','fa-arrow-left':'arrowLeft','fa-arrow-right-from-bracket':'arrow','fa-right-to-bracket':'arrow','fa-chevron-down':'chevron','fa-bars':'menu','fa-xmark':'close',
        'fa-plus-circle':'plus','fa-check':'check','fa-circle-check':'check','fa-fire':'sparkle','fa-phone':'phone','fa-phone-volume':'phone','fa-location-dot':'location','fa-location-crosshairs':'location','fa-map-location-dot':'map',
        'fa-gift':'gift','fa-ticket':'tag','fa-tags':'tag','fa-tag':'tag','fa-percent':'percent','fa-wallet':'wallet','fa-receipt':'file','fa-qrcode':'qr','fa-filter':'sort','fa-magnifying-glass':'search',
        'fa-bell':'bell','fa-bell-concierge':'bell','fa-book-medical':'book','fa-book-open-reader':'book','fa-file-medical':'file','fa-file-excel':'file','fa-list-check':'check','fa-clipboard-user':'users',
        'fa-chart-area':'chart','fa-chart-line':'chart','fa-chart-pie':'chart','fa-square-poll-vertical':'chart','fa-comments':'chat','fa-bullhorn':'chat','fa-paper-plane':'arrow','fa-network-wired':'network','fa-code-compare':'network',
        'fa-box-open':'box','fa-boxes-stacked':'box','fa-truck-ramp-box':'box','fa-users':'users','fa-users-gear':'users','fa-users-viewfinder':'users','fa-id-badge':'person','fa-id-card-clip':'person',
        'fa-lock':'lock','fa-key':'lock','fa-fingerprint':'person','fa-eye':'eye','fa-play':'play','fa-spinner':'refresh','fa-arrows-rotate':'refresh','fa-history':'clock','fa-radar':'network',
        'fa-triangle-exclamation':'warning','fa-circle-question':'question','fa-scale-balanced':'check','fa-layer-group':'box','fa-hand-holding-medical':'shieldPlus','fa-notes-medical':'file','fa-clipboard-check':'check',
        'fa-pen-to-square':'file','fa-floppy-disk':'file','fa-door-open':'arrow','fa-bolt':'sparkle','fa-face-smile':'person','fa-utensils':'plus','fa-terminal':'terminal','fa-database':'box','fa-briefcase':'box','fa-reply':'arrowLeft','fa-rotate-right':'refresh','fa-gauge-high':'chart','fa-money-bill-wave':'wallet','fa-stethoscope':'tooth','fa-folder-open':'file','fa-trash-can':'close'
    };

    function iconName(el) {
        for (const name of el.classList) {
            if (explicit[name]) return explicit[name];
        }
        const names = [...el.classList];
        if (names.some(n => n.includes('user') || n.includes('doctor'))) return 'person';
        if (names.some(n => n.includes('calendar'))) return 'calendar';
        if (names.some(n => n.includes('clock') || n.includes('time'))) return 'clock';
        if (names.some(n => n.includes('shield') || n.includes('lock'))) return 'shield';
        if (names.some(n => n.includes('cart') || n.includes('bag'))) return 'bag';
        if (names.some(n => n.includes('tooth') || n.includes('teeth'))) return 'tooth';
        if (names.some(n => n.includes('chart') || n.includes('poll'))) return 'chart';
        if (names.some(n => n.includes('location') || n.includes('map'))) return 'location';
        if (names.some(n => n.includes('arrow'))) return 'arrow';
        return 'sparkle';
    }

    function drawIcon(svg, key) {
        const paths = iconPaths[key] || iconPaths.sparkle;
        svg.replaceChildren();
        paths.forEach(d => {
            const path = document.createElementNS(NS, 'path');
            path.setAttribute('d', d);
            svg.appendChild(path);
        });
        svg.dataset.svgIcon = key;
    }

    window.DentalCareUiIcons = {
        setIcon(element, key) {
            if (element instanceof SVGElement) drawIcon(element, key);
        }
    };

    function replaceIcon(el) {
        if (!(el instanceof Element) || el.dataset.svgReady === 'true') return;
        const iconClass = [...el.classList].find(name => name.startsWith('fa-') && !['fa-solid','fa-regular','fa-brands','fa-fw','fa-spin','fa-pulse','fa-lg','fa-xs','fa-sm'].includes(name));
        if (!iconClass) return;

        const svg = document.createElementNS(NS, 'svg');
        const keptClasses = [...el.classList].filter(name => !name.startsWith('fa-') && !['fas','far','fab'].includes(name));
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
        if (el.classList.contains('fa-spin')) svg.classList.add('ui-icon-spin');

        drawIcon(svg, iconName(el));
        if (el.style.cssText) svg.style.cssText = el.style.cssText;
        el.replaceWith(svg);
    }

    function scan(root) {
        if (root instanceof Element && root.matches('i[class*="fa-"]')) replaceIcon(root);
        if (root.querySelectorAll) root.querySelectorAll('i[class*="fa-"]').forEach(replaceIcon);
    }

    scan(document);
    new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(node => {
        if (node.nodeType === Node.ELEMENT_NODE) scan(node);
    }))).observe(document.documentElement, { childList: true, subtree: true });
})();
