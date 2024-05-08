package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.wallets.manager.UserWalletsListManager

/**
 * Use case for user wallet name generation
 */
class WalletNameGenerateUseCase (
    private val userWalletsListManager: UserWalletsListManager,
) {

    suspend operator fun invoke(
        productType: ProductType,
        isBackupNotAllowed: Boolean,
        isStartToCoin: Boolean,
    ): String {
        val defaultName = getDefaultName(
            productType = productType,
            isBackupNotAllowed = isBackupNotAllowed,
            isStartToCoin = isStartToCoin,
        )

        val existingNames = userWalletsListManager.userWalletsSync.map { it.name }.toSet()
        return suggestedWalletName(defaultName, existingNames)
    }

    private fun suggestedWalletName(defaultName: String, existingNames: Set<String>): String {
        val startIndex = 1
        if (!existingNames.contains(defaultName)) {
            return defaultName
        }

        for (index in startIndex..10000) {
            val potentialName = "$defaultName $index"
            if (!existingNames.contains(potentialName)) {
                return potentialName
            }
        }

        return defaultName
    }

    private fun getDefaultName(
        productType: ProductType,
        isBackupNotAllowed: Boolean,
        isStartToCoin: Boolean,
    ): String {
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
}
