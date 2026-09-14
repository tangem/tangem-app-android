# Decompose — a child component gets a *child* context

A component never hands a child the context it runs on itself — not `this`, not its own `context`
constructor parameter. The parent creates a fresh child context for every child it builds:

| Where the child comes from | What to pass |
|---|---|
| The parent constructs it directly | `child(key = "…")` |
| Decompose hands a `ComponentContext` (`childStack` / `childSlot` / `childPages` factory) | `childByContext(componentContext)` |

Both live in `com.tangem.core.decompose.context`.

```kotlin
// ❌ the child runs on the parent's context
private val stepper = stepperComponentFactory.create(context = this, params = …)

// ✅ its own keyed child context
private val stepper = stepperComponentFactory.create(context = child(key = "stepper"), params = …)
```

## Why

`getOrCreateModel()` stores the model in the context's `InstanceKeeper` under `model_<ClassName>`,
and `childStack`/`childSlot`/`childPages` register their keys in the same context. A shared context
therefore means: the child's model outlives the child, two children with the same model class
collide, and the child's `componentScope`, back handling and state keeper are really the parent's. A
child context gives the child a lifecycle of its own (ending with the parent's), its own retained
model and its own navigation namespace.

## Keys

Keys must be unique among the children of one parent — a duplicate throws at construction. When the
key also identifies the child elsewhere (a lazy-list item key, for example), declare it once on the
child and reference it from the parent:

```kotlin
ForYouBlockComponent(context = child(key = ForYouBlockComponent.KEY))
```

## Take the context you are given

A component delegates the context it receives (`AppComponentContext by context`) and does not
re-derive it (`by context.child(key)`) in its own constructor. Creating the child context is the
parent's job precisely so the nesting is visible at the call site; a component that silently
re-parents its argument nests twice as soon as a caller does the right thing.