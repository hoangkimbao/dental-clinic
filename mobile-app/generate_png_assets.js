/**
 * Standalone Pure Node.js Generator for DentalCare Luxury Mobile PNG Assets
 * Zero external npm dependencies — uses only Node.js built-in modules (fs, path, zlib).
 * Generates fully compliant binary PNG files with genuine 8-byte magic header:
 * 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
 */

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// CRC32 table implementation
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) {
    c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
  }
  crcTable[n] = c;
}

function crc32(buf) {
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    crc = crcTable[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function makeChunk(typeStr, dataBuf) {
  const typeBuf = Buffer.from(typeStr, 'ascii');
  const lenBuf = Buffer.alloc(4);
  lenBuf.writeUInt32BE(dataBuf.length, 0);

  const toCrc = Buffer.concat([typeBuf, dataBuf]);
  const crcVal = crc32(toCrc);
  const crcBuf = Buffer.alloc(4);
  crcBuf.writeUInt32BE(crcVal, 0);

  return Buffer.concat([lenBuf, typeBuf, dataBuf, crcBuf]);
}

function createPngBuffer(width, height, isFavicon) {
  const PNG_HEADER = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);

  // IHDR: width(4), height(4), bitDepth=8(1), colorType=6(RGBA)(1), comp=0(1), filter=0(1), interlace=0(1)
  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8;  // 8 bits per channel
  ihdrData[9] = 6;  // RGBA
  ihdrData[10] = 0; // deflate
  ihdrData[11] = 0; // filter method
  ihdrData[12] = 0; // no interlace
  const ihdrChunk = makeChunk('IHDR', ihdrData);

  // Raw image scanlines
  const bytesPerPixel = 4;
  const scanlineLength = 1 + width * bytesPerPixel; // 1 filter byte + RGBA pixels
  const rawData = Buffer.alloc(height * scanlineLength);

  const cx = width / 2;
  const cy = height / 2;
  const scale = Math.min(width, height) / 512.0;

  for (let y = 0; y < height; y++) {
    const rowOffset = y * scanlineLength;
    rawData[rowOffset] = 0; // Filter type 0 (None)

    for (let x = 0; x < width; x++) {
      const pxOffset = rowOffset + 1 + x * bytesPerPixel;

      // Base gradient: Luxury Teal/Emerald (#042f2e -> #0d9488)
      const t = (x / width) * 0.4 + (y / height) * 0.6;
      let r = Math.round(4 + (13 - 4) * t);
      let g = Math.round(47 + (148 - 47) * t);
      let b = Math.round(46 + (136 - 46) * t);
      let a = 255;

      const dx = (x - cx) / scale;
      const dy = (y - cy) / scale;

      if (!isFavicon) {
        // Glowing Aura Circle around center
        const distSq = dx * dx + dy * dy;
        if (distSq < 180 * 180) {
          const glowAlpha = Math.max(0, 1 - Math.sqrt(distSq) / 180) * 0.35;
          r = Math.round(r * (1 - glowAlpha) + 45 * glowAlpha);
          g = Math.round(g * (1 - glowAlpha) + 212 * glowAlpha);
          b = Math.round(b * (1 - glowAlpha) + 191 * glowAlpha);
        }

        // Tooth Crown (White)
        const inLeftCrown = (dx + 50) * (dx + 50) + (dy + 65) * (dy + 65) < 60 * 60;
        const inRightCrown = (dx - 50) * (dx - 50) + (dy + 65) * (dy + 65) < 60 * 60;
        const inMidBody = dx >= -50 && dx <= 50 && dy >= -65 && dy <= 30;

        // Tooth Roots
        const inLeftRoot = (dx + 35) * (dx + 35) / (35 * 35) + (dy - 40) * (dy - 40) / (55 * 55) < 1.0 && dy <= 90;
        const inRightRoot = (dx - 35) * (dx - 35) / (35 * 35) + (dy - 40) * (dy - 40) / (55 * 55) < 1.0 && dy <= 90;

        if (inLeftCrown || inRightCrown || inMidBody || inLeftRoot || inRightRoot) {
          // White enamel with slight luxury gradient shading
          const toothGrad = (dy + 80) / 160;
          r = Math.round(255 - toothGrad * 15);
          g = Math.round(255 - toothGrad * 10);
          b = 255;
        }

        // Golden Accent Cross (#f59e0b)
        const inCrossHoriz = dx >= 10 && dx <= 70 && dy >= -55 && dy <= -35;
        const inCrossVert = dx >= 30 && dx <= 50 && dy >= -75 && dy <= -15;
        if (inCrossHoriz || inCrossVert) {
          r = 245;
          g = 158;
          b = 11;
        }
      } else {
        // Favicon 64x64 simplified tooth
        const fdx = x - 32;
        const fdy = y - 32;
        const inFavCircle = fdx * fdx + fdy * fdy < 20 * 20;
        if (inFavCircle) {
          r = 255;
          g = 255;
          b = 255;
        }
        const inCrossH = fdx >= -4 && fdx <= 4 && fdy >= -10 && fdy <= 10;
        const inCrossV = fdx >= -10 && fdx <= 10 && fdy >= -4 && fdy <= 4;
        if (inCrossH || inCrossV) {
          r = 13;
          g = 148;
          b = 136;
        }
      }

      rawData[pxOffset] = r;
      rawData[pxOffset + 1] = g;
      rawData[pxOffset + 2] = b;
      rawData[pxOffset + 3] = a;
    }
  }

  // Compress IDAT
  const compressed = zlib.deflateSync(rawData, { level: 9 });
  const idatChunk = makeChunk('IDAT', compressed);

  // IEND
  const iendChunk = makeChunk('IEND', Buffer.alloc(0));

  return Buffer.concat([PNG_HEADER, ihdrChunk, idatChunk, iendChunk]);
}

function main() {
  const assetsDir = path.resolve(__dirname, 'assets');
  if (!fs.existsSync(assetsDir)) {
    fs.mkdirSync(assetsDir, { recursive: true });
  }

  const targets = [
    { name: 'icon.png', width: 1024, height: 1024, isFavicon: false },
    { name: 'adaptive-icon.png', width: 1024, height: 1024, isFavicon: false },
    { name: 'splash.png', width: 1242, height: 2436, isFavicon: false },
    { name: 'favicon.png', width: 64, height: 64, isFavicon: true },
  ];

  console.log('Generating authentic binary PNG assets for DentalCare Mobile App...');

  for (const t of targets) {
    const dest = path.join(assetsDir, t.name);
    const buf = createPngBuffer(t.width, t.height, t.isFavicon);
    fs.writeFileSync(dest, buf);

    // Verify
    const verifyBuf = fs.readFileSync(dest);
    const magic = verifyBuf.subarray(0, 8);
    const expected = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
    const isMatch = magic.equals(expected);

    console.log(`- ${t.name}: ${buf.length} bytes | Magic: ${magic.toString('hex')} | Valid: ${isMatch}`);
  }

  console.log('All 4 binary PNG assets generated successfully.');
}

if (require.main === module) {
  main();
}

module.exports = { createPngBuffer, makeChunk, crc32 };
