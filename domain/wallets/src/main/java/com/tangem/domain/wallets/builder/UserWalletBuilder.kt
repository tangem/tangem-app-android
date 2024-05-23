package com.tangem.domain.wallets.builder

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase

class UserWalletBuilder(
    private val scanResponse: ScanResponse,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val getCardImageUseCase: GetCardImageUseCase = GetCardImageUseCase(),
) {
    private var backupCardsIds: Set<String> = emptySet()
    private var hasBackupError: Boolean = false

    private val CardDTO.isBackupNotAllowed: Boolean
        get() = !settings.isBackupAllowed

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
                        name = generateWalletNameUseCase(
                            productType = productType,
                            isBackupNotAllowed = card.isBackupNotAllowed,
                            isStartToCoin = cardTypesResolver.isStart2Coin(),
                        ),
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