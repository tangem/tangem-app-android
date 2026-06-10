---
name: add-storybook-component
description: Add a component showcase page to the Tangem storybook (in features/tester). Wires the entity, Build factory, Composable page, and registers it either in the "DS Components" sub-list (first/default target — for design-system components under core.ui.ds2.*) or in the root storybook list (second target — for any other component). Use when asked to "add a storybook page/story", "add <Component> to the storybook", "сделай сторибук для <компонент>", "добавь стори/историю в storybook", or to showcase a DS component in the tester.
allowed-tools: Read, Grep, Glob, Bash, Edit, Write
argument-hint: [component to add, e.g. "TangemCheckbox (DS)" or "MyLegacyCard"]
---

Add a new component page to the Tangem storybook. The storybook lives in
`features/tester/impl/src/main/java/com/tangem/feature/tester/presentation/storybook/`
and renders interactive DS/component showcases on a device or emulator.

This is an **interactive** skill: read the real production component first to get its actual
parameters, enums, and package — never guess the API. Then mirror the closest existing story.

## Two placement targets — pick one

| Target | Use for | List screen | Page dir | Entity supertype |
|---|---|---|---|---|
| **1. DS Components (default)** | Design-system components under `com.tangem.core.ui.ds2.*` (the newest "DS3"/redesign components: `TangemButton`, `TangemBadge`, `TangemRow`, `TangemLoader`, …) | `page/ds/DsComponentsListScreen.kt` → `buildDsStories()` | `page/ds/<component>/` | `DsStoryBookPage` |
| **2. Other components** | Anything else (legacy/cross-cutting components, backgrounds, effects, typography demos) | `ui/StoryBookListScreen.kt` → `buildStories()` | `page/<component>/` | `StoryBookPage` |

**Default to Target 1 (DS Components)** when the component lives under `core.ui.ds2.*` or the user
mentions "DS"/"ds3"/"design system". Only the **list screen** and **page directory** differ between
the two targets — everything else (entity declaration file, `StoryBookScreen.kt` routing, factory
pattern) is identical.

> The ONLY behavioral difference of `DsStoryBookPage` vs `StoryBookPage`: `StoryBookViewModel.onBackClick`
> routes a `DsStoryBookPage` back to the DS sub-list, while a plain `StoryBookPage` routes back to the
> root list. That's it.

## Reference

`features/tester/impl/src/main/java/com/tangem/feature/tester/presentation/storybook/STORYBOOK.md` is the canonical doc — read it for the **design guidelines** (mandatory
page layout: single live preview pinned at top + one control per parameter below, chip-selector pattern,
colors, realistic text). This skill covers the *wiring*; STORYBOOK.md covers the *look*.

Best reference implementations to mirror:
- **Stateful DS page with many controls:** `page/ds/button/` (TangemButton — variant/size/background
  selectors, toggles, text-scale slider, blur backdrop). Read all three files: `Build.kt`,
  `TangemButtonStory.kt`, and the `TangemButtonStory` entity in `entity/StoryBookPage.kt`.
- **Simple stateful page:** `page/ds/loader/` (TangemLoader — single size selector).
- **Stateless page (no params):** a `data object` sibling such as `ButtonsStory`.

## Workflow

1. **Read the production component.** Grep `core/ui/src/main/java/com/tangem/core/ui/ds2/<name>/`
   (or wherever it lives) for the composable signature, its `enum`s (Variant/Size/Status/…), and
   required vs optional params. The set of parameters becomes the set of controls.
2. **Decide stateless vs stateful:**
   - **Stateless** (`data object`) — ONLY if the component has no configurable parameters at all.
   - **Stateful** (`data class`) — the normal case: one field per parameter the user can change, each
     paired with an `onXxxChange`/`onXxxToggle` lambda.
3. **Pick the target** (see table above) and **mirror the closest sibling**.
4. **Do the 4 edits + 1 new dir** (Steps A–E below).
5. **Verify it compiles** (see Build).

## The edits

Assume component `Foo` rendered by `com.tangem.core.ui.ds2.foo.TangemFoo` with a `Variant` enum and an
`isEnabled` flag. Adjust names to the real component. `<page-dir>` =
`page/ds/foo/` for Target 1, or `page/foo/` for Target 2.

### A. Declare the entity in `entity/StoryBookPage.kt`

Stateful (normal):
```kotlin
internal data class TangemFooStory(
    val variant: TangemFoo.Variant,
    val isEnabled: Boolean,
    val onVariantChange: (TangemFoo.Variant) -> Unit,
    val onEnabledToggle: () -> Unit,
) : DsStoryBookPage   // <- StoryBookPage for Target 2
```
Stateless: `internal data object TangemFooStory : DsStoryBookPage` (or `StoryBookPage`).

Add the matching import for the production type at the top of the file.

### B. Create `<page-dir>/Build.kt`

Stateful — uses `storyPageFactory` + `StateUpdater`:
```kotlin
internal fun StateUpdater<TangemFooStory>.build(): TangemFooStory {
    return TangemFooStory(
        variant = TangemFoo.Variant.Primary,
        isEnabled = true,
        onVariantChange = { v -> updateStory { it.copy(variant = v) } },
        onEnabledToggle = { updateStory { it.copy(isEnabled = !it.isEnabled) } },
    )
}

internal val tangemFooStoryFactory
    get() = storyPageFactory(StateUpdater<TangemFooStory>::build)
```
Stateless: `internal val tangemFooStoryFactory: StoryPageFactory = StoryPageFactory { TangemFooStory }`

### C. Create `<page-dir>/TangemFooStory.kt`

`@Composable internal fun TangemFooStory(state: TangemFooStory, modifier: Modifier = Modifier)`
(drop `state` for stateless). Follow STORYBOOK.md design guidelines: live preview pinned at the top
in a `Column`, controls scrolling below. Reuse the chip-selector / toggle-row patterns from
`page/ds/button/TangemButtonStory.kt` (its `Section`, `ChipGrid`, `Chip`, `ToggleRow` are private —
copy the ones you need into the new file). Use representative text, not "Btn".

### D. Register routing in `ui/StoryBookScreen.kt`

Add both imports (entity + page composable share the simple name — Kotlin resolves them by position):
```kotlin
import com.tangem.feature.tester.presentation.storybook.entity.TangemFooStory
import com.tangem.feature.tester.presentation.storybook.page.ds.foo.TangemFooStory
```
Add a branch to the `when (storyState)`:
```kotlin
is TangemFooStory -> TangemFooStory(state = storyState)   // stateless: TangemFooStory -> TangemFooStory()
```

### E. Register in the list screen (target-specific)

- **Target 1 (DS):** in `page/ds/DsComponentsListScreen.kt` add the factory import and a row to
  `buildDsStories()`:
  ```kotlin
  DsStoryItem(title = "🔘 TangemFoo", factory = tangemFooStoryFactory),
  ```
- **Target 2 (other):** in `ui/StoryBookListScreen.kt` add the factory import and a row to
  `buildStories()`:
  ```kotlin
  StoryItem(title = "🔘 Foo", factory = tangemFooStoryFactory),
  ```

**Every title must start with an emoji** matching the component category (🔘 buttons, 🏷️ badge,
📋 row, ⏳ loader, 🔤 typography, 🔍 search, 🧭 navigation, 💀 placeholder, ✨ effects, 🪙 token…).

## Build

```bash
./gradlew :features:tester:impl:assembleGoogleDebug
```
Detekt runs via the convention plugin; keep `@file:Suppress("MagicNumber")` on showcase files that use
literal dp/colors (the button story does this). Then run the app, open Tester → Storybook → (DS
Components →) your entry, and confirm the preview + every control works.

## Checklist

- [ ] Read the real component; every meaningful parameter has a control.
- [ ] Entity in `StoryBookPage.kt` extends the correct supertype (`DsStoryBookPage` for DS, else `StoryBookPage`).
- [ ] `Build.kt` factory name is `<camelCaseName>StoryFactory`.
- [ ] Page composable shares the entity's simple name; both imported in `StoryBookScreen.kt`.
- [ ] `when` branch added in `StoryBookScreen.kt` (`is` prefix for stateful, bare for stateless).
- [ ] Registered in the correct list screen with an emoji-prefixed title.
- [ ] Live preview pinned at top, controls below (STORYBOOK.md layout rule).
- [ ] `:features:tester:impl:assembleGoogleDebug` passes.