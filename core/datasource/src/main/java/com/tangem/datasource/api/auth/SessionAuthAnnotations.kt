package com.tangem.datasource.api.auth

/**
 * Marks a Retrofit endpoint as needing a DPoP proof header ([RFC 9449](https://www.rfc-editor.org/rfc/rfc9449))
 * but **not** automatic refresh-on-401.
 *
 * Read at runtime by the DPoP authorization interceptor: methods carrying this annotation
 * (or the umbrella [RequiresSessionAuth]) receive `Authorization: DPoP <access-token>` (when
 * available) + `DPoP: <proof-jwt>` headers.
 *
 * Use this on endpoints that are themselves part of the refresh flow — e.g. `/auth/refresh` —
 * to prevent the session authenticator from re-entering refresh on a 401 (which would deadlock
 * the single-flight refresh mutex).
 *
 * For ordinary session-protected endpoints, prefer the combined [RequiresSessionAuth].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresDpopProof

/**
 * Marks a Retrofit endpoint as eligible for automatic session-token refresh on 401/403.
 *
 * Read at runtime by the session authenticator: methods carrying this annotation (or the
 * umbrella [RequiresSessionAuth]) trigger `SessionTokenRefresher.refresh()` + a single retry
 * with new tokens when the server responds with 401/403.
 *
 * Important: this annotation alone does **not** instruct the DPoP interceptor to add headers
 * on the initial outgoing request. The retry built by the session authenticator after a
 * successful refresh, however, always carries fresh `Authorization` / `DPoP` headers — that
 * happens regardless of which annotation gated the refresh.
 *
 * Rare in isolation — proof and refresh-on-401 almost always travel together. Prefer the
 * combined [RequiresSessionAuth] unless you have a concrete reason to omit proof on send.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresSessionRefresh

/**
 * Marks a Retrofit endpoint as fully session-protected ([RFC 9449](https://www.rfc-editor.org/rfc/rfc9449)).
 *
 * Combines [RequiresDpopProof] (outgoing `Authorization: DPoP <access-token>` + `DPoP: <proof-jwt>`
 * headers via the DPoP authorization interceptor) and [RequiresSessionRefresh] (automatic refresh +
 * single retry on 401/403 via the session authenticator).
 *
 * Default choice for normal session-protected endpoints. Use the two specialised annotations only
 * when you need exactly one of the behaviours — typically `@RequiresDpopProof` on endpoints inside
 * the refresh flow itself (`/auth/refresh`) to prevent recursion.
 *
 * Mirrors the per-operation `security` blocks in the backend OpenAPI contract; follows the same
 * on-method annotation pattern as `@ReadTimeout` / `@ConnectTimeout`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresSessionAuth