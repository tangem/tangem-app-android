# Error handling — `Either` / `ApiResponse` are the failure channel

A function that returns a typed result type — Arrow `Either<Error, T>`, `ApiResponse<T>`, `Result<T>` —
**is** the contract for failure. Its callers must consume that result (`fold` / `getOrElse` / `onLeft` /
`onRight` / `when`), and must **not** additionally wrap the call in `runSuspendCatching` / `try-catch`
"just in case it throws".

```kotlin
// ❌ redundant — the Either already models failure; the wrapper never fires for a total function
runSuspendCatching {
    walletRegistrar.unregister(id).onLeft { log(it) }
}.onFailure { log(it) }

// ✅ trust the Either
walletRegistrar.unregister(id).onLeft { log(it) }
```

## The other side of the contract: make the function TOTAL

Trusting the `Either` at the call site is only correct if the function is **total** — it never throws,
routing *every* failure into a `Left`. So inside an `Either`/`ApiResponse`-returning function, wrap the
operations that can actually throw (parsing, IO, SDK/NFC calls, `Instant.parse`, etc.) in `try-catch`
and `raise(...)` — do not leave a throwing statement outside the error-handling. Push the guard **down**
into the implementation, not **up** into the caller.

```kotlin
either {
    when (val response = api.unregisterWallet(request)) {   // ApiResponse — never throws
        is ApiResponse.Success -> try {
            val tokens = converter.convertBack(response.data) // Instant.parse can throw → keep inside
            store.save(tokens)
        } catch (e: Exception) {
            raise(Error.PersistenceFailed(e))                 // → Left, so the method stays total
        }
        is ApiResponse.Error -> raise(Error.Api(errorConverter.convert(response.cause)))
    }
}
```

## When `runSuspendCatching` IS the right tool

Only around a genuinely throwing call that does **not** return a typed result — e.g. an SDK/platform
call — to convert a throw into a handled value. Prefer `runSuspendCatching` over a hand-rolled
`try/catch` there (it rethrows `CancellationException`). See the related guidance in
`.claude/rules/unit-testing.md` / project memory. Do not stack it on top of a call that already returns
`Either`/`ApiResponse`.

> Note: some older auth-registration call sites (`WalletRegistrationLauncher.registerMobile` /
> `registerColdInSession`) still wrap `Either`-returning calls defensively. They predate this rule and
> should be migrated to consume the `Either` directly as they are touched.