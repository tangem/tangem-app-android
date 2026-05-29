package com.tangem.datasource.api.auth

/**
 * Marks a Retrofit endpoint as requiring an authenticated session (DPoP, see
 * [RFC 9449](https://www.rfc-editor.org/rfc/rfc9449)).
 *
 * Read at runtime by the session-auth interceptor: only methods
 * carrying this annotation receive `Authorization: DPoP <access-token>` + `DPoP: <proof-jwt>`
 * headers; unannotated methods (e.g. public nonce endpoints) pass through unchanged.
 *
 * Mirrors the per-operation `security` blocks in the backend OpenAPI contract; follows the
 * same on-method annotation pattern as `@ReadTimeout` / `@ConnectTimeout`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresSessionAuth