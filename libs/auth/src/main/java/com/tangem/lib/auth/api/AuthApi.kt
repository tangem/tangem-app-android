package com.tangem.lib.auth.api

import com.tangem.lib.auth.api.models.request.AuthApiRequest
import com.tangem.lib.auth.api.models.request.McpDeviceVerificationConsent
import com.tangem.lib.auth.api.models.request.NonceApiRequest
import com.tangem.lib.auth.api.models.request.RefreshApiRequest
import com.tangem.lib.auth.api.models.request.RegisterApiRequest
import com.tangem.lib.auth.api.models.request.WalletRegistrationRequest
import com.tangem.lib.auth.api.models.request.WalletUnregisterRequest
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResponse
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResult
import com.tangem.lib.auth.api.models.response.McpSessionResponse
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import com.tangem.core.remote.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Tangem Auth Service API (JWT session tokens / DPoP interceptor / refresh rotation)
 */
interface AuthApi {

    /**
     * Request device registration nonce.
     *
     * Generates a nonce bound to the device public key for the device registration flow.
     */
    @POST("api/authentication/v1/mobile/nonce/device")
    suspend fun requestDeviceNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Register device.
     *
     * Registers a new device using its hardware-backed public key and issues the initial
     * session token pair. Called once per app install.
     */
    @POST("api/authentication/v1/mobile/register")
    suspend fun registerDevice(@Body request: RegisterApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Request authentication nonce.
     *
     * Generates a nonce bound to the device public key for the authentication flow.
     */
    @POST("api/authentication/v1/mobile/nonce/auth")
    suspend fun requestAuthNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Authenticate device.
     *
     * Authenticates a previously registered device using a device-key signature. Issues a new
     * JWT access token with bound `walletIds[]` and risk tier. All subsequent auth after
     * registration uses this endpoint.
     */
    @POST("api/authentication/v1/mobile/authenticate")
    suspend fun authenticate(@Body request: AuthApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Refresh tokens.
     *
     * Rotates the refresh token and issues a new access token. Uses refresh-token rotation
     * with family-based reuse detection — replaying a consumed token revokes the entire token
     * family (SR-8). Sender-constraint is verified via the DPoP-proof header (`cnf.jkt`).
     */
    @POST("api/authentication/v1/mobile/token/refresh")
    @RequiresDpopProof
    suspend fun refresh(@Body request: RefreshApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Request wallet registration nonce.
     *
     * Generates a nonce bound to the device public key for the wallet registration flow.
     */
    @POST("api/authentication/v1/mobile/nonce/wallet")
    suspend fun requestWalletNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Register a wallet.
     *
     * Binds a new wallet to an already-registered device. When a card signature is provided the
     * wallet is bound as COLD (card-backed); otherwise it is registered as a MOBILE (hot) wallet.
     * Returns refreshed session tokens reflecting the updated wallet list.
     */
    @POST("api/authentication/v1/mobile/wallet/register")
    @RequiresSessionAuth
    suspend fun registerWallet(@Body request: WalletRegistrationRequest): ApiResponse<TokenApiResponse>

    @POST("api/authentication/v1/mobile/wallet/unregister")
    @RequiresSessionAuth
    suspend fun unregisterWallet(@Body request: WalletUnregisterRequest): ApiResponse<TokenApiResponse>

    /**
     * Returns who is asking for access behind [userCode].
     *
     * Its `state`, `client_id` and `scope` have to be echoed back by [approveMcpDeviceVerification] —
     * the app never picks them itself.
     */
    @GET("api/authentication/v1/mcp/device_verification")
    @RequiresSessionAuth
    suspend fun getMcpDeviceVerification(
        @Query("user_code") userCode: String,
    ): ApiResponse<McpDeviceVerificationResponse>

    /**
     * Approves the device verification the agent started, granting it the scope it asked for.
     *
     * @param userCode the code the agent displayed, as scanned by the user.
     * @param state from the matching [getMcpDeviceVerification] response.
     */
    @POST("api/authentication/v1/mcp/device_verification")
    @RequiresSessionAuth
    suspend fun approveMcpDeviceVerification(
        @Query("user_code") userCode: String,
        @Query("state") state: String,
        @Body consent: McpDeviceVerificationConsent,
    ): ApiResponse<McpDeviceVerificationResult>

    /**
     * List MCP sessions.
     *
     * Returns the OAuth authorizations issued to MCP clients (Claude Code, MCP Inspector, etc.) for
     * the authenticated principal.
     */
    @GET("api/authentication/v1/mcp/sessions")
    @RequiresSessionAuth
    suspend fun getMcpSessions(): ApiResponse<List<McpSessionResponse>>

    /**
     * Revoke an MCP session.
     *
     * Deletes the authorization [sessionId], revoking the client's access and refresh tokens.
     */
    @DELETE("api/authentication/v1/mcp/sessions/{id}")
    @RequiresSessionAuth
    suspend fun revokeMcpSession(@Path("id") sessionId: String): ApiResponse<Unit>
}