package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

// Wallet1 on a first-batch id (AC01) — resolves to the V1 (legacy) derivation style.
object Wallet1LegacyDerivationMockContent : MockContent by WalletMockContent {

    override val cardDto: CardDTO = WalletMockContent.cardDto.copy(batchId = "AC01")

    override val scanResponse: ScanResponse = WalletMockContent.scanResponse.copy(card = cardDto)
}