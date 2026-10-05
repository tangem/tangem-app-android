package com.tangem.data.mcp.api

import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.RequiresSessionAuth
import com.tangem.data.mcp.api.models.request.McpDeviceVerificationConsent
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResponse
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResult
import com.tangem.data.mcp.api.models.response.McpSessionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MCP endpoints of the Tangem gateway: pairing an agent with a wallet, and the authorizations that
 * pairing issues.
 *
 * Every method rides the mobile session — [RequiresSessionAuth] installs the DPoP headers and the
 * refresh-on-401 retry — but the surface is this module's own, so it lives here rather than beside
 * the authentication endpoints.
 */
internal interface McpApi {

    /**
     * Returns who is asking for access behind [userCode].
     *
     * Its `state`, `client_id` and `scope` have to be echoed back by [approveDeviceVerification] —
     * the app never picks them itself.
     */
    @GET("api/authentication/v1/mcp/device_verification")
    @RequiresSessionAuth
    suspend fun getDeviceVerification(@Query("user_code") userCode: String): ApiResponse<McpDeviceVerificationResponse>

    /**
     * Approves the device verification the agent started, granting it the scope it asked for.
     *
     * @param userCode the code the agent displayed, as scanned by the user.
     * @param state from the matching [getDeviceVerification] response.
     */
    @POST("api/authentication/v1/mcp/device_verification")
    @RequiresSessionAuth
    suspend fun approveDeviceVerification(
        @Query("user_code") userCode: String,
        @Query("state") state: String,
        @Body consent: McpDeviceVerificationConsent,
    ): ApiResponse<McpDeviceVerificationResult>

    /**
     * Rejects the device verification the agent started, so the agent is told no instead of waiting
     * for the code to expire.
     *
     * @param userCode the code the agent displayed, as scanned by the user.
     * @param state from the matching [getDeviceVerification] response.
     */
    @DELETE("api/authentication/v1/mcp/device_verification")
    @RequiresSessionAuth
    suspend fun rejectDeviceVerification(
        @Query("user_code") userCode: String,
        @Query("state") state: String,
    ): ApiResponse<Unit>

    /**
     * Returns the OAuth authorizations issued to MCP clients (Claude Code, MCP Inspector, etc.) for
     * the authenticated principal.
     */
    @GET("api/authentication/v1/mcp/sessions")
    @RequiresSessionAuth
    suspend fun getSessions(): ApiResponse<List<McpSessionResponse>>

    /** Deletes the authorization [sessionId], revoking the client's access and refresh tokens. */
    @DELETE("api/authentication/v1/mcp/sessions/{id}")
    @RequiresSessionAuth
    suspend fun revokeSession(@Path("id") sessionId: String): ApiResponse<Unit>
}