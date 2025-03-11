package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.wallets.legacy.UserWalletsListManager

/**
 * Use case for user wallet name generation
 */
class GenerateWalletNameUseCase(
    private val userWalletsListManager: UserWalletsListManager,
) {

    operator fun invoke(productType: ProductType, isBackupNotAllowed: Boolean, isStartToCoin: Boolean): String {
        val defaultName = getDefaultName(
            productType = productType,
            isBackupNotAllowed = isBackupNotAllowed,
            isStartToCoin = isStartToCoin,
        )

        val existingNames = userWalletsListManager.userWalletsSync.map { it.name }.toSet()
        return suggestedWalletName(defaultName, existingNames)
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

    private fun getDefaultName(productType: ProductType, isBackupNotAllowed: Boolean, isStartToCoin: Boolean): String {
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