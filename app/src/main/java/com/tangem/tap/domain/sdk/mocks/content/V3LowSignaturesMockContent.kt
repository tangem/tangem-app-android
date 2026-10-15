package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

object V3LowSignaturesMockContent : MockContent by V3MockContent {

    const val REMAINING_SIGNATURES = 10

    override val cardDto: CardDTO = V3MockContent.cardDto.copy(
        wallets = V3MockContent.cardDto.wallets.map { it.copy(remainingSignatures = REMAINING_SIGNATURES) },
    )

    override val scanResponse: ScanResponse = V3MockContent.scanResponse.copy(card = cardDto)
}