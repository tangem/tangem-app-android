package com.tangem.datasource.api.auth

import com.tangem.datasource.api.auth.models.request.AuthApiRequest
import com.tangem.datasource.api.auth.models.request.NonceApiRequest
import com.tangem.datasource.api.auth.models.request.RefreshApiRequest
import com.tangem.datasource.api.auth.models.request.RegisterApiRequest
import com.tangem.datasource.api.auth.models.request.WalletRegistrationRequest
import com.tangem.datasource.api.auth.models.response.NonceApiResponse
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Tangem Auth Service API (JWT session tokens / DPoP interceptor / refresh rotation)
 */
interface AuthApi {

    /**
     * Request device registration nonce.
     *
     * Generates a nonce bound to the device public key for the device registration flow.
     */
    @POST("api/v1/auth/nonce/device")
    suspend fun requestDeviceNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Register device.
     *
     * Registers a new device using its hardware-backed public key and issues the initial
     * session token pair. Called once per app install.
     */
    @POST("api/v1/auth/register")
    suspend fun registerDevice(@Body request: RegisterApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Request authentication nonce.
     *
     * Generates a nonce bound to the device public key for the authentication flow.
     */
    @POST("api/v1/auth/nonce/auth")
    suspend fun requestAuthNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Authenticate device.
     *
     * Authenticates a previously registered device using a device-key signature. Issues a new
     * JWT access token with bound `walletIds[]` and risk tier. All subsequent auth after
     * registration uses this endpoint.
     */
    @POST("api/v1/auth/authenticate")
    suspend fun authenticate(@Body request: AuthApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Refresh tokens.
     *
     * Rotates the refresh token and issues a new access token. Uses refresh-token rotation
     * with family-based reuse detection — replaying a consumed token revokes the entire token
     * family (SR-8). Sender-constraint is verified via the DPoP-proof header (`cnf.jkt`).
     */
    @POST("api/v1/auth/refresh")
    @RequiresDpopProof
    suspend fun refresh(@Body request: RefreshApiRequest): ApiResponse<TokenApiResponse>

    /**
     * Request wallet registration nonce.
     *
     * Generates a nonce bound to the device public key for the wallet registration flow.
     */
    @POST("api/v1/auth/nonce/wallet")
    suspend fun requestWalletNonce(@Body request: NonceApiRequest): ApiResponse<NonceApiResponse>

    /**
     * Register a wallet.
     *
     * Binds a new wallet to an already-registered device. When a card signature is provided the
     * wallet is bound as COLD (card-backed); otherwise it is registered as a MOBILE (hot) wallet.
     * Returns refreshed session tokens reflecting the updated wallet list.
     */
    @POST("api/v1/auth/wallet")
    @RequiresDpopProof
    suspend fun registerWallet(@Body request: WalletRegistrationRequest): ApiResponse<TokenApiResponse>
}