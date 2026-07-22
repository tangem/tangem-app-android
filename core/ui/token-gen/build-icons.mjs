import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { stripCr, compareCodeUnits } from './hash-util.mjs';

// ── Paths ──────────────────────────────────────────────────────────────────────
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const dsTokensDir = path.join(__dirname, '..', 'ds-tokens');
const generatedDir = path.join(
  __dirname, '..', 'src', 'main', 'java', 'com', 'tangem', 'core', 'ui', 'res', 'generated',
);

// ── Build configs ────────────────────────────────────────────────────────────────
// Two families of SVG vectors are generated the same way, only differing by:
//  • source folder / output folder / package / namespace object
//  • tint handling: icons are single-color and tintable (the #0F0F0F placeholder is
//    rewritten to Color.Black so Icon(tint = …) can recolor them); illustrations
//    ("assets") keep their own colors verbatim, so they have no tint placeholder.

const ICONS_CONFIG = {
  label: 'icon',
  sourceDir: path.join(dsTokensDir, 'icons'),
  outputDir: path.join(generatedDir, 'icons'),
  packageName: 'com.tangem.core.ui.res.generated.icons',
  namespace: 'Icons',
  // Icons own the `Icons` namespace object, generated alongside them in this package.
  generateNamespaceObject: true,
  namespaceImport: null,
  hashFileName: '.icons-hash',
  namespaceDoc:
    'Auto-generated namespace for design-system icons.\n' +
    ' * Each icon is provided as an extension property on this object.',
  // Source SVGs use #0F0F0F as a "tint placeholder" — rewrite to Color.Black so
  // Icon(tint = …) at the call site can re-color the icon.
  tintPlaceholders: new Set(['#0f0f0f', '#0F0F0F']),
};

const ASSETS_CONFIG = {
  label: 'asset',
  sourceDir: path.join(dsTokensDir, 'assets'),
  outputDir: path.join(generatedDir, 'assets'),
  packageName: 'com.tangem.core.ui.res.generated.assets',
  // Illustrations are exposed as `Icons.<name>` extensions (same namespace as icons),
  // so they reuse the `Icons` object from the icons package — no separate object here.
  namespace: 'Icons',
  generateNamespaceObject: false,
  namespaceImport: 'com.tangem.core.ui.res.generated.icons.Icons',
  hashFileName: '.assets-hash',
  namespaceDoc: null,
  // Illustrations keep their own colors — no tint placeholder rewrite.
  tintPlaceholders: new Set(),
};

// ── Helpers ────────────────────────────────────────────────────────────────────

function* walkSvgs(dir) {
  if (!fs.existsSync(dir)) return;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) yield* walkSvgs(full);
    else if (entry.name.endsWith('.svg')) yield full;
  }
}

/** Find an attribute value in a snippet of XML. */
function attr(snippet, name) {
  const m = snippet.match(new RegExp(`\\b${name}="([^"]*)"`));
  return m ? m[1] : null;
}

function capitalize(s) {
  return s.charAt(0).toUpperCase() + s.slice(1);
}

/** Parse an SVG file into a normalized icon descriptor. */
function parseSvg(filePath) {
  const src = fs.readFileSync(filePath, 'utf8');

  const svgOpen = src.match(/<svg\b[^>]*>/);
  if (!svgOpen) throw new Error('No <svg> root element');
  const svgEl = svgOpen[0];

  // Viewport / default size
  const viewBox = attr(svgEl, 'viewBox');
  let viewportW, viewportH;
  if (viewBox) {
    const parts = viewBox.split(/\s+/).map(Number);
    viewportW = parts[2];
    viewportH = parts[3];
  }
  const defaultW = parseFloat(attr(svgEl, 'width')) || viewportW;
  const defaultH = parseFloat(attr(svgEl, 'height')) || viewportH;
  viewportW = viewportW ?? defaultW;
  viewportH = viewportH ?? defaultH;

  if (!viewportW || !viewportH) {
    throw new Error('Missing viewBox/width/height');
  }

  // Group transforms aren't supported (would need matrix decomposition).
  if (/<g\b[^>]*\btransform=/.test(src)) {
    throw new Error('<g transform="…"> is not supported by the current generator');
  }

  // <path .../> elements
  const paths = [];
  const pathRe = /<path\b([^>]*?)\/?>/g;
  let m;
  while ((m = pathRe.exec(src)) !== null) {
    const a = m[1];
    paths.push({
      d: attr(a, 'd'),
      fill: attr(a, 'fill'),
      fillRule: attr(a, 'fill-rule'),
      fillOpacity: attr(a, 'fill-opacity'),
      stroke: attr(a, 'stroke'),
      strokeWidth: attr(a, 'stroke-width'),
      strokeLinecap: attr(a, 'stroke-linecap'),
      strokeLinejoin: attr(a, 'stroke-linejoin'),
      opacity: attr(a, 'opacity'),
    });
  }

  if (paths.length === 0) throw new Error('No <path> elements found');
  for (const p of paths) {
    if (!p.d) throw new Error('A <path> is missing the "d" attribute');
  }

  return { viewportW, viewportH, defaultW, defaultH, paths };
}

/**
 * ic_arrow_down_24_regular.svg →
 *   { propName: 'ic_arrow_down_24', fileName: 'IcArrowDown24' }
 * il_token_custom.svg →
 *   { propName: 'il_token_custom', fileName: 'IlTokenCustom' }
 */
function deriveNames(svgFile) {
  const base = path.basename(svgFile, '.svg').replace(/_regular$/, '');
  const fileName = base
    .split('_')
    .map(part => capitalize(part))
    .join('');
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(base)) {
    throw new Error(`Vector name "${base}" is not a valid Kotlin identifier`);
  }
  return { propName: base, fileName };
}

/** Convert an SVG color string into a Compose Color expression, or null to skip. */
function svgColorToKotlin(value, tintPlaceholders) {
  if (!value || value === 'none') return null;
  if (tintPlaceholders.has(value.toLowerCase())) return 'Color.Black';

  const hex6 = value.match(/^#([0-9a-fA-F]{6})$/);
  if (hex6) return `Color(0xFF${hex6[1].toUpperCase()})`;

  const hex3 = value.match(/^#([0-9a-fA-F]{3})$/);
  if (hex3) {
    const [r, g, b] = hex3[1].toUpperCase().split('');
    return `Color(0xFF${r}${r}${g}${g}${b}${b})`;
  }

  const hex8 = value.match(/^#([0-9a-fA-F]{8})$/);
  if (hex8) return `Color(0x${hex8[1].toUpperCase()})`;

  const rgba = value.match(/^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([\d.]+)\s*\)$/);
  if (rgba) {
    const r = (+rgba[1]).toString(16).padStart(2, '0').toUpperCase();
    const g = (+rgba[2]).toString(16).padStart(2, '0').toUpperCase();
    const b = (+rgba[3]).toString(16).padStart(2, '0').toUpperCase();
    const a = Math.round(parseFloat(rgba[4]) * 255).toString(16).padStart(2, '0').toUpperCase();
    return `Color(0x${a}${r}${g}${b})`;
  }

  if (value === 'black') return 'Color.Black';
  if (value === 'white') return 'Color.White';
  if (value === 'transparent') return 'Color.Transparent';

  throw new Error(`Unsupported SVG color: "${value}"`);
}

function renderPath(p, indent, config) {
  const pad = '    '.repeat(indent);
  const pad1 = '    '.repeat(indent + 1);
  const args = [];

  // If a path has no fill at all and has stroke, leave fill out. Otherwise default to tintable black.
  const hasStroke = !!p.stroke && p.stroke !== 'none';
  const fillSpecified = p.fill != null;
  let fillKt = svgColorToKotlin(p.fill, config.tintPlaceholders);
  if (!fillSpecified && !hasStroke) fillKt = 'Color.Black';
  if (fillKt) args.push(`fill = SolidColor(${fillKt})`);

  if (p.fillOpacity != null) {
    args.push(`fillAlpha = ${parseFloat(p.fillOpacity)}f`);
  } else if (p.opacity != null && fillKt) {
    args.push(`fillAlpha = ${parseFloat(p.opacity)}f`);
  }

  const strokeKt = svgColorToKotlin(p.stroke, config.tintPlaceholders);
  if (strokeKt) args.push(`stroke = SolidColor(${strokeKt})`);
  if (p.strokeWidth != null) args.push(`strokeLineWidth = ${parseFloat(p.strokeWidth)}f`);
  if (p.strokeLinecap) args.push(`strokeLineCap = StrokeCap.${capitalize(p.strokeLinecap)}`);
  if (p.strokeLinejoin) args.push(`strokeLineJoin = StrokeJoin.${capitalize(p.strokeLinejoin)}`);

  args.push(`pathFillType = PathFillType.${p.fillRule === 'evenodd' ? 'EvenOdd' : 'NonZero'}`);
  args.push(`pathData = addPathNodes(${JSON.stringify(p.d)})`);

  const lines = [`${pad}addPath(`];
  for (const arg of args) lines.push(`${pad1}${arg},`);
  lines.push(`${pad})`);
  return lines.join('\n');
}

function renderIconFile({ propName, fileName }, icon, config) {
  const usesStroke = icon.paths.some(p => p.stroke && p.stroke !== 'none');

  const imports = [
    'androidx.compose.material3.Icon',
    'androidx.compose.runtime.Composable',
    'androidx.compose.ui.graphics.Color',
    'androidx.compose.ui.graphics.PathFillType',
    'androidx.compose.ui.graphics.SolidColor',
    'androidx.compose.ui.graphics.vector.ImageVector',
    'androidx.compose.ui.graphics.vector.addPathNodes',
    'androidx.compose.ui.tooling.preview.Preview',
    'androidx.compose.ui.unit.dp',
  ];
  if (usesStroke) {
    imports.push('androidx.compose.ui.graphics.StrokeCap');
    imports.push('androidx.compose.ui.graphics.StrokeJoin');
  }
  // When the namespace object lives in another package, import it.
  if (config.namespaceImport) imports.push(config.namespaceImport);
  imports.sort();

  const pathBlocks = icon.paths.map(p => renderPath(p, 2, config)).join('\n');

  return `@file:Suppress("all")

package ${config.packageName}

${imports.map(i => `import ${i}`).join('\n')}

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _${propName}: ImageVector? = null

val ${config.namespace}.${propName}: ImageVector
    get() {
        if (_${propName} != null) return _${propName}!!
        _${propName} = ImageVector.Builder(
            name = ${JSON.stringify(propName)},
            defaultWidth = ${icon.defaultW}.dp,
            defaultHeight = ${icon.defaultH}.dp,
            viewportWidth = ${icon.viewportW}f,
            viewportHeight = ${icon.viewportH}f,
        ).apply {
${pathBlocks.replace(/^/gm, '    ')}
        }.build()
        return _${propName}!!
    }

@Composable
@Preview(showBackground = true)
private fun ${fileName}Preview() {
    Icon(
        imageVector = ${config.namespace}.${propName},
        contentDescription = null,
    )
}
`;
}

function renderNamespaceFile(config) {
  return `@file:Suppress("all")

package ${config.packageName}

/**
 * ${config.namespaceDoc}
 */
object ${config.namespace}
`;
}

// ── Hash gate ──────────────────────────────────────────────────────────────────

function computeSourceHash(config) {
  const files = [...walkSvgs(config.sourceDir)];
  // Code-unit order (not localeCompare) to match the Kotlin verifier's invariantSeparatorsPath
  // sort deterministically across locales/ICU versions — see hash-util.mjs.
  files.sort((a, b) => compareCodeUnits(
    path.relative(config.sourceDir, a).split(path.sep).join('/'),
    path.relative(config.sourceDir, b).split(path.sep).join('/'),
  ));
  const hash = crypto.createHash('sha256');
  for (const file of files) {
    hash.update(path.relative(config.sourceDir, file).split(path.sep).join('/'));
    hash.update('\0');
    hash.update(stripCr(fs.readFileSync(file)));
    hash.update('\0');
  }
  return hash.digest('hex');
}

// ── Generic build ────────────────────────────────────────────────────────────────

function buildVectors(config) {
  console.log(`\nBuilding ${config.label} vectors...`);

  const newHash = computeSourceHash(config);
  const hashFile = path.join(config.outputDir, config.hashFileName);
  if (fs.existsSync(hashFile)) {
    const prev = fs.readFileSync(hashFile, 'utf8').trim();
    if (prev === newHash) {
      console.log(`  ✓ ${config.label}s unchanged (${newHash.substring(0, 12)}…); skipping`);
      return { hash: newHash };
    }
  }

  fs.mkdirSync(config.outputDir, { recursive: true });

  // Parse every SVG up-front so we fail fast on errors before writing anything.
  const icons = [];
  for (const svgFile of walkSvgs(config.sourceDir)) {
    const names = deriveNames(svgFile);
    let parsed;
    try {
      parsed = parseSvg(svgFile);
    } catch (e) {
      throw new Error(`${path.relative(config.sourceDir, svgFile)}: ${e.message}`);
    }
    icons.push({ names, parsed });
  }

  // Detect property-name collisions early.
  const seen = new Map();
  for (const { names } of icons) {
    if (seen.has(names.propName)) {
      throw new Error(
        `Duplicate ${config.label} property "${names.propName}" (file collision: ` +
        `${seen.get(names.propName)}.kt vs ${names.fileName}.kt)`,
      );
    }
    seen.set(names.propName, names.fileName);
  }

  // Write namespace (only if this family owns the object) + per-vector files.
  const expectedFiles = new Set([config.hashFileName]);
  if (config.generateNamespaceObject) {
    const namespaceFile = `${config.namespace}.kt`;
    expectedFiles.add(namespaceFile);
    fs.writeFileSync(path.join(config.outputDir, namespaceFile), renderNamespaceFile(config));
  }

  for (const { names, parsed } of icons) {
    const file = `${names.fileName}.kt`;
    expectedFiles.add(file);
    fs.writeFileSync(path.join(config.outputDir, file), renderIconFile(names, parsed, config));
  }

  // Cleanup stale generated files (vectors that no longer have a source SVG).
  let removed = 0;
  for (const entry of fs.readdirSync(config.outputDir)) {
    if (!expectedFiles.has(entry) && entry.endsWith('.kt')) {
      fs.unlinkSync(path.join(config.outputDir, entry));
      removed++;
    }
  }

  fs.writeFileSync(hashFile, newHash + '\n');
  const removedNote = removed > 0 ? `, removed ${removed} stale` : '';
  console.log(`  ✓ ${icons.length} ${config.label}(s) (${newHash.substring(0, 12)}…${removedNote})`);
  return { hash: newHash };
}

// ── Public API ───────────────────────────────────────────────────────────────────

export async function buildIcons() {
  return buildVectors(ICONS_CONFIG);
}

export async function buildAssets() {
  return buildVectors(ASSETS_CONFIG);
}

// Run directly when executed as `node build-icons.mjs`.
if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  await buildIcons();
  await buildAssets();
}
