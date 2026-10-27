package com.tangem.domain.mcp.repository

import arrow.core.Either
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Authorizations the wallet owner has granted to MCP clients, and the device verification flow that
 * creates them.
 *
 * Every call runs on top of the mobile authentication session, so a verification and the approval
 * that follows it have to be made from the same device.
 */
interface McpRepository {

    /**
     * Returns the agent behind [clientId], for the sheets that tell the owner who is asking for something.
     *
     * Never fails: an agent that cannot be looked up (unknown client, no network, rate limit) comes back as
     * [McpAgent.unknown], because not knowing who asks must not block the owner from the request itself.
     */
    suspend fun getAgent(clientId: String): McpAgent

    /** Returns who is asking for access behind [userCode], so the owner can decide on it. */
    suspend fun getDeviceVerification(userCode: String): Either<Throwable, McpDeviceVerification>

    /**
     * Grants [verification] access to [walletId], with the client and the scope it carries.
     *
     * A refusal by the service comes back as [McpDeviceVerificationResult.isSuccess], not as a failure.
     */
    suspend fun approveDeviceVerification(
        verification: McpDeviceVerification,
        walletId: UserWalletId,
    ): Either<Throwable, McpDeviceVerificationResult>

    /** Turns [verification] down, so the agent is told no instead of waiting for the code to expire. */
    suspend fun rejectDeviceVerification(verification: McpDeviceVerification): Either<Throwable, Unit>

    /**
     * Authorizations currently issued to MCP clients, as a hot list shared by every collector.
     *
     * Collecting it reads the list unless a read is already in flight, and the list is re-read after
     * every change this repository makes. It stays [Lce.Loading] until the first read answers, and a
     * failed read answers with [Lce.Error] rather than the last known list — after a revoke or an
     * approval that list is no longer known to be current, so the caller offers [reloadSessions]
     * instead of showing it.
     */
    fun getSessions(): Flow<Lce<Throwable, List<McpSession>>>

    /**
     * Reads the list again and suspends until that read is over, whatever it ends with.
     *
     * The read in flight, if any, is dropped: it predates this call, and the caller is asking about
     * the list as it is now.
     */
    suspend fun reloadSessions()

    /**
     * Revokes [sessionId], cutting off the client's access and refresh tokens.
     *
     * A session the service no longer knows is a success, not a failure: the authorization is gone
     * either way, and the caller has nothing left to tell the owner about.
     */
    suspend fun revokeSession(sessionId: String): Either<Throwable, Unit>

    /**
     * Revokes every authorization currently issued, across all wallets — the agent half of the
     * screen's Disconnect All, which cuts off every dApp the same way.
     *
     * The service has no bulk endpoint, so this is the single-session revoke sent for each of them at
     * once. A session that fails to be revoked stays, and comes back with the reloaded list.
     */
    suspend fun revokeAllSessions()
}