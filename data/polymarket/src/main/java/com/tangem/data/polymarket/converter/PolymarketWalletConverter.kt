package com.tangem.data.polymarket.converter

import com.tangem.datasource.api.polymarket.models.PolymarketApprovalCallDto
import com.tangem.datasource.api.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.datasource.api.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import javax.inject.Inject

/** Maps between the BFF wallet DTOs and the domain wallet models. */
internal class PolymarketWalletConverter @Inject constructor() {

    fun toState(response: PolymarketWalletStatusResponse): PolymarketWalletState = PolymarketWalletState(
        depositWalletAddress = response.depositWalletAddress,
        status = PolymarketWalletStatus.fromRaw(response.status),
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