package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import java.math.BigInteger

interface PolymarketRepository {

    /**
     * Fetch the BFF-owned UI categories shown as Discovery feed tabs.
     */
    suspend fun getCategories(): Either<DataError, List<PolymarketCategory>>

    /**
     * Fetch the Discovery feed of prediction events (each with its top active markets).
     *
     * @param category optional category id to filter by; `null` for the default (Trending) feed
     */
    suspend fun getEvents(category: Int? = null): Either<DataError, List<PolymarketEvent>>

    /**
     * Read the owner's deposit-wallet address and onboarding status (BFF `GET /wallet`).
     * This is the endpoint the onboarding flow polls to observe progress.
     */
    suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState>

    /**
     * Initiate deposit-wallet deployment (BFF `POST /wallet/deploy`). The client supplies its own
     * CREATE2-derived [depositWalletAddress] (the BFF re-derives and cross-checks) and the current
     * [userWalletId]. Gasless, unsigned; the client then polls `GET /wallet`.
     */
    suspend fun deployWallet(
        ownerAddress: String,
        userWalletId: UserWalletId,
        depositWalletAddress: String,
    ): Either<PolymarketWalletError, PolymarketWalletStatus>

    /**
     * Relay the fully-signed 6-approval [batch] (BFF `POST /wallet/approvals`). The deposit wallet must
     * be deployed first (otherwise the BFF responds 409).
     */
    suspend fun submitApprovals(batch: PolymarketApprovalsBatch): Either<PolymarketWalletError, PolymarketWalletStatus>

    /**
     * Check whether Polymarket is geo-blocked for the caller's region (`GET polymarket.com/api/geoblock`).
     * Returns `true` when trading is blocked.
     */
    suspend fun checkGeoblock(): Either<DataError, Boolean>

    /**
     * Fetch the relayer nonce for [ownerAddress] (`GET relayer-v2.polymarket.com/nonce?address=&type=WALLET`),
     * used as the `nonce` field of the approvals `Batch` signed during onboarding.
     */
    suspend fun getRelayerNonce(ownerAddress: String): Either<DataError, BigInteger>

    /**
     * Deterministically re-derive the CLOB L2 API credentials for the signer
     * (`GET clob.polymarket.com/auth/derive-api-key`). Idempotent; try this before [createApiCredentials].
     */
    suspend fun deriveApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials>

    /**
     * Create a fresh set of CLOB L2 API credentials (`POST clob.polymarket.com/auth/api-key`).
     */
    suspend fun createApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials>

    /**
     * Refresh the CLOB's cached collateral balance and allowance for the owner's deposit wallet
     * (`GET clob.polymarket.com/balance-allowance/update`). Authenticated with the L2 HMAC headers derived
     * from [credentials]; changes no on-chain or backend state.
     */
    suspend fun syncBalanceAllowance(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, Unit>
}