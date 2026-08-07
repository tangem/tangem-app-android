package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

// Wallet2 without its ed25519_slip0010 wallet, so adding Solana warns UnsupportedCurve (no wallet for its curve).
object Wallet2NoEd25519Slip0010MockContent : MockContent by Wallet2WithSeedPhraseMockContent {

    override val cardDto: CardDTO = Wallet2WithSeedPhraseMockContent.cardDto.copy(
        wallets = Wallet2WithSeedPhraseMockContent.cardDto.wallets.filterNot {
            it.curve == EllipticCurve.Ed25519Slip0010
        },
    )

    override val scanResponse: ScanResponse = Wallet2WithSeedPhraseMockContent.scanResponse.copy(card = cardDto)
}