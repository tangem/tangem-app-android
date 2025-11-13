package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.requireUserWalletsSync
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.wallets.legacy.UserWalletsListManager

/**
 * Use case for user wallet name generation
 */
class GenerateWalletNameUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    private val CardDTO.isBackupNotAllowed: Boolean
        get() = !settings.isBackupAllowed

    operator fun invoke(productType: ProductType, card: CardDTO, isStartToCoin: Boolean): String {
        val defaultName = getDefaultName(
            productType = productType,
            card = card,
            isBackupNotAllowed = card.isBackupNotAllowed,
            isStartToCoin = isStartToCoin,
        )

        val existingNames = getNamesSet()
        return suggestedWalletName(defaultName, existingNames)
    }

    fun invokeForHot(): String {
        val defaultName = "Wallet"
        val existingNames = getNamesSet()
        return suggestedWalletName(defaultName, existingNames)
    }

    private fun getNamesSet(): Set<String> {
        return if (useNewRepository) {
            userWalletsListRepository.requireUserWalletsSync().map { it.name }.toSet()
        } else {
            userWalletsListManager.userWalletsSync.map { it.name }.toSet()
        }
    }

    private fun suggestedWalletName(defaultName: String, existingNames: Set<String>): String {
        val startIndex = 2
        if (!existingNames.contains(defaultName)) {
            return defaultName
        }

        for (index in startIndex..MAX_WALLETS_LIMIT) {
            val potentialName = "$defaultName $index"
            if (!existingNames.contains(potentialName)) {
                return potentialName
            }
        }

        return defaultName
    }

    private fun getDefaultName(
        productType: ProductType,
        card: CardDTO,
        isBackupNotAllowed: Boolean,
        isStartToCoin: Boolean,
    ): String {
        /**
         * @workaround
         * There were produced 20k Note demo cards that should work like multiwallet (except Onboarding)
         * for that reasons we've just added some specific checks for their BatchId
         */
        if (DemoConfig.isDemoNoteAsMultiwallet(card.cardId)) {
            return "Wallet"
        }
        return when (productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.Start2Coin -> "Start2Coin"
            ProductType.Visa -> "Tangem Visa"
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> when {
                isBackupNotAllowed -> "Tangem card"
                isStartToCoin -> "Start2Coin"
                else -> "Wallet"
            }
        }
    }

    companion object {
        const val MAX_WALLETS_LIMIT = 10000
    }
}