package com.tangem.domain.wallets.builder

import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ColdUserWalletBuilder @AssistedInject constructor(
    @Assisted private val scanResponse: ScanResponse,
    private val generateWalletNameUseCase: GenerateWalletNameUseCase,
    private val cardRepository: CardRepository,
) {
    private var backupCardsIds: Set<String> = emptySet()
    private var hasBackupError: Boolean = false

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

    suspend fun build(): UserWallet.Cold? {
        val walletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
            ?: return null

        return UserWallet.Cold(
            walletId = walletId,
            name = generateWalletNameUseCase(
                card = scanResponse.card,
                productType = scanResponse.productType,
                isStartToCoin = scanResponse.cardTypesResolver.isStart2Coin(),
            ),
            cardsInWallet = backupCardsIds.plus(scanResponse.card.cardId),
            scanResponse = scanResponse,
            isMultiCurrency = scanResponse.cardTypesResolver.isMultiwalletAllowed(),
            hasBackupError = hasBackupError || cardRepository.hasBackupError(scanResponse.card.cardId),
        )
    }

    @AssistedFactory
    interface Factory {

        fun create(scanResponse: ScanResponse): ColdUserWalletBuilder
    }
}