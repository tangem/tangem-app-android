import crypto from 'crypto';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// ── Paths ──────────────────────────────────────────────────────────────────────
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const iconsDir = path.join(__dirname, '..', 'ds-tokens', 'icons');
const outputDir = path.join(
  __dirname, '..', 'src', 'main', 'java', 'com', 'tangem', 'core', 'ui',
  'res', 'generated', 'icons',
);

const PACKAGE = 'com.tangem.core.ui.res.generated.icons';

// Source SVGs use #0F0F0F as a "tint placeholder" — rewrite to Color.Black so
// Icon(tint = …) at the call site can re-color the icon.
const TINT_PLACEHOLDERS = new Set(['#0f0f0f', '#0F0F0F']);

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
 */
function deriveNames(svgFile) {
  const base = path.basename(svgFile, '.svg').replace(/_regular$/, '');
  const fileName = base
    .split('_')
    .map(part => capitalize(part))
    .join('');
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(base)) {
    throw new Error(`Icon name "${base}" is not a valid Kotlin identifier`);
  }
  return { propName: base, fileName };
}

/** Convert an SVG color string into a Compose Color expression, or null to skip. */
function svgColorToKotlin(value) {
  if (!value || value === 'none') return null;
  if (TINT_PLACEHOLDERS.has(value.toLowerCase())) return 'Color.Black';

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

function renderPath(p, indent) {
  const pad = '    '.repeat(indent);
  const pad1 = '    '.repeat(indent + 1);
  const args = [];

  // If a path has no fill at all and has stroke, leave fill out. Otherwise default to tintable black.
  const hasStroke = !!p.stroke && p.stroke !== 'none';
  const fillSpecified = p.fill != null;
  let fillKt = svgColorToKotlin(p.fill);
  if (!fillSpecified && !hasStroke) fillKt = 'Color.Black';
  if (fillKt) args.push(`fill = SolidColor(${fillKt})`);

  if (p.fillOpacity != null) {
    args.push(`fillAlpha = ${parseFloat(p.fillOpacity)}f`);
  } else if (p.opacity != null && fillKt) {
    args.push(`fillAlpha = ${parseFloat(p.opacity)}f`);
  }

  const strokeKt = svgColorToKotlin(p.stroke);
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

function renderIconFile({ propName, fileName }, icon) {
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
  imports.sort();

  const pathBlocks = icon.paths.map(p => renderPath(p, 2)).join('\n');

  return `@file:Suppress("all")

package ${PACKAGE}

${imports.map(i => `import ${i}`).join('\n')}

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _${propName}: ImageVector? = null

val Icons.${propName}: ImageVector
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
        imageVector = Icons.${propName},
        contentDescription = null,
    )
}
`;
}

const ICONS_NAMESPACE = `@file:Suppress("all")

package ${PACKAGE}

/**
 * Auto-generated namespace for design-system icons.
 * Each icon is provided as an extension property on this object.
 */
object Icons
`;

// ── Hash gate ──────────────────────────────────────────────────────────────────

function computeIconsHash() {
  const files = [...walkSvgs(iconsDir)];
  files.sort((a, b) => {
    const ra = path.relative(iconsDir, a).split(path.sep).join('/');
    const rb = path.relative(iconsDir, b).split(path.sep).join('/');
    return ra.localeCompare(rb);
  });
  const hash = crypto.createHash('sha256');
  for (const file of files) {
    hash.update(path.relative(iconsDir, file).split(path.sep).join('/'));
    hash.update('\0');
    hash.update(fs.readFileSync(file));
    hash.update('\0');
  }
  return hash.digest('hex');
}

// ── Main ───────────────────────────────────────────────────────────────────────

export async function buildIcons() {
  console.log('\nBuilding icon vectors...');

  const newHash = computeIconsHash();
  const hashFile = path.join(outputDir, '.icons-hash');
  if (fs.existsSync(hashFile)) {
    const prev = fs.readFileSync(hashFile, 'utf8').trim();
    if (prev === newHash) {
      console.log(`  ✓ icons unchanged (${newHash.substring(0, 12)}…); skipping`);
      return { hash: newHash };
    }
  }

  fs.mkdirSync(outputDir, { recursive: true });

  // Parse every SVG up-front so we fail fast on errors before writing anything.
  const icons = [];
  for (const svgFile of walkSvgs(iconsDir)) {
    const names = deriveNames(svgFile);
    let parsed;
    try {
      parsed = parseSvg(svgFile);
    } catch (e) {
      throw new Error(`${path.relative(iconsDir, svgFile)}: ${e.message}`);
    }
    icons.push({ names, parsed });
  }

  // Detect property-name collisions early.
  const seen = new Map();
  for (const { names } of icons) {
    if (seen.has(names.propName)) {
      throw new Error(
        `Duplicate icon property "${names.propName}" (file collision: ` +
        `${seen.get(names.propName)}.kt vs ${names.fileName}.kt)`,
      );
    }
    seen.set(names.propName, names.fileName);
  }

  // Write namespace + per-icon files.
  const expectedFiles = new Set(['Icons.kt', '.icons-hash']);
  fs.writeFileSync(path.join(outputDir, 'Icons.kt'), ICONS_NAMESPACE);

  for (const { names, parsed } of icons) {
    const file = `${names.fileName}.kt`;
    expectedFiles.add(file);
    fs.writeFileSync(path.join(outputDir, file), renderIconFile(names, parsed));
  }

  // Cleanup stale generated files (icons that no longer have a source SVG).
  let removed = 0;
  for (const entry of fs.readdirSync(outputDir)) {
    if (!expectedFiles.has(entry) && entry.endsWith('.kt')) {
      fs.unlinkSync(path.join(outputDir, entry));
      removed++;
    }
  }

  fs.writeFileSync(hashFile, newHash + '\n');
  const removedNote = removed > 0 ? `, removed ${removed} stale` : '';
  console.log(`  ✓ ${icons.length} icon(s) (${newHash.substring(0, 12)}…${removedNote})`);
  return { hash: newHash };
}

// Run directly when executed as `node build-icons.mjs`.
if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  await buildIcons();
}
