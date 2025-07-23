package com.tangem.common.test.domain.wallet

import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.UserWalletIdBuilder

/**
[REDACTED_AUTHOR]
 */
object MockUserWalletFactory {

    private val defaultScanResponse = MockScanResponseFactory.create(
        cardConfig = GenericCardConfig(2),
        derivedKeys = emptyMap(),
    )

    fun create(scanResponse: ScanResponse = defaultScanResponse): UserWallet.Cold {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()!!

        return UserWallet.Cold(
            walletId = userWalletId,
            name = "Wallet 1",
            cardsInWallet = emptySet(),
            scanResponse = scanResponse,
            isMultiCurrency = scanResponse.cardTypesResolver.isMultiwalletAllowed(),
            hasBackupError = false,
        )
    }
}