package com.tangem.domain.userwallets

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet

class UserWalletBuilder(
    private val scanResponse: ScanResponse,
    private val getCardImageUseCase: GetCardImageUseCase = GetCardImageUseCase(),
) {
    private var backupCardsIds: Set<String> = emptySet()
    private var hasBackupError: Boolean = false

    private val CardDTO.isBackupNotAllowed: Boolean
        get() = !this.settings.isBackupAllowed

    private val ScanResponse.userWalletName: String
        get() = when (productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.Start2Coin -> "Start2Coin"
            ProductType.Visa -> "Tangem Visa"
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> when {
                card.isBackupNotAllowed -> "Tangem card"
                cardTypesResolver.isStart2Coin() -> "Start2Coin"
                else -> "Wallet"
            }
        }

    /**
     * DANGEROUS!!!
     * [backupCardsIds] will be non-empty list if card is backed up on current device.
     */
    fun backupCardsIds(backupCardsIds: Set<String>?) = this.apply {
        if (backupCardsIds != null) {
            this.backupCardsIds = backupCardsIds
        }
    }

    /**
     * Sets if UserWallet has any backup errors (wrong curves etc). Use in onboarding
     */
    fun hasBackupError(hasBackupError: Boolean) = this.apply {
        this.hasBackupError = hasBackupError
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
                        hasBackupError = hasBackupError,
                    )
                }
        }
    }
}