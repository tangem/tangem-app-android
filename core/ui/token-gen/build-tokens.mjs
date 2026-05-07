import StyleDictionary from 'style-dictionary';
import { register, getTransforms } from '@tokens-studio/sd-transforms';
import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { buildIcons } from './build-icons.mjs';

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

/**
 * Build a tree of nested objects from entries.
 * Each entry: { path: string[], value: string }.
 * The last segment is the property name; preceding segments become nested objects.
 */
function buildPropertyTree(entries) {
  const root = { props: [], children: new Map() };

  for (const { path, value } of entries) {
    let node = root;
    for (let i = 0; i < path.length - 1; i++) {
      const seg = path[i];
      if (!node.children.has(seg)) {
        node.children.set(seg, { props: [], children: new Map() });
      }
      node = node.children.get(seg);
    }
    const propName = path[path.length - 1];
    const existingIdx = node.props.findIndex(p => p.name === propName);
    if (existingIdx >= 0) {
      console.warn(`  ⚠ Duplicate property path: ${path.join('.')} — overwriting`);
      node.props[existingIdx] = { name: propName, value };
    } else {
      node.props.push({ name: propName, value });
    }
  }

  return root;
}

/**
 * Render a property tree as Kotlin nested objects.
 * Returns an array of indented lines.
 */
function renderTree(node, indent = 1) {
  const pad = '    '.repeat(indent);
  const lines = [];

  for (const { name, value } of node.props) {
    lines.push(`${pad}val ${name} = ${value}`);
  }

  for (const [name, child] of node.children) {
    if (lines.length > 0) lines.push('');
    lines.push(`${pad}object ${name} {`);
    lines.push(...renderTree(child, indent + 1));
    lines.push(`${pad}}`);
  }

  return lines;
}

/**
 * Render a @Stable class tree for dimension tokens.
 * Each node with children becomes a nested @Stable class.
 * Props carry { default, type } values.
 */
function renderStableDimenClass(className, node, indent) {
  const pad = '    '.repeat(indent);
  const pad1 = '    '.repeat(indent + 1);
  const lines = [];

  lines.push(`${pad}@Stable`);
  lines.push(`${pad}class ${className} internal constructor(`);

  for (const { name, value } of node.props) {
    lines.push(`${pad1}val ${kotlinSafe(name)}: ${value.type} = ${value.default},`);
  }
  for (const [childName, childNode] of node.children) {
    const typeName = capitalize(childName);
    const propName = kotlinSafe(childName.charAt(0).toLowerCase() + childName.slice(1));
    lines.push(`${pad1}val ${propName}: ${typeName} = ${typeName}(),`);
  }

  if (node.children.size === 0) {
    lines.push(`${pad})`);
  } else {
    lines.push(`${pad}) {`);

    let first = true;
    for (const [childName, childNode] of node.children) {
      if (!first) lines.push('');
      first = false;
      lines.push(...renderStableDimenClass(capitalize(childName), childNode, indent + 1));
    }

    lines.push(`${pad}}`);
  }

  return lines;
}

/**
 * Capitalize the first letter of a string.
 */
function capitalize(str) {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/**
 * Convert a kebab-case segment to PascalCase (for object names).
 */
function toPascalCase(seg) {
  return seg
    .split('-')
    .map(part => capitalize(part))
    .join('');
}

/**
 * Wrap a name in backticks if it starts with a digit or is a Kotlin hard keyword.
 */
const KOTLIN_HARD_KEYWORDS = new Set([
  'as', 'break', 'class', 'continue', 'do', 'else', 'false', 'for', 'fun',
  'if', 'in', 'interface', 'is', 'null', 'object', 'package', 'return',
  'super', 'this', 'throw', 'true', 'try', 'typealias', 'typeof', 'val',
  'var', 'when', 'while',
]);

function kotlinSafe(name) {
  if (/^\d/.test(name) || KOTLIN_HARD_KEYWORDS.has(name)) return `\`${name}\``;
  return name;
}

// ── TangemColors3 helpers ─────────────────────────────────────────────────────

/** Shared structure tree for TangemColors3, computed from light theme codeSyntax. */
let colors3StructureTree = null;

/** Extract codeSyntax.Android path, stripping TangemTheme.colors3. prefix. */
function getAndroidCodeSyntax(token) {
  const ext = token.$extensions?.['com.figma.codeSyntax'];
  if (!ext?.Android) return null;
  const prefix = 'TangemTheme.colors3.';
  const android = ext.Android;
  return android.startsWith(prefix) ? android.slice(prefix.length) : null;
}

/** Get the token's property path for the class tree from codeSyntax, or fallback to JSON path. */
function colorTokenClassPath(token) {
  const cs = getAndroidCodeSyntax(token);
  if (cs) return cs.split('.');
  // Fallback for material tokens (no codeSyntax): use JSON path minus 'color' prefix
  const pathSegs = token.path.slice(1); // remove 'color'
  return pathSegs.map(seg => seg.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase()));
}

/**
 * Extract a simple palette reference from a token's original (unresolved) value.
 * Returns the reference string like "palette.neutral.95" or null for complex values.
 */
function extractPaletteRef(token) {
  const original = token.original?.$value;
  if (typeof original !== 'string') return null;
  const match = original.match(/^\{(palette\.[^}]+)\}$/);
  return match ? match[1] : null;
}

/**
 * Convert a palette reference path to Kotlin code referencing TangemColorPalette.
 * e.g., "palette.neutral.95" → "TangemColorPalette.Neutral.`95`"
 * e.g., "palette.opaque.base-black.60" → "TangemColorPalette.Opaque.BaseBlack.`60`"
 */
function paletteRefToKotlin(refPath) {
  const parts = refPath.split('.').slice(1); // drop "palette"
  const objParts = parts.slice(0, -1).map(seg => toPascalCase(seg));
  const leaf = parts[parts.length - 1].replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase());
  return `TangemColorPalette.${objParts.join('.')}.${kotlinSafe(leaf)}`;
}

/** Get Kotlin value for a color token: palette reference if simple, else resolved Color literal. */
function paletteRefOrColor(token) {
  const ref = extractPaletteRef(token);
  if (ref) return paletteRefToKotlin(ref);
  return toComposeColor(token.$value, token.path.join('.'));
}

/**
 * Build a class structure tree from color tokens using codeSyntax paths.
 * Each node: { props: [{name, jsonPath}], children: Map<string, node> }
 */
function buildClassStructureTree(tokens) {
  const root = { props: [], children: new Map() };

  for (const token of tokens) {
    const classPath = colorTokenClassPath(token);
    const jsonPath = token.path.join('.');

    let node = root;
    for (let i = 0; i < classPath.length - 1; i++) {
      const seg = classPath[i];
      if (!node.children.has(seg)) {
        node.children.set(seg, { props: [], children: new Map() });
      }
      node = node.children.get(seg);
    }

    const propName = classPath[classPath.length - 1];
    const existingIdx = node.props.findIndex(p => p.name === propName);
    if (existingIdx >= 0) {
      console.warn(`  ⚠ Duplicate codeSyntax path: ${classPath.join('.')} (${jsonPath}) — overwriting`);
      node.props[existingIdx] = { name: propName, jsonPath };
    } else {
      node.props.push({ name: propName, jsonPath });
    }
  }

  return root;
}

/**
 * Resolve conflicts where a name appears as both a leaf property and a child class.
 * Resolution: move the leaf into the child as "default".
 */
function resolveClassTreeConflicts(node) {
  const propsToRemove = [];

  for (const [childName, childNode] of node.children) {
    const conflictIdx = node.props.findIndex(p => p.name === childName);
    if (conflictIdx >= 0) {
      const prop = node.props[conflictIdx];
      console.log(`  ℹ Conflict resolved: "${childName}" is both property and class → moved to "${childName}.default"`);
      childNode.props.unshift({ name: 'default', jsonPath: prop.jsonPath });
      propsToRemove.push(conflictIdx);
    }
  }

  for (const idx of propsToRemove.sort((a, b) => b - a)) {
    node.props.splice(idx, 1);
  }

  for (const child of node.children.values()) {
    resolveClassTreeConflicts(child);
  }
}

/**
 * Render a @Stable class with mutableStateOf pattern for Compose theme colors.
 * Returns array of Kotlin source lines.
 */
function renderStableClass(className, node, indent = 0) {
  const pad = '    '.repeat(indent);
  const pad1 = '    '.repeat(indent + 1);
  const pad2 = '    '.repeat(indent + 2);
  const lines = [];

  lines.push(`${pad}@Stable`);
  lines.push(`${pad}class ${className} internal constructor(`);

  for (const { name } of node.props) {
    lines.push(`${pad1}${kotlinSafe(name)}: Color,`);
  }
  for (const [childName] of node.children) {
    lines.push(`${pad1}val ${childName}: ${capitalize(childName)},`);
  }

  lines.push(`${pad}) {`);

  // mutableStateOf delegates for leaf Color props
  if (node.props.length > 0) {
    for (const { name } of node.props) {
      const safe = kotlinSafe(name);
      lines.push(`${pad1}var ${safe} by mutableStateOf(${safe})`);
      lines.push(`${pad2}private set`);
    }
  }

  // Nested child classes
  for (const [childName, childNode] of node.children) {
    lines.push('');
    lines.push(...renderStableClass(capitalize(childName), childNode, indent + 1));
  }

  // update() function
  lines.push('');
  lines.push(`${pad1}fun update(other: ${className}) {`);
  for (const { name } of node.props) {
    const safe = kotlinSafe(name);
    lines.push(`${pad2}${safe} = other.${safe}`);
  }
  for (const [childName] of node.children) {
    lines.push(`${pad2}${childName}.update(other.${childName})`);
  }
  lines.push(`${pad1}}`);

  lines.push(`${pad}}`);

  return lines;
}

/**
 * Render the content lines of a factory constructor call (param assignments + child constructors).
 */
function renderFactoryContent(classPath, node, valueMap, indent) {
  const pad = '    '.repeat(indent);
  const lines = [];

  for (const { name, jsonPath } of node.props) {
    const value = valueMap.get(jsonPath);
    if (!value) console.warn(`  ⚠ No value for jsonPath "${jsonPath}" (property: ${name})`);
    lines.push(`${pad}${kotlinSafe(name)} = ${value || 'Color.Unspecified'},`);
  }

  for (const [childName, childNode] of node.children) {
    const childClassPath = `${classPath}.${capitalize(childName)}`;
    lines.push(`${pad}${childName} = ${childClassPath}(`);
    lines.push(...renderFactoryContent(childClassPath, childNode, valueMap, indent + 1));
    lines.push(`${pad}),`);
  }

  return lines;
}

const FILE_SUPPRESS = '@file:Suppress("all")';

// ── Custom formats ─────────────────────────────────────────────────────────────

/**
 * Kotlin format for palette tokens.
 * Generates nested objects: object Base { val black = ... }, object Neutral { val `5` = ... }, etc.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-palette',
  format: ({ dictionary }) => {
    const paletteTokens = dictionary.allTokens.filter(
      t => t.$type === 'color' && t.path[0] === 'palette',
    );

    const entries = paletteTokens.map(token => {
      // palette.base.black → ["Base", "black"]
      // palette.neutral.5  → ["Neutral", "`5`"]
      // palette.opaque.base-black.5 → ["Opaque", "BaseBlack", "`5`"]
      const segments = token.path.slice(1); // drop "palette"
      const path = segments.map((seg, i) => {
        if (i < segments.length - 1) {
          // intermediate segments → PascalCase object names
          return toPascalCase(seg);
        }
        // leaf segment → property name, backtick-wrap if starts with digit
        const camel = seg.replace(/-([a-z0-9])/g, (_, c) => c.toUpperCase());
        return kotlinSafe(camel);
      });

      const value = toComposeColor(token.$value, token.path.join('.'));
      return { path, value };
    });

    const tree = buildPropertyTree(entries);
    const body = renderTree(tree);

    return [
      FILE_SUPPRESS,
      '',
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.graphics.Color',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      'internal object TangemColorPalette {',
      body.join('\n'),
      '}',
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for TangemDimens3 — structured dimension tokens as @Immutable data class.
 * Generates nested @Immutable data classes from codeSyntax.Android paths (prefix: TangemTheme.dimens3.).
 * Includes spacing, size, borderRadius, borderWidth, blur, and semantic opacity tokens.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-dimens3',
  format: ({ dictionary }) => {
    const prefix = 'TangemTheme.dimens3.';

    const entries = [];
    for (const token of dictionary.allTokens) {
      const ext = token.$extensions?.['com.figma.codeSyntax'];
      if (!ext?.Android?.startsWith(prefix)) continue;

      const codePath = ext.Android.slice(prefix.length);
      const segPath = codePath.split('.');

      const raw = parseFloat(token.$value);
      const tp = token.path.join('.');
      if (isNaN(raw)) throw new Error(`Non-numeric value for ${tp}: "${token.$value}"`);

      // Opacity tokens → Float, all others → Dp
      const isOpacity = token.$type === 'opacity';
      const value = isOpacity ? `${raw}f` : `${raw}.dp`;
      const type = isOpacity ? 'Float' : 'Dp';

      entries.push({ path: segPath, value, type });
    }

    const tree = buildPropertyTree(entries.map(e => ({
      path: e.path,
      value: { default: e.value, type: e.type },
    })));

    const classLines = renderStableDimenClass('TangemDimens3', tree, 0);

    return [
      FILE_SUPPRESS,
      '',
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.runtime.Stable',
      'import androidx.compose.ui.unit.Dp',
      'import androidx.compose.ui.unit.dp',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      ...classLines,
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for TangemTypography3 — @Stable class with nested categories.
 * Generates a class taking FontFamily, with nested classes for each typography category
 * (display, heading, body, subheading, caption).
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-typography3',
  format: ({ dictionary }) => {
    const typoTokens = dictionary.allTokens.filter(
      t => t.$type === 'typography' && !isSourceOnlyToken(t),
    );

    // Group by category (path[1]: display, heading, body, subheading, caption)
    const categories = new Map();
    for (const token of typoTokens) {
      const category = token.path[1];
      if (!categories.has(category)) categories.set(category, []);
      categories.get(category).push(token);
    }

    // Build nested class lines
    const outerProps = [];
    const innerClasses = [];

    for (const [category, tokens] of categories) {
      const className = capitalize(category);
      const propName = category;
      const isHeading = category === 'display' || category === 'heading';

      outerProps.push(`    val ${propName}: ${className} = ${className}(fontFamily)`);

      const classLines = [`    @Stable`, `    class ${className} internal constructor(fontFamily: FontFamily) {`];

      for (const token of tokens) {
        const size = token.path[2]; // medium, small, etc.
        const v = token.$value;
        const tp = token.path.join('.');

        if (!v.fontWeight) throw new Error(`Missing fontWeight for ${tp}`);
        const fontWeight = mapFontWeight(v.fontWeight);
        const fontSize = parseFloat(v.fontSize);
        if (isNaN(fontSize)) throw new Error(`Non-numeric fontSize for ${tp}: "${v.fontSize}"`);
        const lineHeight = parseFloat(v.lineHeight);
        if (isNaN(lineHeight)) throw new Error(`Non-numeric lineHeight for ${tp}: "${v.lineHeight}"`);
        const letterSpacing = parseFloat(v.letterSpacing);
        if (isNaN(letterSpacing)) throw new Error(`Non-numeric letterSpacing for ${tp}: "${v.letterSpacing}"`);

        const spacingLiteral = letterSpacing < 0 ? `(${letterSpacing})` : `${letterSpacing}`;

        classLines.push(`        val ${size}: TextStyle = TextStyle(`);
        classLines.push(`            fontFamily = fontFamily,`);
        classLines.push(`            fontWeight = ${fontWeight},`);
        classLines.push(`            fontSize = ${fontSize}.sp,`);
        classLines.push(`            lineHeight = ${lineHeight}.sp,`);
        classLines.push(`            letterSpacing = ${spacingLiteral}.sp,`);
        classLines.push(`            lineHeightStyle = LineHeightStyle(`);
        classLines.push(`                alignment = LineHeightStyle.Alignment.Center,`);
        classLines.push(`                trim = LineHeightStyle.Trim.None,`);
        classLines.push(`            ),`);
        if (isHeading) {
          classLines.push(`            lineBreak = LineBreak.Heading,`);
        }
        classLines.push(`        )`);
      }

      classLines.push(`    }`);
      innerClasses.push(classLines.join('\n'));
    }

    return [
      FILE_SUPPRESS,
      '',
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.runtime.Stable',
      'import androidx.compose.ui.text.TextStyle',
      'import androidx.compose.ui.text.font.FontFamily',
      'import androidx.compose.ui.text.font.FontWeight',
      'import androidx.compose.ui.text.style.LineBreak',
      'import androidx.compose.ui.text.style.LineHeightStyle',
      'import androidx.compose.ui.unit.sp',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      '@Stable',
      'class TangemTypography3 internal constructor(fontFamily: FontFamily) {',
      outerProps.join('\n'),
      '',
      innerClasses.join('\n\n'),
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
 * Kotlin format for TangemColors3 class definition.
 * Generates the @Stable class hierarchy with mutableStateOf + update() pattern.
 * Structure is derived from light theme codeSyntax.Android fields.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-colors3-class',
  format: ({ dictionary }) => {
    const colorTokens = dictionary.allTokens.filter(
      t => t.$type === 'color' && !isSourceOnlyToken(t),
    );

    colors3StructureTree = buildClassStructureTree(colorTokens);
    resolveClassTreeConflicts(colors3StructureTree);

    const classLines = renderStableClass('TangemColors3', colors3StructureTree, 0);

    return [
      FILE_SUPPRESS,
      '',
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.runtime.Stable',
      'import androidx.compose.runtime.getValue',
      'import androidx.compose.runtime.mutableStateOf',
      'import androidx.compose.runtime.setValue',
      'import androidx.compose.ui.graphics.Color',
      '',
      '/**',
      ' * Auto-generated from design tokens. Do not edit manually.',
      ' */',
      ...classLines,
      '',
    ].join('\n');
  },
});

/**
 * Kotlin format for TangemColors3 light/dark factory functions.
 * Generates lightColors3() / darkColors3() functions referencing TangemColorPalette.
 */
StyleDictionary.registerFormat({
  name: 'kotlin/compose-colors3-factory',
  format: ({ dictionary, options }) => {
    const themeName = options.themeName;
    const funcName = `${themeName.toLowerCase()}Colors3`;

    // Ensure tree is built (should already be set by class format in Light build)
    if (!colors3StructureTree) {
      const colorTokens = dictionary.allTokens.filter(
        t => t.$type === 'color' && !isSourceOnlyToken(t),
      );
      colors3StructureTree = buildClassStructureTree(colorTokens);
      resolveClassTreeConflicts(colors3StructureTree);
    }

    // Build value map: jsonPath → Kotlin palette reference or Color literal
    const colorTokens = dictionary.allTokens.filter(
      t => t.$type === 'color' && !isSourceOnlyToken(t),
    );
    const valueMap = new Map();
    for (const token of colorTokens) {
      const jsonPath = token.path.join('.');
      valueMap.set(jsonPath, paletteRefOrColor(token));
    }

    const bodyLines = renderFactoryContent('TangemColors3', colors3StructureTree, valueMap, 2);

    return [
      FILE_SUPPRESS,
      '',
      `package ${PACKAGE}`,
      '',
      'import androidx.compose.ui.graphics.Color',
      '',
      '/**',
      ` * Auto-generated from design tokens. Do not edit manually.`,
      ` * Theme: ${themeName}`,
      ' */',
      `internal fun ${funcName}() =`,
      '    TangemColors3(',
      ...bodyLines,
      '    )',
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

  const colorFilter = token => token.$type === 'color' && !isSourceOnlyToken(token);

  const files = [];

  // Only light build generates the class definition (canonical codeSyntax structure)
  if (themeName === 'Light') {
    files.push({
      destination: 'TangemColors3.kt',
      format: 'kotlin/compose-colors3-class',
      filter: colorFilter,
    });
  }

  // Both themes generate factory functions
  files.push({
    destination: `TangemColors3${themeName}.kt`,
    format: 'kotlin/compose-colors3-factory',
    options: { themeName },
    filter: colorFilter,
  });

  const sd = new StyleDictionary({
    source: sets.map(s => path.join(tokensDir, `${s}.json`)),
    preprocessors: ['tokens-studio'],
    usesDtcg: true,
    log: { warnings: 'disabled', errors: { brokenReferences: 'console' } },
    platforms: {
      compose: {
        transforms: composePlatformTransforms,
        buildPath: outputDir + '/',
        files,
      },
    },
  });

  await sd.buildAllPlatforms();
  if (themeName === 'Light') console.log('  ✓ TangemColors3.kt');
  console.log(`  ✓ TangemColors3${themeName}.kt`);
}

// Build palette tokens
console.log('\nBuilding palette tokens...');

const paletteSd = new StyleDictionary({
  source: [...coreSets, 'semantic/size/opacity'].map(s => path.join(tokensDir, `${s}.json`)),
  preprocessors: ['tokens-studio'],
  usesDtcg: true,
  log: { warnings: 'disabled', errors: { brokenReferences: 'console' } },
  platforms: {
    compose: {
      transforms: composePlatformTransforms,
      buildPath: outputDir + '/',
      files: [
        {
          destination: 'TangemColorPalette.kt',
          format: 'kotlin/compose-palette',
          filter: token => token.$type === 'color' && token.path[0] === 'palette',
        },
      ],
    },
  },
});

await paletteSd.buildAllPlatforms();
console.log('  ✓ TangemColorPalette.kt');

// Build theme-independent tokens (dimensions, typography)
console.log('\nBuilding dimension and typography tokens...');

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
          destination: 'TangemDimens3.kt',
          format: 'kotlin/compose-dimens3',
        },
        {
          destination: 'TangemTypography3.kt',
          format: 'kotlin/compose-typography3',
          filter: token => token.$type === 'typography' && !isSourceOnlyToken(token),
        },
      ],
    },
  },
});

await sd.buildAllPlatforms();
console.log('  ✓ TangemDimens3.kt');
console.log('  ✓ TangemTypography3.kt');

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
  // Sort by relative path with forward slashes to match Gradle's invariantSeparatorsPath sorting
  files.sort((a, b) => {
    const ra = path.relative(tokensDir, a).split(path.sep).join('/');
    const rb = path.relative(tokensDir, b).split(path.sep).join('/');
    return ra.localeCompare(rb);
  });

  const hash = crypto.createHash('sha256');
  for (const file of files) {
    hash.update(path.relative(tokensDir, file).split(path.sep).join('/'));
    hash.update('\0');
    hash.update(fs.readFileSync(file));
    hash.update('\0');
  }
  return hash.digest('hex');
}

// ── Build icons ───────────────────────────────────────────────────────────────
// Run before writing .tokens-hash so the icons hash can be folded in — Gradle
// then has a single hash that invalidates on any ds-tokens change (tokens or icons).
const { hash: iconsHash } = await buildIcons();

const tokensInputHash = computeTokensHash();
const tokensHash = crypto
  .createHash('sha256')
  .update(tokensInputHash)
  .update('\0')
  .update(iconsHash)
  .digest('hex');
fs.writeFileSync(path.join(outputDir, '.tokens-hash'), tokensHash + '\n');
console.log(`\n  ✓ .tokens-hash (${tokensHash.substring(0, 12)}…)`);

console.log(`\nDone! Output: ${outputDir}`);
