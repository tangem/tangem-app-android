import StyleDictionary from 'style-dictionary';
import { register, getTransforms } from '@tokens-studio/sd-transforms';
import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// ── Paths ──────────────────────────────────────────────────────────────────────
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tokensDir = path.join(__dirname, '..', 'ds-tokens', 'tokens');
const outputDir = path.join(
  __dirname, '..', 'src', 'main', 'java', 'com', 'tangem', 'core', 'ui', 'res', 'generated',
);

const PACKAGE = 'com.tangem.core.ui.res.generated';

fs.mkdirSync(outputDir, { recursive: true });

// ── Register sd-transforms ─────────────────────────────────────────────────────
// Use CSS platform for sd-transforms to get ts/color/css/hexrgba (resolves rgba to hex).
// Compose color conversion is done in the format function instead of as a transform,
// because the color/composeColor transform interferes with rgba reference resolution.
await register(StyleDictionary, { platform: 'css' });

// ── Token set definitions ──────────────────────────────────────────────────────
// Reference-only sets (provide values for other tokens but not in the output)
const coreSets = [
  'core/palette',
  'core/font',
  'core/dimension',
];

// Theme-independent semantic sets
const sizeSets = [
  'semantic/size/opacity',
  'semantic/size/blur',
  'semantic/size/border',
  'semantic/size/size',
  'semantic/size/spacing',
];

const fontSets = [
  'semantic/font/sizes/android',
  'semantic/font/styles',
];

const sharedSets = [
  'semantic/theme/shadows',
  'semantic/theme/gradient',
];

// Theme-specific sets
// Note: material variant files (glass/blur/solid) define colliding paths and are excluded.
// The material color tokens come from materials/light and materials/dark instead.
const themeBuilds = {
  Light: {
    sets: [
      ...coreSets, ...sizeSets, ...fontSets, ...sharedSets,
      'semantic/theme/light',
      'semantic/theme/materials/light',
    ],
  },
  Dark: {
    sets: [
      ...coreSets, ...sizeSets, ...fontSets, ...sharedSets,
      'semantic/theme/dark',
      'semantic/theme/materials/dark',
    ],
  },
};

// For theme-independent builds (dimensions, typography, etc.)
const sharedBuildSets = [...coreSets, ...sizeSets, ...fontSets, ...sharedSets];

for (const [theme, { sets }] of Object.entries(themeBuilds)) {
  console.log(`${theme} theme: ${sets.length} token sets`);
}

// ── Helpers ────────────────────────────────────────────────────────────────────

/** Convert token path segments to a camelCase property name */
function toCamelCase(segments) {
  return segments
    .map((seg, i) => {
      // kebab-case → camelCase
      const cleaned = seg.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase());
      if (i === 0) return cleaned;
      return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
    })
    .join('');
}

/** Check if a token is from core/palette, dimension, or gradient source sets (not for output) */
function isSourceOnlyToken(token) {
  const top = token.path[0];
  return top === 'palette' || top === 'dimension' || top === 'gradient';
}

/**
 * Convert a resolved CSS color string to Compose Color(0xAARRGGBB).
 * Handles: #RRGGBB, #AARRGGBB, rgba(r, g, b, a), rgb(r, g, b)
 */
function toComposeColor(value, tokenPath = '') {
  if (typeof value !== 'string') {
    throw new Error(`Unrecognized color value for ${tokenPath}: ${JSON.stringify(value)}`);
  }

  // #RRGGBB
  const hex6 = value.match(/^#([0-9a-fA-F]{6})$/);
  if (hex6) return `Color(0xFF${hex6[1].toUpperCase()})`;

  // #AARRGGBB (8-digit hex)
  const hex8 = value.match(/^#([0-9a-fA-F]{8})$/);
  if (hex8) return `Color(0x${hex8[1].toUpperCase()})`;

  // rgba(r, g, b, a)
  const rgba = value.match(/^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d.]+)\s*\)$/);
  if (rgba) {
    const r = parseInt(rgba[1]).toString(16).padStart(2, '0').toUpperCase();
    const g = parseInt(rgba[2]).toString(16).padStart(2, '0').toUpperCase();
    const b = parseInt(rgba[3]).toString(16).padStart(2, '0').toUpperCase();
    const a = Math.round(parseFloat(rgba[4]) * 255).toString(16).padStart(2, '0').toUpperCase();
    return `Color(0x${a}${r}${g}${b})`;
  }

  // rgb(r, g, b)
  const rgb = value.match(/^rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)$/);
  if (rgb) {
    const r = parseInt(rgb[1]).toString(16).padStart(2, '0').toUpperCase();
    const g = parseInt(rgb[2]).toString(16).padStart(2, '0').toUpperCase();
    const b = parseInt(rgb[3]).toString(16).padStart(2, '0').toUpperCase();
    return `Color(0xFF${r}${g}${b})`;
  }

  // Transparent
  if (value === 'transparent' || value === '#00000000') return 'Color(0x00000000)';

  throw new Error(`Unrecognized color format for ${tokenPath}: "${value}"`);
}

/** Group tokens by their first N path segments */
function groupByPath(tokens, depth = 1) {
  const groups = {};
  for (const token of tokens) {
    const key = token.path.slice(0, depth).join('.');
    if (!groups[key]) groups[key] = [];
    groups[key].push(token);
  }
  return groups;
}

// ── Custom formats ─────────────────────────────────────────────────────────────

/**
 * Kotlin format for color tokens.
 * Generates: class TangemLightColorTokens / TangemDarkColorTokens
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-colors',
  format: ({ dictionary, options }) => {
    const themeName = options.themeName; // "Light" or "Dark"
    const objectName = `Tangem${themeName}ColorTokens`;

    const colorTokens = dictionary.allTokens.filter(
      t => t.$type === 'color' && !isSourceOnlyToken(t),
    );

    // Group by top-level category (color.text, color.bg, color.icon, etc.)
    const groups = groupByPath(colorTokens, 2);
    const sections = [];

    for (const [groupKey, tokens] of Object.entries(groups)) {
      const comment = `    // ${groupKey}`;
      const props = tokens.map(token => {
        const propName = toCamelCase(token.path);
        const value = toComposeColor(token.$value, token.path.join('.'));
        return `    val ${propName} = ${value}`;
      });
      sections.push([comment, ...props].join('\n'));
    }

    return [
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.graphics.Color',
      '',
      '/**',
      ` * Auto-generated from design tokens. Do not edit manually.`,
      ` * Theme: ${themeName}`,
      ' */',
      `internal class ${objectName} {`,
      sections.join('\n\n'),
      '}',
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for dimension tokens (spacing, size, border-radius, border-width).
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-dimensions',
  format: ({ dictionary }) => {
    const dimTokens = dictionary.allTokens.filter(t => {
      // Exclude font tokens — letter-spacing values are already in typography tokens as sp
      if (t.path[0] === 'font') return false;
      return (
        (t.$type === 'dimension' || t.$type === 'borderRadius' || t.$type === 'borderWidth') &&
        !isSourceOnlyToken(t)
      );
    });

    const groups = groupByPath(dimTokens, 1);
    const sections = [];

    for (const [groupKey, tokens] of Object.entries(groups)) {
      const comment = `    // ${groupKey}`;
      const props = tokens.map(token => {
        const propName = toCamelCase(token.path);
        // Resolved value is in px (number), convert to dp
        const raw = parseFloat(token.$value);
        if (isNaN(raw)) throw new Error(`Non-numeric dimension value for ${token.path.join('.')}: "${token.$value}"`);
        const dpVal = `${raw}.dp`;
        return `    val ${propName} = ${dpVal}`;
      });
      sections.push([comment, ...props].join('\n'));
    }

    return [
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.unit.dp',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      'internal class TangemDimensionTokens {',
      sections.join('\n\n'),
      '}',
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for opacity tokens.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-opacity',
  format: ({ dictionary }) => {
    const opacityTokens = dictionary.allTokens.filter(
      t => t.$type === 'opacity' && !isSourceOnlyToken(t),
    );

    const props = opacityTokens.map(token => {
      const propName = toCamelCase(token.path);
      const raw = parseFloat(token.$value);
      if (isNaN(raw)) throw new Error(`Non-numeric opacity value for ${token.path.join('.')}: "${token.$value}"`);
      const floatVal = `${raw}f`;
      return `    val ${propName} = ${floatVal}`;
    });

    return [
      `package ${PACKAGE}`,
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      'internal class TangemOpacityTokens {',
      ...props,
      '}',
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for typography tokens.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-typography',
  format: ({ dictionary }) => {
    const typoTokens = dictionary.allTokens.filter(
      t => t.$type === 'typography' && !isSourceOnlyToken(t),
    );

    const props = typoTokens.map(token => {
      const propName = toCamelCase(token.path);
      const v = token.$value;

      // v is an object: { fontFamily, fontWeight, fontSize, lineHeight, letterSpacing, ... }
      const tp = token.path.join('.');
      if (!v.fontWeight) throw new Error(`Missing fontWeight for ${tp}`);
      const fontWeight = mapFontWeight(v.fontWeight);
      const fontSize = parseFloat(v.fontSize);
      if (isNaN(fontSize)) throw new Error(`Non-numeric fontSize for ${tp}: "${v.fontSize}"`);
      const lineHeight = parseFloat(v.lineHeight);
      if (isNaN(lineHeight)) throw new Error(`Non-numeric lineHeight for ${tp}: "${v.lineHeight}"`);
      const letterSpacing = parseFloat(v.letterSpacing);
      if (isNaN(letterSpacing)) throw new Error(`Non-numeric letterSpacing for ${tp}: "${v.letterSpacing}"`);

      // display and heading categories get LineBreak.Heading (matches TangemTypography2)
      const category = token.path[1]; // display, heading, body, subheading, caption
      const isHeading = category === 'display' || category === 'heading';

      const lines = [
        `    val ${propName} = TextStyle(`,
        `        fontFamily = InterFamily,`,
        `        fontWeight = ${fontWeight},`,
        `        fontSize = ${fontSize}.sp,`,
        `        lineHeight = ${lineHeight}.sp,`,
        `        letterSpacing = ${letterSpacing < 0 ? `(${letterSpacing})` : letterSpacing}.sp,`,
        `        lineHeightStyle = LineHeightStyle(`,
        `            alignment = LineHeightStyle.Alignment.Center,`,
        `            trim = LineHeightStyle.Trim.None,`,
        `        ),`,
      ];
      if (isHeading) {
        lines.push(`        lineBreak = LineBreak.Heading,`);
      }
      lines.push(`    )`);

      return lines.join('\n');
    });

    return [
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.text.TextStyle',
      'import androidx.compose.ui.text.font.FontWeight',
      'import androidx.compose.ui.text.style.LineBreak',
      'import androidx.compose.ui.text.style.LineHeightStyle',
      'import androidx.compose.ui.unit.sp',
      'import com.tangem.core.ui.res.InterFamily',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      'internal class TangemTypographyTokens {',
      props.join('\n\n'),
      '}',
      '',
    ].join('\n');
  },
});

function mapFontWeight(value) {
  const num = parseInt(value, 10);
  if (!isNaN(num)) {
    if (num <= 400) return 'FontWeight.Normal';
    if (num <= 500) return 'FontWeight.Medium';
    if (num <= 600) return 'FontWeight.SemiBold';
    return 'FontWeight.Bold';
  }
  const lower = String(value).toLowerCase();
  if (lower.includes('semibold') || lower.includes('semi bold')) return 'FontWeight.SemiBold';
  if (lower.includes('bold')) return 'FontWeight.Bold';
  if (lower.includes('medium')) return 'FontWeight.Medium';
  return 'FontWeight.Normal';
}

/**
 * Kotlin format for shadow tokens.
 * Shadow tokens are composite (type: shadow) with object $value containing
 * blur, spread, color, offsetX, offsetY, type.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-shadows',
  format: ({ dictionary }) => {
    const shadowTokens = dictionary.allTokens.filter(
      t => (t.$type === 'shadow' || t.$type === 'boxShadow') && !isSourceOnlyToken(t),
    );

    const entries = shadowTokens.map(token => {
      const propName = toCamelCase(token.path);
      const v = token.$value;
      const sp = token.path.join('.');
      if (!v) throw new Error(`Missing shadow value for ${sp}`);
      const blur = parseFloat(v.blur);
      if (isNaN(blur)) throw new Error(`Non-numeric blur for ${sp}: "${v.blur}"`);
      const spread = parseFloat(v.spread);
      if (isNaN(spread)) throw new Error(`Non-numeric spread for ${sp}: "${v.spread}"`);
      const offsetX = parseFloat(v.offsetX);
      if (isNaN(offsetX)) throw new Error(`Non-numeric offsetX for ${sp}: "${v.offsetX}"`);
      const offsetY = parseFloat(v.offsetY);
      if (isNaN(offsetY)) throw new Error(`Non-numeric offsetY for ${sp}: "${v.offsetY}"`);
      if (!v.color) throw new Error(`Missing color for ${sp}`);
      const colorVal = toComposeColor(v.color, sp + '.color');

      return [
        `    val ${propName}Blur = ${blur}.dp`,
        `    val ${propName}OffsetX = ${offsetX}.dp`,
        `    val ${propName}OffsetY = ${offsetY}.dp`,
        `    val ${propName}Spread = ${spread}.dp`,
        `    val ${propName}Color = ${colorVal}`,
      ].join('\n');
    });

    return [
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.graphics.Color',
      'import androidx.compose.ui.unit.dp',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      'internal class TangemShadowTokens {',
      entries.join('\n\n'),
      '}',
      '',
    ].join('\n');
  },
});

// ── Build ──────────────────────────────────────────────────────────────────────

const composePlatformTransforms = [
  ...getTransforms({ platform: 'css' }),
  'name/camel',
];

// Build color tokens per theme (light/dark)
for (const [themeName, { sets }] of Object.entries(themeBuilds)) {
  console.log(`\nBuilding ${themeName} color tokens...`);

  const sd = new StyleDictionary({
    source: sets.map(s => path.join(tokensDir, `${s}.json`)),
    preprocessors: ['tokens-studio'],
    usesDtcg: true,
    log: { warnings: 'disabled', errors: { brokenReferences: 'console' } },
    platforms: {
      compose: {
        transforms: composePlatformTransforms,
        buildPath: outputDir + '/',
        files: [
          {
            destination: `Tangem${themeName}ColorTokens.kt`,
            format: 'kotlin/compose-colors',
            options: { themeName },
            filter: token => token.$type === 'color' && !isSourceOnlyToken(token),
          },
        ],
      },
    },
  });

  await sd.buildAllPlatforms();
  console.log(`  ✓ Tangem${themeName}ColorTokens.kt`);
}

// Build theme-independent tokens (dimensions, opacity, typography, shadows)
console.log('\nBuilding dimension, opacity, typography, and shadow tokens...');

const sd = new StyleDictionary({
  source: sharedBuildSets.map(s => path.join(tokensDir, `${s}.json`)),
  preprocessors: ['tokens-studio'],
  usesDtcg: true,
  log: { warnings: 'disabled', errors: { brokenReferences: 'console' } },
  platforms: {
    compose: {
      transforms: composePlatformTransforms,
      buildPath: outputDir + '/',
      files: [
        {
          destination: 'TangemDimensionTokens.kt',
          format: 'kotlin/compose-dimensions',
          filter: token => {
            const t = token.$type;
            return (
              token.path[0] !== 'font' &&
              (t === 'dimension' || t === 'borderRadius' || t === 'borderWidth') &&
              !isSourceOnlyToken(token)
            );
          },
        },
        {
          destination: 'TangemOpacityTokens.kt',
          format: 'kotlin/compose-opacity',
          filter: token => token.$type === 'opacity' && !isSourceOnlyToken(token),
        },
        {
          destination: 'TangemTypographyTokens.kt',
          format: 'kotlin/compose-typography',
          filter: token => token.$type === 'typography' && !isSourceOnlyToken(token),
        },
        {
          destination: 'TangemShadowTokens.kt',
          format: 'kotlin/compose-shadows',
          filter: token => (token.$type === 'shadow' || token.$type === 'boxShadow') && !isSourceOnlyToken(token),
        },
      ],
    },
  },
});

await sd.buildAllPlatforms();
console.log('  ✓ TangemDimensionTokens.kt');
console.log('  ✓ TangemOpacityTokens.kt');
console.log('  ✓ TangemTypographyTokens.kt');
console.log('  ✓ TangemShadowTokens.kt');

// ── Write source hash ─────────────────────────────────────────────────────────
// Hash all token JSON files so Gradle can verify generated code matches ds-tokens.
function computeTokensHash() {
  const files = [];
  function walk(dir) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.name.endsWith('.json')) files.push(full);
    }
  }
  walk(tokensDir);
  files.sort(); // deterministic order

  const hash = crypto.createHash('sha256');
  for (const file of files) {
    hash.update(path.relative(tokensDir, file).split(path.sep).join('/'));
    hash.update('\0');
    hash.update(fs.readFileSync(file));
    hash.update('\0');
  }
  return hash.digest('hex');
}

const tokensHash = computeTokensHash();
fs.writeFileSync(path.join(outputDir, '.tokens-hash'), tokensHash + '\n');
console.log(`  ✓ .tokens-hash (${tokensHash.substring(0, 12)}…)`);

console.log(`\nDone! Output: ${outputDir}`);
