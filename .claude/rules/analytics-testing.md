# Analytics events must be unit-tested

Analytics is business data. A broken event, a renamed param or a send that stopped firing shows no
crash and no UI change, so QA rarely sees it. It turns up weeks later as a hole in a dashboard. Unit
tests are the only thing that catches it, so this rule is **strict**.

## The rule

Every PR that **adds or changes** an analytics event ships unit tests for it in the same PR:

- a new `AnalyticsEvent` subclass
- a new, removed or renamed param, or a change to a param's value mapping
- a renamed `category` / `event`
- a new place that sends an event, or a change to the condition that decides whether it fires

**No existing test file is no excuse.** If the events class (`FooAnalyticsEvent`, `FooEvents`, …)
has no `FooAnalyticsEventTest` yet, create it. You don't have to backfill the events that were there
before. Cover what you add or touch, and the file is there for whoever comes next.

A PR that adds or changes an event without tests is incomplete, and reviewers block it.

## What to test

### 1. The event contract: id and params

Build the event and check it against the analytics spec:

- `event.id` (`"[Category] Event"`) and the **whole** `event.params` map in one `isEqualTo(mapOf(…))`.
  A whole-map assertion catches an extra or missing param, which a field-by-field check misses.
- Expected values are **string literals copied from the spec** (`"Display Id"`, `"Yes"`), not the
  production constants. If the test reads the same constant as the code, a renamed key still passes.
- Conditional params need a case with the param present **and** a case with it absent.
- Value mappings (enum → string, `Boolean` → `"Yes"`/`"No"`, error → code) are a parameterized test
  over every input, using `@ProvideTestModels` or `@EnumSource`.

References: `PromoBannerAnalyticsEventTest`, `JointAccountAnalyticsEventTest`,
`FeedSectionAnalyticsEventTest`.

### 2. The send: when it fires

Whatever decides to send the event (a `*AnalyticsSender`, a model, a subscriber) gets a test that
mocks `AnalyticsEventHandler` and verifies the exact event:

```kotlin
verify(exactly = 1) { analyticsEventHandler.send(expectedEvent) }
```

`AnalyticsEvent.equals` compares the class, category, event and params, so `send(expectedEvent)`
checks the payload as well as the call. Where a condition says the event must **not** fire (a
deduplicated "shown" event, an error type that isn't reported), test that too with
`verify(exactly = 0) { analyticsEventHandler.send(any()) }`, or match on the event type.

References: `AddressBookAnalyticsSenderTest`, `WalletWarningsAnalyticsSenderTest`,
`SwapModelSlippageAnalyticsTest`.

## Placement

The test goes in the module that declares the event, with a mirrored package under `src/test/kotlin/`
(for example `features/joint-account/api/src/test/...` for events declared in `api`). Name it after the
events class: `FooAnalyticsEventTest`, `FooAnalyticsSenderTest`. Everything else (JUnit 5, GWT
names, AAA comments, MockK, Truth) follows @.claude/rules/unit-testing.md.