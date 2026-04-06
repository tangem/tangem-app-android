package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Represents the state of the wallet balance in the UI.
 *
 * The sealed interface has three implementations:
 * - [Content]: Represents the state when the wallet balance is successfully loaded.
 * - [Error]: Represents the state when there was an error loading the wallet balance.
 * - [Loading]: Represents the state when the wallet balance is currently being loaded.
 *
 * @property id The unique identifier of the wallet.
 * @property name The name of the wallet.
 */
@Immutable
internal sealed interface WalletBalanceUM {

    /** Wallet Id */
    val id: UserWalletId

    /** Wallet Name */
    val name: String

    /** Wallet Icon */
    val deviceIcon: DeviceIconUM

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
        override val deviceIcon: DeviceIconUM,
        val balance: TextReference,
        val balanceInAppBar: TextReference,
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
        override val deviceIcon: DeviceIconUM,
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
        override val deviceIcon: DeviceIconUM,
    ) : WalletBalanceUM

    /**
     * Wallet card loading state
     *
     * @property id            wallet id
     * @property name          wallet name
     */
    data class Empty(
        override val id: UserWalletId,
        override val name: String,
        override val deviceIcon: DeviceIconUM,
    ) : WalletBalanceUM

    fun copySealed(name: String): WalletBalanceUM {
        return when (this) {
            is Content -> copy(name = name)
            is Error -> copy(name = name)
            is Loading -> copy(name = name)
            is Empty -> copy(name = name)
        }
    }
}