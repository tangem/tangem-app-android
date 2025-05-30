package com.tangem.tap.domain.userWalletList.utils

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.userWalletList.model.UserWalletPublicInformation
import com.tangem.tap.domain.userWalletList.model.UserWalletSensitiveInformation

internal val UserWallet.sensitiveInformation: UserWalletSensitiveInformation
    get() = when (this) {
        is UserWallet.Cold -> UserWalletSensitiveInformation(
            wallets = scanResponse.card.wallets,
            visaCardActivationStatus = scanResponse.visaCardActivationStatus,
        )
        is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
    }

internal val UserWallet.publicInformation: UserWalletPublicInformation
    get() = when (this) {
        is UserWallet.Cold -> UserWalletPublicInformation(
            name = name,
            walletId = walletId,
            cardsInWallet = cardsInWallet,
            isMultiCurrency = isMultiCurrency,
            scanResponse = scanResponse.copy(
                card = scanResponse.card.copy(
                    wallets = emptyList(),
                ),
                visaCardActivationStatus = null,
            ),
            hasBackupError = hasBackupError,
        )
        is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
    }

internal fun UserWalletPublicInformation.toUserWallet(): UserWallet {
    return UserWallet.Cold(
        name = name,
        walletId = walletId,
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
    return when (this) {
        is UserWallet.Cold -> {
            copy(
                scanResponse = scanResponse.copy(
                    card = scanResponse.card.copy(
                        wallets = sensitiveInformation.wallets,
                    ),
                    visaCardActivationStatus = sensitiveInformation.visaCardActivationStatus,
                ),
            )
        }
        is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
    }
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

internal fun UserWallet.lock(): UserWallet = when (this) {
    is UserWallet.Cold -> {
        copy(
            scanResponse = scanResponse.copy(
                card = scanResponse.card.copy(
                    wallets = emptyList(),
                ),
                visaCardActivationStatus = null,
            ),
        )
    }
    is UserWallet.Hot -> TODO("[REDACTED_TASK_KEY]")
}