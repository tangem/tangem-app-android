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

> **DS components section.** If the page belongs to the DS components sub-list,
> implement [`DsStoryBookPage`] instead of `StoryBookPage` directly. The view model
> uses this marker to route back-navigation to the DS list rather than the root
> story list. `DsComponentsListStory` itself stays on `StoryBookPage`, so back
> from the DS list still goes to the root.
>
> ```kotlin
> internal data class TangemLoaderStory(
>     val selectedSize: TangemLoaderSize,
>     val onSizeChange: (TangemLoaderSize) -> Unit,
> ) : DsStoryBookPage
> ```

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

### Page layout rule (mandatory)

> **Every DS component page must show a SINGLE instance of the component at the
> top, with configuration controls below it for almost all of its parameters.**

The storybook is an interactive playground, not a static catalog. Pages must NOT
render a grid of every possible variant; instead, expose every meaningful
parameter as a control and let the user pick the configuration.

**Mapping parameter kinds to controls:**

| Parameter kind | Control |
|---|---|
| Enum-like (`size`, `color`, `shape`, `type`, `variant`) | **Chips / segmented selector** |
| Boolean (`enabled`, `selected`, `withIcon`) | **Toggle / switch** |
| Selectable boolean state | **Checkbox** |

The selected values live in the page's `StoryBookPage` data class
(e.g. `TangemLoaderStory(selectedSize, onSizeChange)`) and are wired through
`storyPageFactory` + `StateUpdater<T>` (see [Step 2](#2-create-pagefoobuildk)).

**Skeleton:**

```kotlin
@Composable
internal fun FooStory(state: FooStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(TangemTheme.colors2.surface.level1),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 1. Single component preview at the top
        ComponentPreview(/* uses state.* */)

        // 2. One control per configurable parameter below
        SizeSelector(selected = state.selectedSize, onSelect = state.onSizeChange)
        ShapeSelector(selected = state.selectedShape, onSelect = state.onShapeChange)
        EnabledToggle(checked = state.isEnabled, onCheckedChange = state.onEnabledChange)
    }
}
```

**Stateless (`data object`) pages are reserved for components with no
configurable parameters at all.**

See `TangemLoaderStory` and `TangemBadgeStory` for reference implementations.

### Layout

Use a `Column` (or `LazyColumn` if the controls overflow vertically) as the
root, with the component preview on top and the controls grouped below.

```kotlin
Column(
    modifier = modifier
        .statusBarsPadding()
        .fillMaxSize()
        .background(TangemTheme.colors2.surface.level1),
    verticalArrangement = Arrangement.spacedBy(24.dp),
) { /* preview, then controls */ }
```

### Chip selector pattern

For enum-like parameters, use a pill-shaped row of chips. The selected chip
gets `surface.level3`; unselected chips stay on `surface.level2`.

```kotlin
val shape = RoundedCornerShape(50)
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(TangemTheme.colors2.surface.level2)
        .border(1.dp, TangemTheme.colors2.border.neutral.secondary, shape)
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    SomeEnum.entries.forEach { value ->
        Chip(
            label = value.name,
            selected = value == state.selected,
            onClick = { state.onSelect(value) },
            modifier = Modifier.weight(1f),
        )
    }
}
```

See `TangemLoaderStory` (size selector) and `TangemBadgeStory.ColorToggle`
for reference implementations.

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