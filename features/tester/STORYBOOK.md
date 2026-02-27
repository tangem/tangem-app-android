# Storybook — Adding New Design System Pages

The storybook lives in `features/tester` and lets developers browse and validate
design system components at runtime on a device or emulator.

---

## Architecture overview

```
storybook/
├── entity/
│   ├── StoryBookPage.kt      ← sealed interface + all page state classes
│   ├── StoryBookUM.kt        ← top-level UI model (current page, navigation)
│   └── StoryPageFactory.kt   ← factory interface used by the list screen
├── page/
│   └── <component>/
│       ├── Build.kt          ← creates the StoryPageFactory for this page
│       └── <Component>Story.kt  ← the Composable that renders the showcase
├── ui/
│   ├── StoryBookListScreen.kt ← list of all stories (add your entry here)
│   └── StoryBookScreen.kt    ← routes currentPage → correct Composable
└── viewmodel/
    ├── StoryBookViewModel.kt
    └── StateUpdater.kt       ← helper for stateful pages
```

---

## Step-by-step: adding a new page

### 1. Declare the page type in `StoryBookPage.kt`

For a **stateless** showcase (no user interaction that mutates page state):
```kotlin
internal data object FooStory : StoryBookPage
```

For a **stateful** page (e.g. toggle between variants like NorthernLightsStory):
```kotlin
internal data class FooStory(
    val selectedVariant: Variant,
    val onVariantChange: (Variant) -> Unit,
) : StoryBookPage {
    enum class Variant { A, B }
}
```

---

### 2. Create `page/foo/Build.kt`

**Stateless:**
```kotlin
internal val fooStoryFactory: StoryPageFactory = StoryPageFactory { FooStory }
```

**Stateful** (use `storyPageFactory` + `StateUpdater`):
```kotlin
internal fun StateUpdater<FooStory>.build(): FooStory {
    return FooStory(
        selectedVariant = FooStory.Variant.A,
        onVariantChange = { newVariant ->
            updateStory { it.copy(selectedVariant = newVariant) }
        },
    )
}

internal val fooStoryFactory
    get() = storyPageFactory(StateUpdater<FooStory>::build)
```

---

### 3. Create `page/foo/FooStory.kt`

Write a `@Composable internal fun FooStory(...)` that renders the showcase.
See [Design guidelines](#design-guidelines) below for layout advice.

**Stateless example skeleton:**
```kotlin
@Composable
internal fun FooStory(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item("section_a") { /* ... */ }
    }
}
```

**Stateful example skeleton:**
```kotlin
@Composable
internal fun FooStory(state: FooStory, modifier: Modifier = Modifier) {
    // use state.selectedVariant, state.onVariantChange
}
```

---

### 4. Register in `StoryBookScreen.kt`

Add a branch to the `when` block.

> **Naming note:** the entity type and the Composable function will share the
> same simple name (e.g. `FooStory`). Kotlin resolves them correctly — the
> entity import is used in the pattern position, the function import is used
> as a call. This is the same pattern used for `NorthernLightsStory` and
> `ButtonsStory`.

```kotlin
import com.tangem.feature.tester.presentation.storybook.entity.FooStory
import com.tangem.feature.tester.presentation.storybook.page.foo.FooStory

when (storyState) {
    StoryList            -> StoryBookListScreen(state = state)
    is NorthernLightsStory -> NorthernLightsStory(state = storyState)
    ButtonsStory         -> ButtonsStory()
    FooStory             -> FooStory()          // stateless
    is FooStory          -> FooStory(storyState) // stateful (note `is`)
}
```

---

### 5. Register in `StoryBookListScreen.kt`

Add one entry to `buildStories()`. **Every title must start with an emoji** that
represents the component category — this makes the list easier to scan at a glance.

```kotlin
private fun buildStories() = listOf(
    StoryItem(title = "🃏 Foo Component", factory = fooStoryFactory),
    // existing entries...
)
```

Pick an emoji that reflects the component's visual nature or purpose, e.g.:
- Buttons → 🔘
- Background effects → 🌌
- Typography → 🔤
- Icons → 🎨
- Cards → 🃏
- Inputs / Text fields → ✏️
- Navigation → 🧭
- Loaders / Progress → ⏳

---

## Design guidelines

### Layout

Use a `LazyColumn` as the root for component showcases so the page scrolls
when content is taller than the screen.

```kotlin
LazyColumn(
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxSize().background(TangemTheme.colors2.surface.level1),
) { /* items */ }
```

### Showing all variants

Show every meaningful axis of variation in one place:

| Axis | How to display |
|---|---|
| **States** (Default, Disabled, Pressed, Loading) | One row per state |
| **Shapes** (Default, Rounded) | One labeled group (`ShapeGroup`) per shape, iterate `TangemButtonShape.entries` |
| **Content** (text+icon vs icon-only) | Two columns per row |
| **Sizes** | Separate `LazyColumn` item per size group if needed |
| **Styles / Effects** (e.g. `TangemMessageEffect`) | Chip toggle — see below |

> **Prefer vertical stacking over horizontal.** A row should contain at most
> 2–3 components; more than that overflows on narrow screens. Use
> `Modifier.weight(1f)` on columns instead of fixed widths.

### Toggle for style/effect axes

When a discrete axis (e.g. a visual effect enum) would produce too many full-width
components on one screen, use a **sticky chip-picker** instead of stacking all values.
Make the page **stateful** and store the selected value in the `StoryBookPage` data class.

```
┌─────────────────────────────────┐  ← stickyHeader
│  Magic  │  Card  │ Warning │ None│  ← chip row (EffectToggle)
└─────────────────────────────────┘
  No icon, no buttons
  [  message with selected effect ]
  With icon
  [  message with selected effect ]
  …
```

**Pattern:**

1. Add the selected value + callback to the `StoryBookPage` data class:
   ```kotlin
   internal data class FooStory(
       val selectedVariant: Variant,
       val onVariantChange: (Variant) -> Unit,
   ) : StoryBookPage
   ```
2. Use a stateful `Build.kt` (see [Step 2](#2-create-pagefoobuildk)).
3. In the story composable, add a `stickyHeader` with a chip row:
   ```kotlin
   stickyHeader("toggle") {
       VariantToggle(
           selected = state.selectedVariant,
           onSelect = state.onVariantChange,
           modifier = Modifier
               .fillMaxWidth()
               .background(TangemTheme.colors2.surface.level1)
               .padding(horizontal = 16.dp, vertical = 8.dp),
       )
   }
   ```
4. Each `item` below uses `state.selectedVariant` for the component under test.

See `TangemMessageStory` for a complete example.

### Section structure (component grids)

Follow the pattern used in `ButtonsStory`:
- **Section title** — `TangemTheme.typography.subtitle1`
- **Group sub-header** (shape/size/variant name) — `TangemTheme.typography.body2`
- **Column headers** (Text + Icon, Icon only, etc.) — `TangemTheme.typography.caption2`
- **State label** (Default, Disabled…) — `TangemTheme.typography.caption2`, fixed width ~80 dp

```
Primary                          ← subtitle1
  Default                        ← body2  (shape/group sub-header)
               Text + Icon  Icon only    ← caption2 column headers
  Default      [■ Continue] [■]          ← state row
  Disabled     [■ Continue] [■]
  Pressed      [■ Continue] [■]
  Loading      [   ⟳     ] [⟳]
  Rounded                        ← body2
  ...
```

### Colors

- Page background: `TangemTheme.colors2.surface.level1`
- Sections that need a contrasting background (e.g. PrimaryInverse):
  `TangemTheme.colors2.surface.level2`
- Section divider: `HorizontalDivider(color = TangemTheme.colors2.border.neutral.secondary)`

### Realistic text

Use representative text strings, not placeholders like "Btn". Pick labels that
match how the component would appear in the product (e.g. `"Continue"`,
`"Send payment"`, `"Confirm"`).

### DS component imports

All design system components (`PrimaryTangemButton`, `TangemButtonSize`, etc.)
live in `com.tangem.core.ui.ds.*` and are `public`, so they are directly
importable from the `features/tester` module.

Use `com.tangem.core.ui.R` for drawable resources (e.g. `R.drawable.ic_tangem_24`).