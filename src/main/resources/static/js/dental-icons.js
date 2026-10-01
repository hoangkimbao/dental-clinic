/**
 * DentalCare Luxury Clinic - Bespoke Vector Iconography & Brand System
 * High-definition, scalable SVG vector assets for Dental Services, Feature Badges & Brand Logos.
 * 
 * Standards:
 * - viewBox: "0 0 64 64" (Services & Badges), "0 0 240 52" (Full Logo), "0 0 48 48" (Logo Mark)
 * - Multi-stop linear & radial gradients with distinct IDs
 * - vector-effect="non-scaling-stroke" for razor-sharp rendering on all screens (Retina to 4K)
 * - Semantic dental & medical motifs compliant with R4 & R5
 */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.DentalIcons = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    // Unique ID generator to avoid SVG gradient ID collisions
    let idCounter = 0;
    function uid(prefix) {
        idCounter += 1;
        return `${prefix}-${idCounter}`;
    }

    /**
     * BESPOKE BRAND LOGOS
     */
    const logos = {
        // Diamond smile luxury emblem mark only (viewBox: 0 0 48 48)
        mark: function (opts = {}) {
            const cls = opts.className || 'w-10 h-10';
            const gid = uid('di-logo');
            return `<svg class="${cls}" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="DentalCare Luxury Emblem">
                <defs>
                    <linearGradient id="${gid}-grad" x1="4" y1="4" x2="44" y2="44" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#087a72"/>
                        <stop offset="50%" stop-color="#0d9488"/>
                        <stop offset="100%" stop-color="#0284c7"/>
                    </linearGradient>
                    <linearGradient id="${gid}-gold" x1="16" y1="28" x2="36" y2="40" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#fbbf24"/>
                        <stop offset="50%" stop-color="#f59e0b"/>
                        <stop offset="100%" stop-color="#d97706"/>
                    </linearGradient>
                    <linearGradient id="${gid}-facet" x1="12" y1="10" x2="36" y2="26" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95"/>
                        <stop offset="100%" stop-color="#ccfbf1" stop-opacity="0.3"/>
                    </linearGradient>
                    <filter id="${gid}-shadow" x="-10%" y="-10%" width="120%" height="130%" filterUnits="userSpaceOnUse">
                        <feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#087a72" flood-opacity="0.3"/>
                    </filter>
                </defs>
                <!-- Outer Squircle Container -->
                <rect x="2" y="2" width="44" height="44" rx="14" fill="url(#${gid}-grad)" filter="url(#${gid}-shadow)"/>
                <rect x="2.5" y="2.5" width="43" height="43" rx="13.5" stroke="#ffffff" stroke-opacity="0.25" stroke-width="1"/>
                
                <!-- Diamond Smile Hybrid Motif -->
                <!-- Multi-faceted Diamond Top -->
                <path d="M16 14 L32 14 L37 20 L24 20 L11 20 Z" fill="url(#${gid}-facet)"/>
                <polygon points="24,14 19,20 29,20" fill="#ffffff" fill-opacity="0.9"/>
                <polygon points="16,14 11,20 19,20" fill="#ffffff" fill-opacity="0.5"/>
                <polygon points="32,14 29,20 37,20" fill="#ffffff" fill-opacity="0.6"/>

                <!-- Diamond Pavilion tapering into Curvature -->
                <polygon points="11,20 18,27 24,20" fill="#ffffff" fill-opacity="0.7"/>
                <polygon points="37,20 30,27 24,20" fill="#ffffff" fill-opacity="0.75"/>
                <polygon points="18,27 30,27 24,20" fill="#ffffff" fill-opacity="0.95"/>

                <!-- Radiant Smiling Dental Arc Contour (Harmonious smile curve) -->
                <path d="M13 25 C17 35 31 35 35 25 C33 37 15 37 13 25 Z" fill="url(#${gid}-gold)"/>
                <path d="M15 26 C19 33 29 33 33 26" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" vector-effect="non-scaling-stroke"/>

                <!-- Sparkling Diamond Stars -->
                <path d="M37 9 L38 12 L41 13 L38 14 L37 17 L36 14 L33 13 L36 12 Z" fill="#fef08a"/>
                <circle cx="10" cy="12" r="1.5" fill="#fef08a" opacity="0.85"/>
            </svg>`;
        },

        // Full Horizontal Brand Lockup (viewBox: 0 0 240 52)
        full: function (opts = {}) {
            const cls = opts.className || 'h-10 sm:h-12 w-auto';
            const isDark = opts.theme === 'dark';
            const gid = uid('di-logofull');
            const mainTextColor = isDark ? '#ffffff' : '#0b1f33';
            const subTextColor = isDark ? '#94a3b8' : '#64748b';

            return `<svg class="${cls}" viewBox="0 0 240 52" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="DentalCare Luxury Brand Logo">
                <defs>
                    <linearGradient id="${gid}-grad" x1="4" y1="4" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#087a72"/>
                        <stop offset="60%" stop-color="#0d9488"/>
                        <stop offset="100%" stop-color="#0284c7"/>
                    </linearGradient>
                    <linearGradient id="${gid}-gold" x1="18" y1="28" x2="36" y2="40" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#fbbf24"/>
                        <stop offset="50%" stop-color="#f59e0b"/>
                        <stop offset="100%" stop-color="#d97706"/>
                    </linearGradient>
                    <linearGradient id="${gid}-facet" x1="14" y1="12" x2="38" y2="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95"/>
                        <stop offset="100%" stop-color="#ccfbf1" stop-opacity="0.3"/>
                    </linearGradient>
                </defs>
                
                <!-- Emblem Container (x: 2, y: 2, w: 48, h: 48) -->
                <rect x="2" y="2" width="48" height="48" rx="14" fill="url(#${gid}-grad)"/>
                <rect x="2.5" y="2.5" width="47" height="47" rx="13.5" stroke="#ffffff" stroke-opacity="0.25" stroke-width="1"/>
                
                <!-- Diamond Top -->
                <path d="M17 15 L35 15 L40 21 L26 21 L12 21 Z" fill="url(#${gid}-facet)"/>
                <polygon points="26,15 21,21 31,21" fill="#ffffff" fill-opacity="0.9"/>
                <polygon points="17,15 12,21 21,21" fill="#ffffff" fill-opacity="0.5"/>
                <polygon points="35,15 31,21 40,21" fill="#ffffff" fill-opacity="0.6"/>

                <!-- Diamond Pavilion -->
                <polygon points="12,21 20,29 26,21" fill="#ffffff" fill-opacity="0.7"/>
                <polygon points="40,21 32,29 26,21" fill="#ffffff" fill-opacity="0.75"/>
                <polygon points="20,29 32,29 26,21" fill="#ffffff" fill-opacity="0.95"/>

                <!-- Smiling Dental Arc -->
                <path d="M14 27 C18 38 34 38 38 27 C36 40 16 40 14 27 Z" fill="url(#${gid}-gold)"/>
                <path d="M16 28 C20 36 32 36 36 28" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" vector-effect="non-scaling-stroke"/>

                <!-- Starburst Accents -->
                <path d="M40 10 L41 13 L44 14 L41 15 L40 18 L39 15 L36 14 L39 13 Z" fill="#fef08a"/>
                <circle cx="11" cy="13" r="1.5" fill="#fef08a" opacity="0.85"/>

                <!-- Typography Lockup -->
                <!-- Main Wordmark: DentalCare -->
                <text x="60" y="28" font-family="'Plus Jakarta Sans', system-ui, sans-serif" font-weight="900" font-size="22" letter-spacing="-0.5" fill="${mainTextColor}">Dental<tspan fill="#087a72">Care</tspan></text>
                
                <!-- Luxury Pill Badge -->
                <rect x="180" y="14" width="54" height="17" rx="4" fill="#fef3c7" stroke="#fcd34d" stroke-width="0.8"/>
                <text x="207" y="26" font-family="'Plus Jakarta Sans', system-ui, sans-serif" font-weight="800" font-size="9" letter-spacing="1.2" fill="#b45309" text-anchor="middle">LUXURY</text>
                
                <!-- Subtitle / Institute Name -->
                <text x="60" y="43" font-family="'Plus Jakarta Sans', system-ui, sans-serif" font-weight="600" font-size="9.5" letter-spacing="0.8" fill="${subTextColor}">VIỆN NHA KHOA THẨM MỸ QUỐC TẾ</text>
            </svg>`;
        }
    };

    /**
     * 6 UNIQUE DENTAL SERVICE ICONS (viewBox: 0 0 64 64)
     */
    const services = {
        /**
         * 1. ORTHODONTICS (Niềng răng chỉnh nha 3D)
         * Precision dental arch curve with 3D faceted brackets, archwire tension line,
         * subtle alignment guides, emerald/cyan gradient.
         */
        ORTHODONTICS: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-ortho');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Niềng răng chỉnh nha 3D">
                <defs>
                    <linearGradient id="${gid}-grad" x1="8" y1="12" x2="56" y2="52" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#0d9488"/>
                        <stop offset="50%" stop-color="#087a72"/>
                        <stop offset="100%" stop-color="#0369a1"/>
                    </linearGradient>
                    <linearGradient id="${gid}-wire" x1="10" y1="36" x2="54" y2="36" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#38bdf8"/>
                        <stop offset="50%" stop-color="#e0f2fe"/>
                        <stop offset="100%" stop-color="#38bdf8"/>
                    </linearGradient>
                    <linearGradient id="${gid}-bracket" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stop-color="#f8fafc"/>
                        <stop offset="100%" stop-color="#94a3b8"/>
                    </linearGradient>
                    <radialGradient id="${gid}-glow" cx="32" cy="28" r="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#2dd4bf" stop-opacity="0.22"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Subtle Ambient Glow -->
                <circle cx="32" cy="32" r="28" fill="url(#${gid}-glow)"/>
                
                <!-- 3D Digital Alignment Grid Coordinates -->
                <circle cx="32" cy="32" r="26" stroke="#0d9488" stroke-opacity="0.25" stroke-dasharray="2 3" stroke-width="1"/>
                <line x1="32" y1="8" x2="32" y2="14" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="32" y1="50" x2="32" y2="56" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="8" y1="32" x2="14" y2="32" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="50" y1="32" x2="56" y2="32" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>

                <!-- Dental Arch Curvature (Anatomical Upper Teeth Row) -->
                <!-- Teeth Silhouettes forming U-curve -->
                <path d="M12 44 C15 25 24 16 32 16 C40 16 49 25 52 44 C47 41 42 38 32 38 C22 38 17 41 12 44 Z" fill="url(#${gid}-grad)" fill-opacity="0.18"/>
                <path d="M12 44 C15 25 24 16 32 16 C40 16 49 25 52 44" stroke="url(#${gid}-grad)" stroke-width="3" stroke-linecap="round" vector-effect="non-scaling-stroke"/>
                
                <!-- Individual Teeth Segments in Arch -->
                <path d="M14 42 C16 34 20 28 24 24" stroke="#ffffff" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.7"/>
                <path d="M25 23 C28 20 30 19 32 19 C34 19 36 20 39 23" stroke="#ffffff" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.8"/>
                <path d="M40 24 C44 28 48 34 50 42" stroke="#ffffff" stroke-width="1.2" stroke-linecap="round" stroke-opacity="0.7"/>

                <!-- Precision Nitinol Archwire Tension Line with spring curvature -->
                <path d="M11 41 C15 28 23 21 32 21 C41 21 49 28 53 41" stroke="url(#${gid}-wire)" stroke-width="2.5" stroke-linecap="round" filter="drop-shadow(0 2px 4px rgba(2,132,199,0.4))"/>

                <!-- 5 Precision 3D Faceted Brackets with slots -->
                <!-- Bracket 1 (Left Posterior) -->
                <rect x="14" y="33" width="5" height="5" rx="1.2" fill="url(#${gid}-bracket)" stroke="#0f172a" stroke-width="0.8"/>
                <line x1="14" y1="35.5" x2="19" y2="35.5" stroke="#0284c7" stroke-width="0.9"/>
                <!-- Bracket 2 (Left Anterior) -->
                <rect x="22" y="24" width="5.5" height="5.5" rx="1.2" fill="url(#${gid}-bracket)" stroke="#0f172a" stroke-width="0.8"/>
                <line x1="22" y1="26.75" x2="27.5" y2="26.75" stroke="#0284c7" stroke-width="0.9"/>
                <!-- Bracket 3 (Central Incisor Apex) -->
                <rect x="29.25" y="18.5" width="5.5" height="5.5" rx="1.2" fill="url(#${gid}-bracket)" stroke="#0f172a" stroke-width="0.8"/>
                <line x1="29.25" y1="21.25" x2="34.75" y2="21.25" stroke="#0284c7" stroke-width="0.9"/>
                <circle cx="32" cy="21.25" r="0.9" fill="#f59e0b"/>
                <!-- Bracket 4 (Right Anterior) -->
                <rect x="36.5" y="24" width="5.5" height="5.5" rx="1.2" fill="url(#${gid}-bracket)" stroke="#0f172a" stroke-width="0.8"/>
                <line x1="36.5" y1="26.75" x2="42" y2="26.75" stroke="#0284c7" stroke-width="0.9"/>
                <!-- Bracket 5 (Right Posterior) -->
                <rect x="45" y="33" width="5" height="5" rx="1.2" fill="url(#${gid}-bracket)" stroke="#0f172a" stroke-width="0.8"/>
                <line x1="45" y1="35.5" x2="50" y2="35.5" stroke="#0284c7" stroke-width="0.9"/>

                <!-- 3D Vector Depth Guides -->
                <path d="M32 24 L32 34" stroke="#087a72" stroke-width="1.2" stroke-dasharray="1.5 1.5"/>
                <path d="M29 33 L32 36 L35 33" stroke="#087a72" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>

                <!-- Sparkle Accent on Perfect Alignment -->
                <path d="M46 16 L47 18.5 L49.5 19.5 L47 20.5 L46 23 L45 20.5 L42.5 19.5 L45 18.5 Z" fill="#38bdf8"/>
            </svg>`;
        },

        /**
         * 2. IMPLANT (Cấy ghép Implant Thụy Sĩ)
         * Anatomical tooth crown anchored to titanium screw with spiral threads,
         * abutment collar, and Swiss cross accent in gold/teal.
         */
        IMPLANT: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-implant');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Cấy ghép Implant Thụy Sĩ">
                <defs>
                    <linearGradient id="${gid}-crown" x1="20" y1="6" x2="44" y2="24" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="70%" stop-color="#e0f2fe"/>
                        <stop offset="100%" stop-color="#bae6fd"/>
                    </linearGradient>
                    <linearGradient id="${gid}-collar" x1="22" y1="24" x2="42" y2="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#f59e0b"/>
                        <stop offset="50%" stop-color="#fbbf24"/>
                        <stop offset="100%" stop-color="#d97706"/>
                    </linearGradient>
                    <linearGradient id="${gid}-screw" x1="24" y1="28" x2="40" y2="58" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#0d9488"/>
                        <stop offset="40%" stop-color="#087a72"/>
                        <stop offset="80%" stop-color="#0f172a"/>
                        <stop offset="100%" stop-color="#14b8a6"/>
                    </linearGradient>
                    <radialGradient id="${gid}-halo" cx="32" cy="54" r="16" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#2dd4bf" stop-opacity="0.35"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Osseointegration Apex Energy Halo -->
                <circle cx="32" cy="52" r="11" fill="url(#${gid}-halo)"/>
                
                <!-- Anatomical Ceramic Tooth Crown -->
                <path d="M22 8 C24 6 28 6 32 6 C36 6 40 6 42 8 C46 11 46 18 44 23 C41 24.5 37 25 32 25 C27 25 23 24.5 20 23 C18 18 18 11 22 8 Z" fill="url(#${gid}-crown)" stroke="#0284c7" stroke-width="1.5" stroke-linejoin="round"/>
                <!-- Crown Glaze Cusp Highlights -->
                <path d="M24 10 C26 9 29 9 30 11" stroke="#ffffff" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M34 11 C35 9 38 9 40 10" stroke="#ffffff" stroke-width="1.5" stroke-linecap="round"/>

                <!-- Swiss Medical Cross Hallmark on Crown -->
                <rect x="29" y="13" width="6" height="6" rx="1" fill="#dc2626"/>
                <rect x="31" y="14" width="2" height="4" fill="#ffffff"/>
                <rect x="30" y="15" width="4" height="2" fill="#ffffff"/>

                <!-- Precision Titanium Abutment Platform Collar -->
                <path d="M25 24 L39 24 L37 28 L27 28 Z" fill="url(#${gid}-collar)" stroke="#b45309" stroke-width="0.8"/>
                <line x1="26" y1="26" x2="38" y2="26" stroke="#fef08a" stroke-width="0.8"/>

                <!-- High-Precision Titanium Screw Fixture (Tapered helical thread steps) -->
                <!-- Fixture Core Body -->
                <path d="M27 28 L37 28 L35.5 54 L32 58 L28.5 54 Z" fill="url(#${gid}-screw)"/>

                <!-- Helical Micro-Thread Ridges (Spiral precision milling) -->
                <path d="M26 31 L38 33" stroke="#2dd4bf" stroke-width="1.8" stroke-linecap="round"/>
                <path d="M26 36 L38 38" stroke="#2dd4bf" stroke-width="1.8" stroke-linecap="round"/>
                <path d="M26.5 41 L37.5 43" stroke="#2dd4bf" stroke-width="1.8" stroke-linecap="round"/>
                <path d="M27 46 L37 48" stroke="#2dd4bf" stroke-width="1.8" stroke-linecap="round"/>
                <path d="M28 51 L36 53" stroke="#2dd4bf" stroke-width="1.8" stroke-linecap="round"/>

                <!-- Lateral Bone Integration Radiance Dots -->
                <circle cx="19" cy="38" r="1.5" fill="#14b8a6"/>
                <circle cx="16" cy="46" r="1.2" fill="#14b8a6" opacity="0.7"/>
                <circle cx="45" cy="38" r="1.5" fill="#14b8a6"/>
                <circle cx="48" cy="46" r="1.2" fill="#14b8a6" opacity="0.7"/>

                <!-- Apex Precision Anchor Dot -->
                <circle cx="32" cy="58" r="1.5" fill="#fbbf24"/>
            </svg>`;
        },

        /**
         * 3. VENEER (Dán sứ Veneer thẩm mỹ / Porcelain Crowns)
         * Ultra-thin porcelain veneer ceramic facing adhering to incandescent incisor tooth,
         * multi-faceted diamond polish reflections, sparkling accents.
         */
        VENEER: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-veneer');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Dán sứ Veneer thẩm mỹ">
                <defs>
                    <linearGradient id="${gid}-tooth" x1="16" y1="12" x2="48" y2="54" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#f8fafc"/>
                        <stop offset="60%" stop-color="#cbd5e1"/>
                        <stop offset="100%" stop-color="#94a3b8"/>
                    </linearGradient>
                    <linearGradient id="${gid}-facing" x1="20" y1="8" x2="52" y2="52" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="40%" stop-color="#a5f3fc"/>
                        <stop offset="80%" stop-color="#2dd4bf"/>
                        <stop offset="100%" stop-color="#087a72"/>
                    </linearGradient>
                    <linearGradient id="${gid}-facet" x1="24" y1="14" x2="44" y2="38" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.9"/>
                        <stop offset="100%" stop-color="#ffffff" stop-opacity="0.1"/>
                    </linearGradient>
                    <filter id="${gid}-glow" x="-10%" y="-10%" width="120%" height="120%" filterUnits="userSpaceOnUse">
                        <feDropShadow dx="0" dy="4" stdDeviation="4" flood-color="#0d9488" flood-opacity="0.25"/>
                    </filter>
                </defs>
                <!-- Natural Base Incisor Tooth (Underlying tooth structure) -->
                <path d="M19 15 C23 10 39 10 43 15 C45 28 41 46 32 54 C23 46 19 28 19 15 Z" fill="url(#${gid}-tooth)" stroke="#64748b" stroke-width="1.2" stroke-dasharray="3 2" opacity="0.6"/>

                <!-- Ultra-thin Veneer Ceramic Facing Shell (Lifted / adhering 0.2mm profile) -->
                <path d="M22 10 C26 7 42 7 46 10 C48 26 44 46 34 56 C24 48 21 28 22 10 Z" fill="url(#${gid}-facing)" filter="url(#${gid}-glow)"/>
                <path d="M22 10 C26 7 42 7 46 10 C48 26 44 46 34 56 C24 48 21 28 22 10 Z" stroke="#ffffff" stroke-width="1.8"/>

                <!-- Multi-Faceted Ceramic Glaze Reflections (Diamond Polish) -->
                <!-- Upper Facet Reflection -->
                <polygon points="26,12 42,12 37,22 28,22" fill="url(#${gid}-facet)"/>
                <!-- Central Diamond Sheen Prism -->
                <polygon points="28,22 37,22 34,36 30,36" fill="#ffffff" fill-opacity="0.65"/>
                <!-- Incisal Edge Translucency Line -->
                <path d="M25 15 C30 17 38 17 43 15" stroke="#ffffff" stroke-width="1.5" stroke-linecap="round"/>
                <!-- Vertical Light Reflection Band -->
                <path d="M30 18 L28 44 C30 48 33 50 34 52 L36 36 Z" fill="#ffffff" fill-opacity="0.35"/>

                <!-- Sparkling Diamond Stars (Veneer brilliance) -->
                <!-- Star 1 (Top Right) -->
                <path d="M50 12 L51.5 16 L55.5 17.5 L51.5 19 L50 23 L48.5 19 L44.5 17.5 L48.5 16 Z" fill="#fbbf24"/>
                <circle cx="50" cy="17.5" r="1" fill="#ffffff"/>
                <!-- Star 2 (Bottom Left) -->
                <path d="M14 36 L15 39 L18 40 L15 41 L14 44 L13 41 L10 40 L13 39 Z" fill="#38bdf8"/>
                
                <!-- Micro Polish Highlight -->
                <circle cx="43" cy="28" r="1.5" fill="#ffffff"/>
            </svg>`;
        },

        /**
         * 4. PIEZOTOME (Nhổ răng khôn sóng siêu âm Piezotome / Wisdom Teeth)
         * Third molar tooth encapsulated by concentric harmonic ultrasonic wave rings,
         * piezoelectric vibration lines, soft-tissue gentle extraction.
         */
        PIEZOTOME: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-piezo');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Nhổ răng khôn sóng siêu âm Piezotome">
                <defs>
                    <linearGradient id="${gid}-molar" x1="18" y1="14" x2="46" y2="52" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="50%" stop-color="#f0fdfa"/>
                        <stop offset="100%" stop-color="#ccfbf1"/>
                    </linearGradient>
                    <linearGradient id="${gid}-wave" x1="6" y1="6" x2="58" y2="58" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#0d9488"/>
                        <stop offset="50%" stop-color="#14b8a6"/>
                        <stop offset="100%" stop-color="#0284c7"/>
                    </linearGradient>
                    <radialGradient id="${gid}-pulse" cx="32" cy="32" r="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#14b8a6" stop-opacity="0.2"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Ultrasonic Acoustic Pressure Field -->
                <circle cx="32" cy="32" r="27" fill="url(#${gid}-pulse)"/>

                <!-- Concentric Harmonic Ultrasonic Wave Rings (28-36 kHz frequency resonance) -->
                <circle cx="32" cy="32" r="26" stroke="url(#${gid}-wave)" stroke-width="1.2" stroke-dasharray="4 3" opacity="0.4"/>
                <circle cx="32" cy="32" r="21" stroke="url(#${gid}-wave)" stroke-width="1.5" stroke-dasharray="6 4" opacity="0.7"/>
                <circle cx="32" cy="32" r="16" stroke="url(#${gid}-wave)" stroke-width="1.8" opacity="0.9"/>

                <!-- Piezoelectric Tip High-Frequency Energy Vectors -->
                <!-- Left Ultrasonic Wave Arcs -->
                <path d="M12 24 C8 29 8 35 12 40" stroke="#0d9488" stroke-width="2" stroke-linecap="round"/>
                <path d="M7 20 C2 27 2 37 7 44" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round" opacity="0.6"/>
                <!-- Right Ultrasonic Wave Arcs -->
                <path d="M52 24 C56 29 56 35 52 40" stroke="#0d9488" stroke-width="2" stroke-linecap="round"/>
                <path d="M57 20 C62 27 62 37 57 44" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round" opacity="0.6"/>

                <!-- Anatomical Third Molar Tooth (Cusps + Twin Divergent Roots) -->
                <!-- Outer Tooth Silhouette -->
                <path d="M22 17 C25 15 28 17 32 17 C36 17 39 15 42 17 C46 19 46 25 45 30 C44 35 41 40 40 50 C38 52 35 51 34 46 C33 42 32 37 32 35 C32 37 31 42 30 46 C29 51 26 52 24 50 C23 40 20 35 19 30 C18 25 18 19 22 17 Z" fill="url(#${gid}-molar)" stroke="#087a72" stroke-width="2" stroke-linejoin="round"/>
                
                <!-- Molar Occlusal Groove Anatomy (Fissures) -->
                <path d="M26 21 C30 24 34 24 38 21" stroke="#0d9488" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M32 21 L32 29" stroke="#0d9488" stroke-width="1.5" stroke-linecap="round"/>

                <!-- Piezo Micro-Vibration Beam Tip at Apex -->
                <path d="M32 8 L32 14" stroke="#f59e0b" stroke-width="2" stroke-linecap="round"/>
                <path d="M30 11 L34 11" stroke="#f59e0b" stroke-width="1.5" stroke-linecap="round"/>
                
                <!-- Zero-Pain Sparkle Accent -->
                <path d="M44 11 L45 13 L47 14 L45 15 L44 17 L43 15 L41 14 L43 13 Z" fill="#fbbf24"/>
            </svg>`;
        },

        /**
         * 5. ENDODONTICS (Điều trị tủy / Root Canal Therapy)
         * Tooth anatomy showing pulp chamber, root canals, and precision endodontic NiTi file motif.
         */
        ENDODONTICS: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-endo');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Điều trị tủy răng công nghệ vi phẫu">
                <defs>
                    <linearGradient id="${gid}-tooth" x1="18" y1="10" x2="46" y2="54" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="60%" stop-color="#f1f5f9"/>
                        <stop offset="100%" stop-color="#e2e8f0"/>
                    </linearGradient>
                    <linearGradient id="${gid}-pulp" x1="26" y1="20" x2="38" y2="48" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#f43f5e"/>
                        <stop offset="50%" stop-color="#e11d48"/>
                        <stop offset="100%" stop-color="#0d9488"/>
                    </linearGradient>
                    <linearGradient id="${gid}-file" x1="32" y1="4" x2="38" y2="46" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#fbbf24"/>
                        <stop offset="40%" stop-color="#f59e0b"/>
                        <stop offset="100%" stop-color="#38bdf8"/>
                    </linearGradient>
                    <radialGradient id="${gid}-apex" cx="36" cy="51" r="10" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#2dd4bf" stop-opacity="0.4"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Apical Healing Luminescence Halo -->
                <circle cx="36" cy="51" r="8" fill="url(#${gid}-apex)"/>
                
                <!-- Cross-Sectional Tooth Contour (Enamel + Dentin Structure) -->
                <path d="M20 14 C23 11 27 12 32 12 C37 12 41 11 44 14 C47 18 47 24 46 29 C45 35 43 40 40 52 C38 54 36 53 35 48 C34 44 33 39 32 37 C31 39 30 44 29 48 C28 53 26 54 24 52 C21 40 19 35 18 29 C17 24 17 18 20 14 Z" fill="url(#${gid}-tooth)" stroke="#087a72" stroke-width="2" stroke-linejoin="round"/>

                <!-- Coronal Pulp Chamber & Root Canals Internal Cavity -->
                <path d="M26 21 C29 19 35 19 38 21 C39 24 38 27 37 31 C36 36 37 42 36 50 C35 50 35 45 34 40 C33 36 32 32 32 30 C32 32 31 36 30 40 C29 45 29 50 28 50 C27 42 28 36 27 31 C26 27 25 24 26 21 Z" fill="url(#${gid}-pulp)" fill-opacity="0.85"/>

                <!-- Precision Endodontic NiTi Rotary File Instrument Motif -->
                <!-- File Handle at Top -->
                <rect x="36" y="4" width="6" height="8" rx="1.5" fill="#f59e0b" stroke="#78350f" stroke-width="0.8"/>
                <line x1="37.5" y1="7" x2="40.5" y2="7" stroke="#ffffff" stroke-width="0.8"/>
                <!-- Tapered Spiral File Shaft entering the canal -->
                <path d="M39 12 L38 26 L36.5 48" stroke="url(#${gid}-file)" stroke-width="1.8" stroke-linecap="round"/>
                <!-- Calibration Depth Markings / Spiral Flutes on NiTi File -->
                <line x1="37" y1="18" x2="41" y2="19" stroke="#0284c7" stroke-width="1.2"/>
                <line x1="36.5" y1="24" x2="40" y2="25" stroke="#0284c7" stroke-width="1.2"/>
                <line x1="36" y1="30" x2="39" y2="31" stroke="#0284c7" stroke-width="1.2"/>
                <line x1="35.5" y1="36" x2="38" y2="37" stroke="#0284c7" stroke-width="1.2"/>
                <line x1="35" y1="42" x2="37" y2="43" stroke="#0284c7" stroke-width="1.2"/>

                <!-- Apical Healing Rays (Preserved natural tooth root) -->
                <path d="M36 54 L36 59" stroke="#14b8a6" stroke-width="1.8" stroke-linecap="round"/>
                <path d="M33 53 L31 57" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M39 53 L41 57" stroke="#14b8a6" stroke-width="1.5" stroke-linecap="round"/>

                <!-- Micro Starlet -->
                <path d="M15 18 L16 20 L18 21 L16 22 L15 24 L14 22 L12 21 L14 20 Z" fill="#2dd4bf"/>
            </svg>`;
        },

        /**
         * 6. WHITENING (Tẩy trắng răng Laser Whitening)
         * Radiant tooth enamel with focused laser beam wavelength rays and brilliant luminescence.
         */
        WHITENING: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-white');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Tẩy trắng răng Laser Whitening">
                <defs>
                    <linearGradient id="${gid}-tooth" x1="18" y1="12" x2="46" y2="52" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="45%" stop-color="#f0fdfa"/>
                        <stop offset="85%" stop-color="#ccfbf1"/>
                        <stop offset="100%" stop-color="#a5f3fc"/>
                    </linearGradient>
                    <linearGradient id="${gid}-laser" x1="4" y1="6" x2="34" y2="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#38bdf8"/>
                        <stop offset="40%" stop-color="#818cf8"/>
                        <stop offset="80%" stop-color="#c084fc"/>
                        <stop offset="100%" stop-color="#ffffff"/>
                    </linearGradient>
                    <radialGradient id="${gid}-burst" cx="30" cy="24" r="22" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff" stop-opacity="0.95"/>
                        <stop offset="35%" stop-color="#38bdf8" stop-opacity="0.4"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Brilliant Luminescence Starburst Halo -->
                <circle cx="30" cy="24" r="20" fill="url(#${gid}-burst)"/>

                <!-- Anatomical Central Incisor Tooth Contour -->
                <path d="M21 13 C25 10 39 10 43 13 C46 22 45 36 38 48 C35 53 29 53 26 48 C19 36 18 22 21 13 Z" fill="url(#${gid}-tooth)" stroke="#0284c7" stroke-width="2" stroke-linejoin="round"/>

                <!-- Tooth Enamel Brilliant Tone Transition (Shade A4 -> B1 Hollywood White) -->
                <path d="M23 15 C26 12 38 12 41 15 C43 23 42 34 37 44 C34 48 30 48 27 44 C22 34 21 23 23 15 Z" fill="#ffffff" fill-opacity="0.75"/>
                
                <!-- Enamel Luster Specular Highlights -->
                <path d="M26 16 C29 14 35 14 38 16" stroke="#ffffff" stroke-width="2" stroke-linecap="round"/>
                <path d="M25 22 L24 38" stroke="#ffffff" stroke-width="1.8" stroke-linecap="round"/>

                <!-- Focused Medical Laser Beam (Wavelength ray targeting enamel chromophores) -->
                <!-- Laser Emitter Source Cone -->
                <polygon points="6,6 18,12 14,16 4,8" fill="#6366f1"/>
                <polygon points="18,12 32,24 28,26 14,16" fill="url(#${gid}-laser)" opacity="0.85"/>
                <!-- High-Energy Coherent Core Beam -->
                <line x1="8" y1="8" x2="30" y2="25" stroke="#ffffff" stroke-width="2" stroke-linecap="round" filter="drop-shadow(0 0 4px #818cf8)"/>

                <!-- Multi-point Crystalline Luminescence Stars (Hollywood Smile Sparkles) -->
                <!-- Star 1 (Impact Apex Star) -->
                <path d="M30 18 L32 23 L37 25 L32 27 L30 32 L28 27 L23 25 L28 23 Z" fill="#fbbf24"/>
                <circle cx="30" cy="25" r="1.5" fill="#ffffff"/>
                <!-- Star 2 (Upper Right Brilliance) -->
                <path d="M47 8 L48.5 12 L52.5 13.5 L48.5 15 L47 19 L45.5 15 L41.5 13.5 L45.5 12 Z" fill="#38bdf8"/>
                <circle cx="47" cy="13.5" r="1" fill="#ffffff"/>
                <!-- Star 3 (Lower Left Sparkle) -->
                <path d="M14 42 L15 44.5 L17.5 45.5 L15 46.5 L14 49 L13 46.5 L10.5 45.5 L13 44.5 Z" fill="#38bdf8"/>
                <!-- Star 4 (Lower Right Accent) -->
                <circle cx="46" cy="38" r="1.8" fill="#fef08a"/>
                <circle cx="40" cy="50" r="1.2" fill="#38bdf8"/>
            </svg>`;
        },

        /**
         * GENERAL (Nha khoa tổng quát & Chăm sóc toàn diện)
         */
        GENERAL: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-gen');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Nha khoa tổng quát">
                <defs>
                    <linearGradient id="${gid}-grad" x1="12" y1="8" x2="52" y2="56" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#087a72"/>
                        <stop offset="50%" stop-color="#0d9488"/>
                        <stop offset="100%" stop-color="#0284c7"/>
                    </linearGradient>
                </defs>
                <path d="M32 6 L14 14 V28 C14 42 22 52 32 58 C42 52 50 42 50 28 V14 L32 6 Z" fill="url(#${gid}-grad)" fill-opacity="0.15" stroke="url(#${gid}-grad)" stroke-width="2.5" stroke-linejoin="round"/>
                <!-- Inner Tooth Icon with Medical Cross -->
                <path d="M26 22 C28 20 30 21 32 21 C34 21 36 20 38 22 C40 24 40 28 39 31 C38 35 37 38 36 44 C35 45 34 45 33 42 C32 39 32 36 32 35 C32 36 32 39 31 42 C30 45 29 45 28 44 C27 38 26 35 25 31 C24 28 24 24 26 22 Z" fill="#ffffff" stroke="#087a72" stroke-width="1.8"/>
                <path d="M32 26 V34 M28 30 H36" stroke="#0d9488" stroke-width="2" stroke-linecap="round"/>
            </svg>`;
        }
    };

    // Alias mapping for compatibility with backend service category codes
    services.PORCELAIN_CROWNS = services.VENEER;
    services.WISDOM_TEETH = services.PIEZOTOME;

    /**
     * 4 UNIQUE FEATURE BADGES (viewBox: 0 0 64 64)
     */
    const badges = {
        /**
         * 1. AI_DOCTOR (Bác sĩ AI Chẩn đoán)
         * AI Neural chip / vision processor badge with cybernetic neural circuit traces
         * and dental scanning grid.
         */
        AI_DOCTOR: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-aidoctor');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Bác sĩ AI Chẩn đoán">
                <defs>
                    <linearGradient id="${gid}-chip" x1="10" y1="10" x2="54" y2="54" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#7c3aed"/>
                        <stop offset="50%" stop-color="#6366f1"/>
                        <stop offset="100%" stop-color="#087a72"/>
                    </linearGradient>
                    <linearGradient id="${gid}-grid" x1="20" y1="20" x2="44" y2="44" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#38bdf8"/>
                        <stop offset="100%" stop-color="#2dd4bf"/>
                    </linearGradient>
                    <radialGradient id="${gid}-pulse" cx="32" cy="32" r="24" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#a855f7" stop-opacity="0.35"/>
                        <stop offset="100%" stop-color="#6366f1" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Neural Network Pulse Field -->
                <circle cx="32" cy="32" r="26" fill="url(#${gid}-pulse)"/>

                <!-- Cybernetic Chip Bus Circuit Traces & Terminal Nodes -->
                <!-- Top traces -->
                <line x1="26" y1="6" x2="26" y2="14" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="26" cy="6" r="1.5" fill="#38bdf8"/>
                <line x1="32" y1="4" x2="32" y2="14" stroke="#a855f7" stroke-width="2" stroke-linecap="round"/>
                <circle cx="32" cy="4" r="1.8" fill="#c084fc"/>
                <line x1="38" y1="6" x2="38" y2="14" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="38" cy="6" r="1.5" fill="#38bdf8"/>

                <!-- Bottom traces -->
                <line x1="26" y1="50" x2="26" y2="58" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="26" cy="58" r="1.5" fill="#38bdf8"/>
                <line x1="32" y1="50" x2="32" y2="60" stroke="#a855f7" stroke-width="2" stroke-linecap="round"/>
                <circle cx="32" cy="60" r="1.8" fill="#c084fc"/>
                <line x1="38" y1="50" x2="38" y2="58" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="38" cy="58" r="1.5" fill="#38bdf8"/>

                <!-- Left traces -->
                <line x1="6" y1="26" x2="14" y2="26" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="6" cy="26" r="1.5" fill="#38bdf8"/>
                <line x1="4" y1="32" x2="14" y2="32" stroke="#2dd4bf" stroke-width="2" stroke-linecap="round"/>
                <circle cx="4" cy="32" r="1.8" fill="#2dd4bf"/>
                <line x1="6" y1="38" x2="14" y2="38" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="6" cy="38" r="1.5" fill="#38bdf8"/>

                <!-- Right traces -->
                <line x1="50" y1="26" x2="58" y2="26" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="58" cy="26" r="1.5" fill="#38bdf8"/>
                <line x1="50" y1="32" x2="60" y2="32" stroke="#2dd4bf" stroke-width="2" stroke-linecap="round"/>
                <circle cx="60" cy="32" r="1.8" fill="#2dd4bf"/>
                <line x1="50" y1="38" x2="58" y2="38" stroke="#818cf8" stroke-width="1.8" stroke-linecap="round"/>
                <circle cx="58" cy="38" r="1.5" fill="#38bdf8"/>

                <!-- Hexagonal Neural Carrier Microchip -->
                <polygon points="22,14 42,14 50,22 50,42 42,50 22,50 14,42 14,22" fill="url(#${gid}-chip)" stroke="#ffffff" stroke-width="1.5"/>
                
                <!-- Inner Diagnostic Scanning Sensor Iris / Reticle -->
                <rect x="20" y="20" width="24" height="24" rx="6" fill="#0f172a" stroke="url(#${gid}-grid)" stroke-width="1.2"/>

                <!-- Dental Digital Scanning Grid Mesh (Holographic Tooth Silhouette) -->
                <path d="M26 27 C28 25 30 26 32 26 C34 26 36 25 38 27 C39 29 39 32 38 34 C37 37 36 39 35 42 C34 43 33 42 32 40 C31 42 30 43 29 42 C28 39 27 37 26 34 C25 32 25 29 26 27 Z" fill="none" stroke="#38bdf8" stroke-width="1.2" stroke-dasharray="2 1.5"/>
                
                <!-- Center Target Reticle Crosshair -->
                <circle cx="32" cy="32" r="3" stroke="#2dd4bf" stroke-width="1.2" fill="none"/>
                <circle cx="32" cy="32" r="1" fill="#fef08a"/>
                <line x1="32" y1="21" x2="32" y2="24" stroke="#38bdf8" stroke-width="1.2"/>
                <line x1="32" y1="40" x2="32" y2="43" stroke="#38bdf8" stroke-width="1.2"/>
                <line x1="21" y1="32" x2="24" y2="32" stroke="#38bdf8" stroke-width="1.2"/>
                <line x1="40" y1="32" x2="43" y2="32" stroke="#38bdf8" stroke-width="1.2"/>
            </svg>`;
        },

        /**
         * 2. WARRANTY (Tra cứu Thẻ bảo hành Răng sứ)
         * Smart QR Shield badge combining medical protective heraldic crest with
         * authentic micro-matrix QR elements.
         */
        WARRANTY: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-warranty');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Tra cứu Thẻ bảo hành">
                <defs>
                    <linearGradient id="${gid}-shield" x1="12" y1="6" x2="52" y2="58" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#0284c7"/>
                        <stop offset="50%" stop-color="#0369a1"/>
                        <stop offset="100%" stop-color="#0b1f33"/>
                    </linearGradient>
                    <linearGradient id="${gid}-gold" x1="20" y1="10" x2="44" y2="40" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#fbbf24"/>
                        <stop offset="50%" stop-color="#f59e0b"/>
                        <stop offset="100%" stop-color="#d97706"/>
                    </linearGradient>
                    <radialGradient id="${gid}-glow" cx="32" cy="30" r="26" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#38bdf8" stop-opacity="0.3"/>
                        <stop offset="100%" stop-color="#0284c7" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Shield Ambient Glow -->
                <circle cx="32" cy="32" r="26" fill="url(#${gid}-glow)"/>

                <!-- European Luxury Coat-of-Arms Heraldic Medical Crest -->
                <path d="M32 6 C40 10 49 10 52 14 C52 30 46 47 32 58 C18 47 12 30 12 14 C15 10 24 10 32 6 Z" fill="url(#${gid}-shield)" stroke="url(#${gid}-gold)" stroke-width="2.5" stroke-linejoin="round"/>
                
                <!-- Inner Protective Beveled Border -->
                <path d="M32 10 C38 13 45 13 48 16 C48 29 43 43 32 52 C21 43 16 29 16 16 C19 13 26 13 32 10 Z" fill="#0f172a" fill-opacity="0.75" stroke="#38bdf8" stroke-width="1"/>

                <!-- Authentic Micro-Matrix QR Code Grid Pattern -->
                <!-- Top-Left QR Position Detection Pattern -->
                <rect x="22" y="20" width="7" height="7" rx="1" fill="#ffffff"/>
                <rect x="23.5" y="21.5" width="4" height="4" fill="#0f172a"/>
                <rect x="24.5" y="22.5" width="2" height="2" fill="#38bdf8"/>

                <!-- Top-Right QR Position Detection Pattern -->
                <rect x="35" y="20" width="7" height="7" rx="1" fill="#ffffff"/>
                <rect x="36.5" y="21.5" width="4" height="4" fill="#0f172a"/>
                <rect x="37.5" y="22.5" width="2" height="2" fill="#38bdf8"/>

                <!-- Bottom-Left QR Position Detection Pattern -->
                <rect x="22" y="33" width="7" height="7" rx="1" fill="#ffffff"/>
                <rect x="23.5" y="34.5" width="4" height="4" fill="#0f172a"/>
                <rect x="24.5" y="35.5" width="2" height="2" fill="#38bdf8"/>

                <!-- Central Authenticity Checkmark / Micro QR Data Cells -->
                <rect x="31" y="22" width="2" height="2" fill="#38bdf8"/>
                <rect x="31" y="26" width="2" height="2" fill="#ffffff"/>
                <rect x="26" y="29" width="2" height="2" fill="#38bdf8"/>
                <rect x="35" y="29" width="3" height="2" fill="#ffffff"/>
                <rect x="31" y="33" width="2" height="3" fill="#ffffff"/>
                <rect x="35" y="34" width="2" height="2" fill="#38bdf8"/>
                <rect x="39" y="34" width="3" height="2" fill="#ffffff"/>
                <rect x="35" y="38" width="5" height="2" fill="#38bdf8"/>

                <!-- Gold Verified Seal & Crest Checkmark -->
                <circle cx="32" cy="14" r="4.5" fill="url(#${gid}-gold)" stroke="#ffffff" stroke-width="0.8"/>
                <path d="M30 14 L31.5 15.5 L34.5 12.5" stroke="#0f172a" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>`;
        },

        /**
         * 3. LOYALTY (Tích điểm Loyalty & Đẳng cấp Thành viên)
         * Golden Crown royal badge with gemstone points and sparkling dental gold sheen.
         */
        LOYALTY: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-loyalty');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Tích điểm Loyalty">
                <defs>
                    <linearGradient id="${gid}-gold" x1="12" y1="12" x2="52" y2="52" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#fef08a"/>
                        <stop offset="25%" stop-color="#fbbf24"/>
                        <stop offset="60%" stop-color="#f59e0b"/>
                        <stop offset="100%" stop-color="#b45309"/>
                    </linearGradient>
                    <linearGradient id="${gid}-gem" x1="0" y1="0" x2="1" y2="1">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="100%" stop-color="#2dd4bf"/>
                    </linearGradient>
                    <radialGradient id="${gid}-glow" cx="32" cy="30" r="26" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#f59e0b" stop-opacity="0.3"/>
                        <stop offset="100%" stop-color="#b45309" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Golden Aura -->
                <circle cx="32" cy="32" r="26" fill="url(#${gid}-glow)"/>

                <!-- Laurel Victory Reward Wreath Arc -->
                <path d="M12 40 C10 32 13 22 20 16" stroke="#fbbf24" stroke-width="2" stroke-linecap="round" opacity="0.75"/>
                <path d="M52 40 C54 32 51 22 44 16" stroke="#fbbf24" stroke-width="2" stroke-linecap="round" opacity="0.75"/>
                <!-- Laurel Leaf Pairs -->
                <ellipse cx="14" cy="27" rx="3" ry="1.5" transform="rotate(-30 14 27)" fill="#fbbf24"/>
                <ellipse cx="15" cy="34" rx="3" ry="1.5" transform="rotate(-15 15 34)" fill="#fbbf24"/>
                <ellipse cx="50" cy="27" rx="3" ry="1.5" transform="rotate(30 50 27)" fill="#fbbf24"/>
                <ellipse cx="49" cy="34" rx="3" ry="1.5" transform="rotate(15 49 34)" fill="#fbbf24"/>

                <!-- Royal 5-Point Crown Body with 3D Sculpted Luster -->
                <path d="M14 44 L16 23 L25 31 L32 16 L39 31 L48 23 L50 44 Z" fill="url(#${gid}-gold)" stroke="#78350f" stroke-width="1.5" stroke-linejoin="round"/>

                <!-- Crown Bevel Ridges (Chiseled Gold Facets) -->
                <polygon points="32,16 25,31 32,36" fill="#fef08a" fill-opacity="0.8"/>
                <polygon points="32,16 39,31 32,36" fill="#d97706" fill-opacity="0.6"/>
                <polygon points="16,23 25,31 19,37" fill="#fef08a" fill-opacity="0.6"/>
                <polygon points="48,23 39,31 45,37" fill="#b45309" fill-opacity="0.7"/>

                <!-- Crown Base Circlet Band -->
                <rect x="13" y="44" width="38" height="7" rx="2" fill="url(#${gid}-gold)" stroke="#78350f" stroke-width="1.2"/>
                
                <!-- Circlet Inlaid Emeralds & Diamonds -->
                <circle cx="19" cy="47.5" r="1.8" fill="url(#${gid}-gem)"/>
                <circle cx="26" cy="47.5" r="1.8" fill="#ffffff"/>
                <circle cx="32" cy="47.5" r="2.2" fill="#087a72"/>
                <circle cx="38" cy="47.5" r="1.8" fill="#ffffff"/>
                <circle cx="45" cy="47.5" r="1.8" fill="url(#${gid}-gem)"/>

                <!-- 5 Solitaire Gemstone Finials on Crown Peaks -->
                <circle cx="16" cy="23" r="2.5" fill="url(#${gid}-gem)" stroke="#78350f" stroke-width="0.8"/>
                <circle cx="25" cy="31" r="2.2" fill="#ffffff" stroke="#78350f" stroke-width="0.8"/>
                <circle cx="32" cy="15" r="3.2" fill="url(#${gid}-gem)" stroke="#78350f" stroke-width="0.8"/>
                <circle cx="39" cy="31" r="2.2" fill="#ffffff" stroke="#78350f" stroke-width="0.8"/>
                <circle cx="48" cy="23" r="2.5" fill="url(#${gid}-gem)" stroke="#78350f" stroke-width="0.8"/>

                <!-- Sparkling Crown Diamond Star -->
                <path d="M32 7 L33 10 L36 11 L33 12 L32 15 L31 12 L28 11 L31 10 Z" fill="#ffffff"/>
            </svg>`;
        },

        /**
         * 4. BRANCH_MAP (Hệ thống Chi nhánh Bản đồ Toàn quốc)
         * Location GPS Pin badge with radar concentric rings and clinic medical cross
         * in emerald/navy (eliminating harsh red Google pins!).
         */
        BRANCH_MAP: function (opts = {}) {
            const cls = opts.className || 'w-full h-full';
            const gid = uid('di-map');
            return `<svg class="${cls} dental-icon-svg" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid meet" aria-label="Hệ thống Chi nhánh Bản đồ">
                <defs>
                    <linearGradient id="${gid}-pin" x1="18" y1="8" x2="46" y2="48" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#0d9488"/>
                        <stop offset="60%" stop-color="#087a72"/>
                        <stop offset="100%" stop-color="#0b4241"/>
                    </linearGradient>
                    <linearGradient id="${gid}-cross" x1="26" y1="16" x2="38" y2="28" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#ffffff"/>
                        <stop offset="100%" stop-color="#ccfbf1"/>
                    </linearGradient>
                    <radialGradient id="${gid}-radar" cx="32" cy="52" r="20" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#14b8a6" stop-opacity="0.5"/>
                        <stop offset="50%" stop-color="#087a72" stop-opacity="0.2"/>
                        <stop offset="100%" stop-color="#087a72" stop-opacity="0"/>
                    </radialGradient>
                </defs>
                <!-- Concentric Sonar Radar Wave Rings beneath Pin Tip -->
                <ellipse cx="32" cy="52" rx="20" ry="7" fill="url(#${gid}-radar)"/>
                <ellipse cx="32" cy="52" rx="18" ry="6" stroke="#0d9488" stroke-width="1.2" stroke-dasharray="3 3" opacity="0.6"/>
                <ellipse cx="32" cy="52" rx="12" ry="4" stroke="#14b8a6" stroke-width="1.5" stroke-dasharray="4 2" opacity="0.8"/>
                <ellipse cx="32" cy="52" rx="5" ry="2" fill="#2dd4bf"/>

                <!-- Navigation Crosshair Grid Lines -->
                <line x1="10" y1="52" x2="54" y2="52" stroke="#14b8a6" stroke-width="1" stroke-opacity="0.4"/>
                <line x1="32" y1="44" x2="32" y2="60" stroke="#14b8a6" stroke-width="1" stroke-opacity="0.4"/>

                <!-- Luxury Medical Teardrop Pin Body (Harmonized Emerald/Teal & Navy) -->
                <path d="M32 8 C23 8 16 15 16 24 C16 35 29 46 32 49 C35 46 48 35 48 24 C48 15 41 8 32 8 Z" fill="url(#${gid}-pin)" stroke="#ffffff" stroke-width="2" stroke-linejoin="round" filter="drop-shadow(0 4px 6px rgba(11,66,65,0.35))"/>

                <!-- Inner White Target Core Disc -->
                <circle cx="32" cy="23" r="10" fill="#ffffff" stroke="#ccfbf1" stroke-width="1"/>

                <!-- Clinic Medical Cross Emblem in Center of Pin -->
                <path d="M30 17 H34 V21 H38 V25 H34 V29 H30 V25 H26 V21 H30 Z" fill="#087a72"/>

                <!-- Pin Highlight Glaze -->
                <path d="M22 15 C25 12 30 11 34 11" stroke="#ffffff" stroke-width="1.5" stroke-linecap="round" stroke-opacity="0.7"/>

                <!-- Signal Beacon Waves Above Pin -->
                <path d="M27 6 C29 4.5 35 4.5 37 6" stroke="#2dd4bf" stroke-width="1.5" stroke-linecap="round"/>
            </svg>`;
        }
    };

    /**
     * PUBLIC API METHODS
     */
    const DentalIcons = {
        logos: logos,
        services: services,
        badges: badges,

        /**
         * Get Main Clinic Logo SVG
         * @param {Object} [options]
         * @param {string} [options.variant='full'] - 'full' (with wordmark) or 'mark' (emblem only)
         * @param {string} [options.className]
         * @param {string} [options.theme='light'] - 'light' or 'dark'
         * @returns {string} SVG HTML string
         */
        getLogoSvg: function (options = {}) {
            const variant = options.variant || 'full';
            if (variant === 'mark' && typeof logos.mark === 'function') {
                return logos.mark(options);
            }
            return logos.full(options);
        },

        /**
         * Get Dental Service SVG
         * @param {string} serviceKey - ORTHODONTICS, IMPLANT, VENEER, PIEZOTOME, ENDODONTICS, WHITENING, GENERAL
         * @param {Object} [options]
         * @returns {string} SVG HTML string
         */
        getServiceSvg: function (serviceKey, options = {}) {
            const normalized = String(serviceKey || '').toUpperCase().trim();
            const renderer = services[normalized] || services.GENERAL || services.ORTHODONTICS;
            return renderer(options);
        },

        /**
         * Get Feature Badge SVG
         * @param {string} badgeKey - AI_DOCTOR, WARRANTY, LOYALTY, BRANCH_MAP
         * @param {Object} [options]
         * @returns {string} SVG HTML string
         */
        getBadgeSvg: function (badgeKey, options = {}) {
            const normalized = String(badgeKey || '').toUpperCase().trim();
            switch (normalized) {
                case 'AI_DOCTOR':
                    return badges.AI_DOCTOR(options);
                case 'WARRANTY':
                    return badges.WARRANTY(options);
                case 'LOYALTY':
                    return badges.LOYALTY(options);
                case 'BRANCH_MAP':
                    return badges.BRANCH_MAP(options);
                default:
                    return badges.WARRANTY(options);
            }
        },

        /**
         * Get Standalone UI SVG helper (for offline / zero-font-load immunity)
         * @param {string} iconKey - HOTLINE, ZALO, CALENDAR, SCROLL_TOP, CART, STAR, CHECK, CLOSE, PAPER_PLANE
         * @param {Object} [options]
         * @returns {string} SVG HTML string
         */
        getUiSvg: function (iconKey, options = {}) {
            const cls = options.className || 'w-4 h-4 inline-block';
            const normalized = String(iconKey || '').toUpperCase().trim();
            switch (normalized) {
                case 'HOTLINE':
                case 'PHONE':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>`;
                case 'CALENDAR':
                case 'BOOKING':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><path d="m9 16 2 2 4-4"/></svg>`;
                case 'CART':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/></svg>`;
                case 'STAR':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`;
                case 'CHECK':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>`;
                case 'CLOSE':
                case 'X':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`;
                case 'PAPER_PLANE':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>`;
                case 'SCROLL_TOP':
                case 'CHEVRON_UP':
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m18 15-6-6-6 6"/></svg>`;
                default:
                    return `<svg class="${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/></svg>`;
            }
        },

        /**
         * Auto-inject SVGs into DOM elements matching data attributes:
         * - data-dental-logo="full|mark"
         * - data-dental-service="KEY"
         * - data-dental-badge="KEY"
         * - data-dental-ui="KEY"
         */
        injectAll: function (rootElement = document) {
            if (!rootElement || !rootElement.querySelectorAll) return;

            // Logos
            rootElement.querySelectorAll('[data-dental-logo]').forEach(el => {
                const variant = el.getAttribute('data-dental-logo') || 'full';
                const cls = el.getAttribute('data-dental-class') || '';
                const theme = el.getAttribute('data-dental-theme') || 'light';
                el.innerHTML = DentalIcons.getLogoSvg({ variant, className: cls, theme });
            });

            // Services
            rootElement.querySelectorAll('[data-dental-service]').forEach(el => {
                const key = el.getAttribute('data-dental-service');
                const cls = el.getAttribute('data-dental-class') || '';
                el.innerHTML = DentalIcons.getServiceSvg(key, { className: cls });
            });

            // Badges
            rootElement.querySelectorAll('[data-dental-badge]').forEach(el => {
                const key = el.getAttribute('data-dental-badge');
                const cls = el.getAttribute('data-dental-class') || '';
                el.innerHTML = DentalIcons.getBadgeSvg(key, { className: cls });
            });

            // UI Icons
            rootElement.querySelectorAll('[data-dental-ui]').forEach(el => {
                const key = el.getAttribute('data-dental-ui');
                const cls = el.getAttribute('data-dental-class') || '';
                el.innerHTML = DentalIcons.getUiSvg(key, { className: cls });
            });
        }
    };

    // Auto-run injectAll when DOM content is loaded
    if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => DentalIcons.injectAll());
        } else {
            DentalIcons.injectAll();
        }
    }

    return DentalIcons;
}));
