#!/usr/bin/env node
/**
 * Master E2E Test Runner for DentalCare Clinic Web Portal
 * Executes Tiers 1-4 tests programmatically and outputs JSON, TAP, and JUnit XML.
 *
 * Usage:
 *   node tests/e2e/runner.js [options]
 *
 * Options:
 *   --tier=1|2|3|4         Run only specified tier
 *   --feature=F01-F23      Run tests for specific feature
 *   --format=json|tap|xml  Specify output format (default: console + files)
 *   --bail                 Exit on first failure
 */

const fs = require('fs');
const path = require('path');
const DentalTestEngine = require('./test_engine');
const tier1Tests = require('./suites/tier1_features');
const tier2Tests = require('./suites/tier2_boundaries');
const tier3Tests = require('./suites/tier3_combos');
const tier4Tests = require('./suites/tier4_journeys');

// Parse CLI flags
const args = process.argv.slice(2);
let selectedTier = null;
let selectedFeature = null;
let outputFormat = 'console';
let bailOnFail = false;

for (const arg of args) {
    if (arg.startsWith('--tier=')) selectedTier = parseInt(arg.split('=')[1]);
    if (arg.startsWith('--feature=')) selectedFeature = arg.split('=')[1].toUpperCase();
    if (arg.startsWith('--format=')) outputFormat = arg.split('=')[1].toLowerCase();
    if (arg === '--bail') bailOnFail = true;
}

// Assemble test list
let allTests = [
    ...tier1Tests,
    ...tier2Tests,
    ...tier3Tests,
    ...tier4Tests
];

if (selectedTier) {
    allTests = allTests.filter(t => t.tier === selectedTier);
}
if (selectedFeature) {
    allTests = allTests.filter(t => t.feature === selectedFeature);
}

// Ensure results directory exists
const resultsDir = path.resolve(__dirname, 'results');
if (!fs.existsSync(resultsDir)) {
    fs.mkdirSync(resultsDir, { recursive: true });
}

// Execute tests
const startTime = Date.now();
const engine = new DentalTestEngine();
const results = [];
let passedCount = 0;
let failedCount = 0;

console.log(`\n================================================================================`);
console.log(` DentalCare Clinic Web Portal E2E Test Suite (Tiers 1 - 4)`);
console.log(` Total Selected Tests: ${allTests.length}`);
console.log(` Target Root: ${engine.projectRoot}`);
console.log(` Timestamp: ${new Date().toISOString()}`);
console.log(`================================================================================\n`);

for (let i = 0; i < allTests.length; i++) {
    const test = allTests[i];
    let testResult;
    const testStart = Date.now();

    try {
        testResult = test.run(engine);
    } catch (err) {
        testResult = {
            passed: false,
            message: `Runtime Error: ${err.message}`
        };
    }

    const durationMs = Date.now() - testStart;
    const record = {
        id: test.id,
        tier: test.tier,
        feature: test.feature || 'SYSTEM',
        featureName: test.featureName || test.name || 'System Test',
        description: test.description,
        passed: !!testResult.passed,
        message: testResult.message || '',
        durationMs
    };

    results.push(record);
    if (record.passed) {
        passedCount++;
        console.log(`  ✓ [T${record.tier}][${record.id}] ${record.description} (${durationMs}ms)`);
    } else {
        failedCount++;
        console.log(`  ✗ [T${record.tier}][${record.id}] ${record.description}`);
        console.log(`    ↳ FAILURE: ${record.message}`);
        if (bailOnFail) {
            console.log(`\n--bail flag specified. Halting on first failure.`);
            break;
        }
    }
}

const totalDurationMs = Date.now() - startTime;

// Tier breakdown statistics
const tierSummary = {
    tier1: { total: 0, passed: 0, failed: 0 },
    tier2: { total: 0, passed: 0, failed: 0 },
    tier3: { total: 0, passed: 0, failed: 0 },
    tier4: { total: 0, passed: 0, failed: 0 }
};

for (const r of results) {
    const key = `tier${r.tier}`;
    if (tierSummary[key]) {
        tierSummary[key].total++;
        if (r.passed) tierSummary[key].passed++;
        else tierSummary[key].failed++;
    }
}

// Generate report.json
const reportJson = {
    metadata: {
        suiteName: 'DentalCare Clinic E2E Test Suite',
        version: '3.0.0',
        timestamp: new Date().toISOString(),
        totalDurationMs,
        totalTests: results.length,
        passed: passedCount,
        failed: failedCount,
        passRate: results.length > 0 ? ((passedCount / results.length) * 100).toFixed(2) + '%' : '0%'
    },
    tierSummary,
    results
};

fs.writeFileSync(path.join(resultsDir, 'report.json'), JSON.stringify(reportJson, null, 2), 'utf-8');

// Generate results.tap
let tapOutput = `TAP version 13\n1..${results.length}\n`;
results.forEach((r, idx) => {
    const status = r.passed ? 'ok' : 'not ok';
    tapOutput += `${status} ${idx + 1} - [T${r.tier}][${r.id}] ${r.description}\n`;
    if (!r.passed) {
        tapOutput += `  ---\n  message: "${r.message.replace(/"/g, '\\"')}"\n  ...\n`;
    }
});
fs.writeFileSync(path.join(resultsDir, 'results.tap'), tapOutput, 'utf-8');

// Generate junit.xml
let junitXml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
junitXml += `<testsuites name="DentalCare E2E" tests="${results.length}" failures="${failedCount}" time="${(totalDurationMs / 1000).toFixed(3)}">\n`;
junitXml += `  <testsuite name="DentalCare Web Portal UI/UX" tests="${results.length}" failures="${failedCount}" time="${(totalDurationMs / 1000).toFixed(3)}">\n`;
for (const r of results) {
    junitXml += `    <testcase classname="Tier${r.tier}.${r.feature}" name="${r.id} - ${r.description.replace(/"/g, '&quot;')}" time="${(r.durationMs / 1000).toFixed(3)}">\n`;
    if (!r.passed) {
        junitXml += `      <failure message="${r.message.replace(/"/g, '&quot;')}">${r.message.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</failure>\n`;
    }
    junitXml += `    </testcase>\n`;
}
junitXml += `  </testsuite>\n</testsuites>\n`;
fs.writeFileSync(path.join(resultsDir, 'junit.xml'), junitXml, 'utf-8');

// Console Summary
console.log(`\n================================================================================`);
console.log(` Test Execution Summary`);
console.log(`================================================================================`);
console.log(` Total Tests: ${results.length}`);
console.log(` Passed:      ${passedCount} (${((passedCount / results.length) * 100).toFixed(1)}%)`);
console.log(` Failed:      ${failedCount} (${((failedCount / results.length) * 100).toFixed(1)}%)`);
console.log(` Duration:    ${(totalDurationMs / 1000).toFixed(2)}s`);
console.log(`--------------------------------------------------------------------------------`);
console.log(` Tier 1 (Feature Coverage):        ${tierSummary.tier1.passed}/${tierSummary.tier1.total} passed (${tierSummary.tier1.failed} failed)`);
console.log(` Tier 2 (Boundaries & Viewports):  ${tierSummary.tier2.passed}/${tierSummary.tier2.total} passed (${tierSummary.tier2.failed} failed)`);
console.log(` Tier 3 (Cross-Feature Combos):    ${tierSummary.tier3.passed}/${tierSummary.tier3.total} passed (${tierSummary.tier3.failed} failed)`);
console.log(` Tier 4 (Real-World Journeys):     ${tierSummary.tier4.passed}/${tierSummary.tier4.total} passed (${tierSummary.tier4.failed} failed)`);
console.log(`--------------------------------------------------------------------------------`);
console.log(` Output Artifacts:`);
console.log(`   JSON Report:  ${path.join(resultsDir, 'report.json')}`);
console.log(`   TAP Report:   ${path.join(resultsDir, 'results.tap')}`);
console.log(`   JUnit Report: ${path.join(resultsDir, 'junit.xml')}`);
console.log(`================================================================================\n`);

// Exit code logic
if (failedCount > 0) {
    console.log(`[E2E Runner] Baseline established with ${failedCount} expected defects across milestones M1-M4.`);
    // If running in baseline mode, exit with code 0 or 1 depending on environment.
    // As an automated test runner, exit code is 1 on failure.
    process.exit(1);
} else {
    console.log(`[E2E Runner] All tests passed cleanly!`);
    process.exit(0);
}
