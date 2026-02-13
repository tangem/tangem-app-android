package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId

/** Wallet card state */
@Immutable
internal sealed interface WalletBalanceUM {

    /** Wallet Id */
    val id: UserWalletId

    /** Wallet Name */
    val name: String

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property name           wallet name
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val name: String,
        val balance: TextReference,
        val isBalanceFlickering: Boolean,
        val isZeroBalance: Boolean?,

    ) : WalletBalanceUM

    /**
     * Wallet card error state
     *
     * @property id            wallet id
     * @property name             wallet name
     */
    data class Error(
        override val id: UserWalletId,
        override val name: String,
    ) : WalletBalanceUM

    /**
     * Wallet card loading state
     *
     * @property id            wallet id
     * @property name          wallet name
     */
    data class Loading(
        override val id: UserWalletId,
        override val name: String,
    ) : WalletBalanceUM

    fun copySealed(name: String): WalletBalanceUM {
        return when (this) {
            is Content -> copy(name = name)
            is Error -> copy(name = name)
            is Loading -> copy(name = name)
        }
    }
}