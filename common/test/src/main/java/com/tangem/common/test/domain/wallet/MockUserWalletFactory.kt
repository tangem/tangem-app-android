package com.tangem.common.test.domain.wallet

import com.tangem.common.card.WalletData
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
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

    fun createSingleWalletWithToken(): UserWallet.Cold {
        return UserWallet.Cold(
            name = "NODL",
            walletId = UserWalletId("011"),
            cardsInWallet = setOf(),
            isMultiCurrency = false,
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(maxWalletCount = 2),
                derivedKeys = emptyMap(),
            ).copy(
                productType = ProductType.Note,
                walletData = WalletData(
                    blockchain = "ETH",
                    token = WalletData.Token(
                        name = "Ethereum",
                        symbol = "ETH",
                        contractAddress = "0x",
                        decimals = 8,
                    ),
                ),
            ),
            hasBackupError = false,
        )
    }
}