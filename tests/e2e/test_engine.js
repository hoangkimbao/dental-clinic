/**
 * DentalCare E2E Testing Engine
 * Opaque-box requirement-driven verification engine for DentalCare Clinic Web Portal.
 * Autonomous, zero-external-dependency DOM/CSS/JS analyzer.
 */

const fs = require('fs');
const path = require('path');
const http = require('http');

class DentalTestEngine {
    constructor(projectRoot) {
        this.projectRoot = projectRoot || path.resolve(__dirname, '../..');
        this.staticDir = path.join(this.projectRoot, 'src/main/resources/static');
        this.indexPath = path.join(this.staticDir, 'index.html');
        this.cssPath = path.join(this.staticDir, 'css/clinic-ui-refresh.css');
        this.appJsPath = path.join(this.staticDir, 'js/app.js');
        this.dentalIconsPath = path.join(this.staticDir, 'js/dental-icons.js');

        this.indexHtml = '';
        this.cssContent = '';
        this.appJsContent = '';
        this.dentalIconsContent = '';

        this.parsedDom = null;
        this.loadAssets();
    }

    loadAssets() {
        if (fs.existsSync(this.indexPath)) {
            this.indexHtml = fs.readFileSync(this.indexPath, 'utf-8');
        }
        if (fs.existsSync(this.cssPath)) {
            this.cssContent = fs.readFileSync(this.cssPath, 'utf-8');
        }
        if (fs.existsSync(this.appJsPath)) {
            this.appJsContent = fs.readFileSync(this.appJsPath, 'utf-8');
        }
        if (fs.existsSync(this.dentalIconsPath)) {
            this.dentalIconsContent = fs.readFileSync(this.dentalIconsPath, 'utf-8');
        }
    }

    // HTML tag & element analyzer
    findTagMatches(tagName, attrs = {}) {
        const regex = new RegExp(`<${tagName}\\b([^>]*)>`, 'gi');
        const matches = [];
        let match;
        while ((match = regex.exec(this.indexHtml)) !== null) {
            const attrString = match[1];
            let matched = true;
            for (const [key, val] of Object.entries(attrs)) {
                const attrRegex = new RegExp(`${key}=["']([^"']*)["']`, 'i');
                const attrMatch = attrString.match(attrRegex);
                if (!attrMatch) {
                    matched = false;
                    break;
                }
                if (val instanceof RegExp) {
                    if (!val.test(attrMatch[1])) {
                        matched = false;
                        break;
                    }
                } else if (typeof val === 'string') {
                    if (!attrMatch[1].includes(val)) {
                        matched = false;
                        break;
                    }
                }
            }
            if (matched) {
                matches.push({
                    fullTag: match[0],
                    attrs: attrString,
                    index: match.index
                });
            }
        }
        return matches;
    }

    getElementSnippet(elementId, maxLen = 2000) {
        const idRegex = new RegExp(`id=["']${elementId}["']`, 'i');
        const match = this.indexHtml.match(idRegex);
        if (!match) return null;
        const start = Math.max(0, match.index - 50);
        return this.indexHtml.substring(start, start + maxLen);
    }

    hasElementWithId(id) {
        const regex = new RegExp(`id=["']${id}["']`, 'i');
        return regex.test(this.indexHtml);
    }

    countOccurrences(regexOrStr) {
        if (typeof regexOrStr === 'string') {
            const escaped = regexOrStr.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(escaped, 'g');
            const matches = this.indexHtml.match(regex);
            return matches ? matches.length : 0;
        }
        const matches = this.indexHtml.match(new RegExp(regexOrStr.source, regexOrStr.flags.includes('g') ? regexOrStr.flags : regexOrStr.flags + 'g'));
        return matches ? matches.length : 0;
    }

    // Tag hierarchy check (e.g. is target tag closed before another section starts?)
    isTagClosedBefore(openingTagId, targetClosingTag, nextSectionId) {
        const openMatch = this.indexHtml.match(new RegExp(`id=["']${openingTagId}["']`, 'i'));
        const nextMatch = this.indexHtml.match(new RegExp(`id=["']${nextSectionId}["']`, 'i'));
        if (!openMatch || !nextMatch) return false;

        const intermediateHtml = this.indexHtml.substring(openMatch.index, nextMatch.index);
        const closingTagRegex = new RegExp(`</${targetClosingTag}>`, 'i');
        return closingTagRegex.test(intermediateHtml);
    }

    // CSS variable and rule analyzer
    getCssVariable(varName) {
        const regex = new RegExp(`${varName}\\s*:\\s*([^;]+);`, 'i');
        const match = this.cssContent.match(regex);
        return match ? match[1].trim() : null;
    }

    hasCssSelector(selector) {
        const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(`${escaped}\\s*\\{`, 'i');
        return regex.test(this.cssContent);
    }

    getCssProperty(selector, propertyName) {
        const blockRegex = new RegExp(`(?:^|[\\}\\s])${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}[^\\{]*\\{([^\\}]+)\\}`, 'im');
        const match = this.cssContent.match(blockRegex);
        if (!match) return null;
        const propRegex = new RegExp(`${propertyName}\\s*:\\s*([^;!]+)(?:!important)?;`, 'i');
        const propMatch = match[1].match(propRegex);
        return propMatch ? propMatch[1].trim() : null;
    }

    // Viewport Width Box-Model Simulation
    simulateNavbarWidth(viewportWidth, isLoggedIn = false) {
        // Compute static widths of elements in header
        // Logo: icon (44px) + gap (12px) + text (~75px) = 131px
        const logoWidth = 131;
        const cartBtnWidth = 42;
        const hamburgerBtnWidth = 42;
        const gaps = 20;

        let authWidth = 0;
        if (isLoggedIn) {
            // Check if app.js has been updated to collapse user badge on mobile
            const collapsesOnMobile = /hidden\s+sm:flex/i.test(this.appJsContent) || /max-w-\[140px\]\s+hidden/i.test(this.appJsContent);
            if (collapsesOnMobile && viewportWidth < 640) {
                // Collapsed to avatar + logout: ~60px
                authWidth = 60;
            } else {
                // Uncollapsed user badge with name + portal pill = 278px
                authWidth = 278;
            }
        } else {
            // Logged out buttons
            const hidesTextOnMobile = /hidden\s+xs:inline/i.test(this.indexHtml);
            if (hidesTextOnMobile) {
                // Ghost xs: breakpoint fails in standard Tailwind, so text is hidden or visible depending on fix
                const fixedBreakpoint = /hidden\s+sm:inline/i.test(this.indexHtml) || /sm:inline/i.test(this.indexHtml);
                authWidth = fixedBreakpoint && viewportWidth < 640 ? 70 : 180;
            } else {
                authWidth = 180;
            }
        }

        const totalRequiredWidth = logoWidth + cartBtnWidth + hamburgerBtnWidth + authWidth + gaps;
        const padding = viewportWidth < 640 ? 32 : 48;
        const availableWidth = viewportWidth - padding;
        const overflow = totalRequiredWidth - availableWidth;

        return {
            viewportWidth,
            availableWidth,
            totalRequiredWidth,
            overflow: Math.max(0, overflow),
            hasOverflow: overflow > 0
        };
    }

    // Touch target measurement simulation
    auditTouchTargets() {
        const substandard = [];
        // Examine buttons with explicitly undersized classes
        const undersizedPatterns = [
            { id: 'navbar-logout', selector: 'button[onclick*="handleLogout"]', pattern: /w-8\s+h-8/ },
            { id: 'cart-drawer-close', selector: '#cart-drawer button', pattern: /p-1\.5/ },
            { id: 'cart-item-qty', selector: 'cart qty buttons', pattern: /w-9\s+h-9/ },
            { id: 'packaging-modal-close', selector: '#product-packaging-modal', pattern: /w-7\s+h-7/ },
            { id: 'modal-close-generic', selector: 'modal close buttons', pattern: /p-2\s+rounded-xl\s+text-slate-400/ },
            { id: 'quick-time-slot', selector: 'booking time slots', pattern: /py-2\s+px-1/ }
        ];

        for (const target of undersizedPatterns) {
            if (target.pattern.test(this.indexHtml) || target.pattern.test(this.appJsContent)) {
                // Check if min-w-[44px] min-h-[44px] has been added
                const fixed = /min-w-\[44px\]\s+min-h-\[44px\]/i.test(this.indexHtml) || /min-w-\[44px\]/i.test(this.appJsContent);
                if (!fixed) {
                    substandard.push(target.id);
                }
            }
        }
        return substandard;
    }
}

module.exports = DentalTestEngine;
