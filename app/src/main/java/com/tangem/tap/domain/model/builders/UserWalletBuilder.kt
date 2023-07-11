package com.tangem.tap.domain.model.builders

import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.tap.domain.userWalletList.GetCardImageUseCase

class UserWalletBuilder(
    private val scanResponse: ScanResponse,
    private val getCardImageUseCase: GetCardImageUseCase = GetCardImageUseCase(),
) {
    private var backupCardsIds: Set<String> = emptySet()

    private val CardDTO.isBackupNotAllowed: Boolean
        get() = !this.settings.isBackupAllowed

    private val ScanResponse.userWalletName: String
        get() = when (productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.Start2Coin -> "Start2Coin"
            ProductType.Wallet -> when {
                card.isBackupNotAllowed -> "Tangem card"
                card.isStart2Coin -> "Start2Coin"
                else -> "Wallet"
            }
        }

    fun backupCardsIds(backupCardsIds: Set<String>?) = this.apply {
        if (backupCardsIds != null) {
            this.backupCardsIds = backupCardsIds
        }
    }

    suspend fun build(): UserWallet? {
        return with(scanResponse) {
            UserWalletIdBuilder.scanResponse(scanResponse)
                .build()
                ?.let {
                    UserWallet(
                        walletId = it,
                        name = userWalletName,
                        artworkUrl = getCardImageUseCase.invoke(card.cardId, card.cardPublicKey),
                        cardsInWallet = backupCardsIds.plus(card.cardId),
                        scanResponse = this,
                        isMultiCurrency = cardTypesResolver.isMultiwalletAllowed(),
                    )
                }
        }
    }
}