package com.tangem.domain.wallets.builder

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class UserWalletBuilder @AssistedInject constructor(
    @Assisted private val scanResponse: ScanResponse,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
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

    fun build(): UserWallet? {
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
                        cardsInWallet = backupCardsIds.plus(card.cardId),
                        scanResponse = this,
                        isMultiCurrency = cardTypesResolver.isMultiwalletAllowed(),
                        hasBackupError = hasBackupError,
                    )
                }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(scanResponse: ScanResponse): UserWalletBuilder
    }
}