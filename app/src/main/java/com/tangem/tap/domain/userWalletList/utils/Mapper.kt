package com.tangem.tap.domain.userWalletList.utils

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation

internal val UserWallet.sensitiveInformation: UserWalletSensitiveInformation
    get() = UserWalletSensitiveInformation(scanResponse.card.wallets)

internal val UserWallet.publicInformation: UserWalletPublicInformation
    get() = UserWalletPublicInformation(
        name = name,
        walletId = walletId,
        artworkUrl = artworkUrl,
        cardsInWallet = cardsInWallet,
        isMultiCurrency = isMultiCurrency,
        scanResponse = scanResponse.copy(
            card = scanResponse.card.copy(
                wallets = emptyList(),
            ),
        ),
        hasBackupError = hasBackupError,
    )

internal fun UserWalletPublicInformation.toUserWallet(): UserWallet {
    return UserWallet(
        name = name,
        walletId = walletId,
        artworkUrl = artworkUrl,
        cardsInWallet = cardsInWallet,
        scanResponse = scanResponse,
        isMultiCurrency = isMultiCurrency,
        hasBackupError = hasBackupError,
    )
}

internal fun List<UserWalletPublicInformation>.toUserWallets(): List<UserWallet> {
    return this.map { it.toUserWallet() }
}

internal fun UserWallet.updateWith(sensitiveInformation: UserWalletSensitiveInformation): UserWallet {
    return copy(
        scanResponse = scanResponse.copy(
            card = scanResponse.card.copy(
                wallets = sensitiveInformation.wallets,
            ),
        ),
    )
}

internal fun List<UserWallet>.updateWith(
    walletIdToSensitiveInformation: Map<UserWalletId, UserWalletSensitiveInformation>,
): List<UserWallet> {
    return if (walletIdToSensitiveInformation.isEmpty()) {
        this
    } else {
        this.map { wallet ->
            walletIdToSensitiveInformation[wallet.walletId]
                ?.let(wallet::updateWith)
                ?: wallet
        }
    }
}

internal fun List<UserWallet>.lockAll(): List<UserWallet> = map(UserWallet::lock)

internal fun UserWallet.lock(): UserWallet = copy(
    scanResponse = scanResponse.copy(
        card = scanResponse.card.copy(
            wallets = emptyList(),
        ),
    ),
)