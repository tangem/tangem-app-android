package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus

interface PolymarketRepository {

    /**
     * Fetch the Discovery feed of prediction events (each with its top active markets).
     */
    suspend fun getEvents(): Either<DataError, List<PolymarketEvent>>

    /**
     * Read the owner's deposit-wallet address and onboarding status (BFF `GET /wallet`).
     * This is the endpoint the onboarding flow polls to observe progress.
     */
    suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState>

    /**
     * Initiate deposit-wallet deployment (BFF `POST /wallet/deploy`). The BFF derives the deposit-wallet
     * address from [ownerAddress] and deploys it via the relayer (gasless, unsigned).
     */
    suspend fun deployWallet(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletStatus>

    /**
     * Relay the fully-signed 6-approval [batch] (BFF `POST /wallet/approvals`). The deposit wallet must
     * be deployed first (otherwise the BFF responds 409).
     */
    suspend fun submitApprovals(batch: PolymarketApprovalsBatch): Either<PolymarketWalletError, PolymarketWalletStatus>
}