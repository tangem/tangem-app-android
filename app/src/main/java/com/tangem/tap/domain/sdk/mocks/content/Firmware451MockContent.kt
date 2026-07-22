package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

// Firmware 4.51: HD-capable (>= 4.39) but below SolanaTokensAvailable (4.52), so Solana tokens are firmware-limited.
object Firmware451MockContent : MockContent by WalletMockContent {

    override val cardDto: CardDTO = WalletMockContent.cardDto.copy(
        firmwareVersion = WalletMockContent.cardDto.firmwareVersion.copy(minor = 51),
    )

    override val scanResponse: ScanResponse = WalletMockContent.scanResponse.copy(card = cardDto)
}