# libs/auth

## Calling a session-protected endpoint

An endpoint of any Tangem backend that requires the mobile session needs two things — miss either one and
the request silently goes out without DPoP headers, getting a 401 from the server.

**1. Annotate each method** (`com.tangem.lib.auth.api`, add `implementation(projects.libs.auth)`):

```kotlin
@GET("…")
@RequiresSessionAuth
suspend fun getFoo(): ApiResponse<FooResponse>
```

**2. Build the client with session auth** in the module's Hilt module:

```kotlin
factory.build(
    RetrofitApiSpec(
        apiConfigId = Foo.ID,
        shouldApplyTimeoutAnnotations = false,
        shouldUseSessionAuth = true,
    ),
)
```

`shouldUseSessionAuth` installs the DPoP interceptor and the session authenticator on the whole client; the
annotations decide per method whether they act. Methods without an annotation on such a client go out
unchanged, so public and protected endpoints can share one interface. An annotation on a client built with
`shouldUseSessionAuth = false` does nothing — the Express `/v1`/`/v2` toggle relies on exactly that.

### Which annotation

| Annotation | DPoP headers on send | Refresh + one retry on 401/403 |
|---|---|---|
| `@RequiresSessionAuth` | yes | yes |
| `@RequiresDpopProof` | yes | no |
| `@RequiresSessionRefresh` | no | yes |

Use `@RequiresSessionAuth`. The other two exist for the token-refresh flow itself: the refresh endpoint carries
`@RequiresDpopProof` only, because refreshing on its own 401 would re-enter the refresh single-flight and
deadlock. Never put `@RequiresSessionAuth` or `@RequiresSessionRefresh` on an endpoint the refresher calls.

### Behaviour to expect

- Before sending, the interceptor makes sure the access token is valid and refreshes it if not, joining a
  refresh already in flight — a request never goes out with a token that is being rotated.
- With no session at all (device not registered, tokens cleared) the request goes out **without** headers and
  fails with 401; it is not queued until a session appears. Handle that like any other API error.
- A 401/403 triggers at most one refresh and one retry; a second failure is returned to the caller.

## Wallet linking

A session belongs to the device; an endpoint that acts on a wallet also needs that wallet linked to the device,
otherwise the backend rejects the request. The app links wallets in the background, but one can stay unlinked
(registration failed, a card not rescanned since) — the main screen then shows a banner.

A feature does not link wallets itself and never calls `WalletRegistrar`. It asks the user through `domain:wallets`:

- **Before a user action** that hits such an endpoint, call `EnsureWalletLinkedUseCase(userWallet)`. It returns
  `true` right away for a linked wallet (or an unknown status); otherwise it shows the link sheet and returns
  `true` only once the wallet is linked. On `false` stop the action silently — the user closed the sheet, or linking
  failed and the sheet has already shown an error dialog. Do not call it around background requests: it shows UI.
- **After the backend rejected a request** for an unlinked wallet, call `WalletLinkRequester.requestLink(userWallet)`
  and retry on `Result.Linked`.

The sheet is hosted by the root component and drawn over any screen, so the feature does not host anything.