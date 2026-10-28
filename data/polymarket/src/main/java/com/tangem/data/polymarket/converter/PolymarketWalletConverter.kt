package com.tangem.data.polymarket.converter

import arrow.core.Either
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.logging.TangemLogger

/** Maps between the BFF wallet DTOs and the domain wallet models. */
internal object PolymarketWalletConverter {

    fun toState(response: PolymarketWalletStatusResponse): PolymarketWalletState = PolymarketWalletState(
        depositWalletAddress = response.depositWalletAddress,
        status = PolymarketWalletStatus.fromRaw(response.status),
        sessionKeys = response.sessionKeys.orEmpty().mapNotNull { dto ->
            Either.catch { PolymarketSessionKeyConverter.convert(dto) }
                .onLeft { TangemLogger.e("Polymarket session key ${dto.address} dropped: invalid timestamps", it) }
                .getOrNull()
        },
    )

    fun toRequest(batch: PolymarketApprovalsBatch): PolymarketWalletApprovalsRequest = PolymarketWalletApprovalsRequest(
        ownerAddress = batch.ownerAddress,
        depositWalletAddress = batch.depositWalletAddress,
        nonce = batch.nonce,
        deadline = batch.deadline,
        calls = batch.calls.map { PolymarketApprovalCallDto(target = it.target, value = it.value, data = it.data) },
        signature = batch.signature,
    )
}