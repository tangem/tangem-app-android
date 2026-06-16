---
description: Design-system generations (DS1/DS2/DS3), component pattern, KDoc & API conventions, storybook
paths:
  - "core/ui/src/main/java/com/tangem/core/ui/ds2/**"
  - "core/ui/src/main/java/com/tangem/core/ui/ds/**"
  - "core/ui/src/main/java/com/tangem/core/ui/components/**"
  - "features/tester/**"
---

# Design System

The app currently hosts **three generations of the design system (DS)** side by side. They differ by
folder, token set (colors / typography / dimensions), and the `@Preview` wrapper. Knowing which
generation a component belongs to is essential so you don't mix tokens or pull the wrong building blocks.

## Three generations

| Generation | Folder | Colors | Typography | Dimensions | Preview wrapper |
|---|---|---|---|---|---|
| **DS1** (legacy) | `core/ui/src/main/java/com/tangem/core/ui/components/` | `TangemTheme.colors` | `TangemTheme.typography` | `TangemTheme.dimens` | `TangemThemePreview` |
| **DS2** (redesign) | `core/ui/src/main/java/com/tangem/core/ui/ds/` | `TangemTheme.colors2` | `TangemTheme.typography2` | `TangemTheme.dimens2` | `TangemThemePreviewRedesign` |
| **DS3** (target) | `core/ui/src/main/java/com/tangem/core/ui/ds2/` | `TangemTheme.colors3` | `TangemTheme.typography3` | `TangemTheme.dimens2` | `TangemThemePreviewRedesign` |

> Mind the numbering mismatch: **folder `ds` is DS2**, **folder `ds2` is DS3**.
> The `colors2` / `typography2` tokens are `@Deprecated` (ReplaceWith `colors3` / `typography3`).

- **DS1** — the entire current app is built on it. Do **not** add new components here.
- **DS2** — redesign components. A transitional generation; don't write new components in it, only
  maintain what already exists.
- **DS3** — the newest design system; **the whole app is being migrated to it**. Build new DS
  components here.

## Using DS3 in features

**All DS3 components (folder `ds2`) may be used in features starting from app version 6.0.** Before
6.0 they must not be used on product screens.

If a needed component does not yet exist in DS3, **add it by analogy with the existing ones** (see the
pattern below).

## DS3 component pattern

Study the existing components as references:
- Simple: `ds2/checkbox/TangemCheckmark.kt` — single file, a public `@Composable` function + `@Preview`.
- Composite: `ds2/button/` — `TangemButton.kt` (public API), `TangemButtonInternal.kt` (private inner
  layout), `TangemButtonExt.kt` (variant / size tokens).

Pattern rules:

1. **Package & location.** `com.tangem.core.ui.ds2.<component>`, folder
   `core/ui/.../ds2/<component>/`. The component name is `Tangem<Name>`.
2. **DS3 tokens only.** Colors — `TangemTheme.colors3.*`, text — `TangemTheme.typography3.*`,
   dimensions — `TangemTheme.dimens2.*`. No `colors` / `colors2` / hardcoded values (literal dp/colors
   are acceptable only inside `@Preview`, where you add `@Suppress("MagicNumber")`).
3. **Signature.** `modifier: Modifier = Modifier` is mandatory (defaulting to `Modifier`, placed first
   among the optional params or right after the required ones). Express variants/sizes via a nested
   `enum` in `object Tangem<Name>` (like `TangemButton.Variant` / `TangemButton.Size`), not boolean flags.
4. **Accessibility.** Pass `contentDescription`, set the `Role`, mark `disabled()` in `semantics`, and
   handle focus/press state via `interactionSource`.
5. **KDoc + Figma link.** Above the public function — KDoc describing behavior, every parameter, and a
   link to the Figma node (see the KDoc requirements below).
6. **Previews.** Two `@Preview`s (Light + Dark via `UI_MODE_NIGHT_YES`), wrapped in
   `TangemThemePreviewRedesign { ... }`, with `TangemTheme.colors3.bg.primary` as the background.
   Preview helpers (`PreviewRow`, `Section`, etc.) are private in the same file.
7. **Composite components** (many variants / heavy layout) are split into 3 files like the button:
   public `Tangem<Name>.kt`, private `Tangem<Name>Internal.kt`, tokens `Tangem<Name>Ext.kt`.

## API conventions

### Public properties live in the `object`

Any public type the component exposes — variant/size/role/align enums, status classes, constants —
is declared inside the namesake `object Tangem<Name>`, **not** as a top-level type. This keeps a single
`Tangem<Name>.Variant` / `Tangem<Name>.Size` / `Tangem<Name>.Role` namespace at the call site and
avoids polluting the package.

```kotlin
object TangemTopNavigation {
    /** Horizontal alignment of the center content slot. */
    enum class ContentAlign { Start, Center }
}
// usage: TangemTopNavigation.ContentAlign.Center
```

References: `TangemTopNavigation.ContentAlign`, `TangemNavigationText.Role`, `TangemButton.Variant` /
`TangemButton.Size`.

### Provide convenient overloads

A component should ship ergonomic overloads so callers don't assemble boilerplate for the common case.
Two acceptable shapes:

1. **Additional `@Composable fun` overloads** with simpler parameters that delegate to the base one.
   `TangemTopNavigation` has a low-level slot-based overload (`startButton`/`endButton`/`contentColumn`
   lambdas) plus several high-level overloads taking `title` / `subtitle` / `onBack` / `onClose` that
   wire the predefined buttons and the title/subtitle center for you.
2. **Extension functions on the `object`** for named presets — e.g. `@Composable fun TangemButton.Back(…)`
   and `TangemButton.Close(…)` in `TangemButtonExt.kt` expose ready-made button presets while reading
   as `TangemButton.Back { … }` at the call site.

Each overload keeps the same rules as the base component (`modifier` first among optionals, DS3 tokens,
its own KDoc — see below).

### Sub-components are first-class

Internal building blocks that are themselves public (e.g. `TangemNavigationText`, used for the
`TangemTopNavigation` title/subtitle slots) follow the **exact same rules** as a top-level component:
DS3 tokens only, `modifier: Modifier = Modifier`, public properties in their own `object`
(`TangemNavigationText.Role`), full KDoc, and their own Storybook entry where it makes sense. Don't
treat "helper" composables as second-class — if a feature can call it, it is a documented DS component.

## KDoc requirements for components

Every public DS component (and any non-trivial public composable) must carry a KDoc block. Use
`ds2/button/TangemButton.kt` and `ds2/checkbox/TangemCheckmark.kt` as the canonical examples.

A component KDoc must contain, in order:

1. **Summary line.** One sentence stating what the component is and which generation it belongs to —
   start with `Design-system v2 …` for DS3 components (matches the existing wording).
2. **Figma link.** A markdown link to the exact Figma node:
   `[Figma](https://www.figma.com/design/…?node-id=…)`. A component without a Figma reference is not
   review-ready.
3. **Behavior notes** (when behavior is non-obvious). A short prose paragraph or a bulleted
   `Behavior notes:` list covering state-dependent rendering — loading, disabled/enabled, icon-only
   vs. labeled, focus ring, animations, what overrides what. Describe *observable behavior*, not the
   implementation.
4. **`@param` for every parameter.** No parameter may be left undocumented — including `modifier`
   when its effect is non-trivial (e.g. "Pass `Modifier.fillMaxWidth()` to switch to fixed-width
   layout"). Each `@param` states the meaning **and** the consequences of notable values
   (`null` → non-interactive, `false` → dimmed & clicks ignored, etc.).
5. **Accessibility guidance** where relevant — e.g. when `contentDescription` should be supplied
   (icon-only buttons, loading state, disabled state) and what it announces.

Additional rules:

- Document the **nested `enum`s** (`Variant`, `Size`, `Status`, …) too: a short KDoc on the enum and,
  where the options aren't self-explanatory, a one-line description per entry (see `TangemButton.Variant`).
- Keep KDoc about **contract and behavior**, not internals. Implementation comments explaining *why*
  a specific approach was taken belong to inline `//` comments inside the body, not the KDoc.
- Reference other DS types with `[TangemSurface]` / `[TangemButton.Variant]` link syntax so they
  resolve in the IDE.
- Detekt enforces missing-KDoc-on-public-API style checks on `core:ui`; run `./gradlew :core:ui:detektMain`.

## Storybook

Add every DS3 component to the **Storybook** (module `features/tester`) — a live on-device/emulator
component gallery (Tester → Storybook → DS Components).

Use the **`add-storybook-component`** skill — it wires the entity, the Build factory, the Composable
page, and registers it in the correct list. Run: `/add-storybook-component TangemCheckmark (DS)`.
Page layout guidelines live in
`features/tester/impl/src/main/java/com/tangem/feature/tester/presentation/storybook/STORYBOOK.md`.

## Checklist: adding a new DS3 component

- [ ] Component created under `core/ui/.../ds2/<component>/`, package `com.tangem.core.ui.ds2.<component>`.
- [ ] Named `Tangem<Name>`; first optional parameter is `modifier: Modifier = Modifier`.
- [ ] Uses **only** DS3 tokens: `colors3`, `typography3`, `dimens2`. No hardcoded values outside previews.
- [ ] Variants/sizes expressed as an `enum` inside `object Tangem<Name>` (not a set of boolean flags).
- [ ] All public types (enums, statuses, constants) declared inside the `object Tangem<Name>`.
- [ ] Convenient overloads provided (simpler `@Composable` overloads and/or `object` extension presets).
- [ ] Public sub-components (e.g. `TangemNavigationText`) follow the same rules + KDoc as a full component.
- [ ] States handled: enabled/disabled, press/focus (`interactionSource`), loading (if applicable).
- [ ] Accessibility: `contentDescription`, `Role`, `disabled()` in `semantics`.
- [ ] KDoc per the requirements above (summary + Figma link + behavior notes + every `@param` + a11y).
- [ ] Two `@Preview`s (Light/Dark) in `TangemThemePreviewRedesign`, background `colors3.bg.primary`.
- [ ] Heavy component split into `Tangem<Name>.kt` / `…Internal.kt` / `…Ext.kt`.
- [ ] Storybook page added (`add-storybook-component` skill).
- [ ] Detekt passes: `./gradlew :core:ui:detektMain` (plus
      `./gradlew :features:tester:impl:assembleGoogleDebug` if you touched the Storybook).
- [ ] Use in product features only from app version **6.0** onward.