package com.tangem.domain.models.wallet

import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Represents user's wallet which stored in app persistence
 *
 * @property name            User wallet name
 * @property walletId        User wallet [UserWalletId]
 */
@Serializable
sealed interface UserWallet {

    val name: String
    val walletId: UserWalletId

    /**
     * Represents user's wallet backed by tangem card.
     *
     * @property name            User wallet name
     * @property walletId        User wallet [UserWalletId]
     * @property cardsInWallet   List of cards IDs assigned with this user's wallet. The list will be empty if the wallet
     *                              has been backed up on another device.
     * @property isMultiCurrency Indicates whether this user wallet can work with more than one currency
     * @property scanResponse    [ScanResponse] of primary user's wallet card.
     */
    @Serializable
    data class Cold(
        override val name: String,
        override val walletId: UserWalletId,
        val cardsInWallet: Set<String>,
        val isMultiCurrency: Boolean,
        val hasBackupError: Boolean,
        val scanResponse: ScanResponse, // TODO: Replace with [com.tangem.domain.models.scan.CardDTO]
    ) : UserWallet {

        /** ID of user's wallet primary card */
        val cardId: String get() = scanResponse.card.cardId

        /** Indicates if the user's wallet primary card has access code */
        val hasAccessCode: Boolean get() = scanResponse.card.isAccessCodeSet

        /** Indicates if this primary card has no currency wallets */
        val isLocked: Boolean get() = scanResponse.card.wallets.isEmpty()

        /** Indicated if this primary card is imported */
        val isImported: Boolean get() = scanResponse.card.wallets.any(CardDTO.Wallet::isImported)
    }

    @Serializable
    data class Hot(
        override val name: String,
        override val walletId: UserWalletId,
        val hotWalletId: HotWalletId,
        val wallets: List<MobileWallet>?,
        val backedUp: Boolean,
    ) : UserWallet {

        val isLocked: Boolean get() = wallets == null
    }
}

@OptIn(ExperimentalContracts::class)
fun UserWallet.requireColdWallet(): UserWallet.Cold {
    contract {
        returns() implies (this@requireColdWallet is UserWallet.Cold)
    }

    return this as? UserWallet.Cold
        ?: error("This user wallet is not a cold wallet")
}

fun UserWallet.copy(name: String = this.name, walletId: UserWalletId = this.walletId): UserWallet = when (this) {
    is UserWallet.Cold -> this.copy(name = name, walletId = walletId)
    is UserWallet.Hot -> this.copy(name = name, walletId = walletId)
}

val UserWallet.isMultiCurrency
    get() = when (this) {
        is UserWallet.Cold -> isMultiCurrency
        is UserWallet.Hot -> true
    }

val UserWallet.isLocked
    get() = when (this) {
        is UserWallet.Cold -> isLocked
        is UserWallet.Hot -> isLocked
    }