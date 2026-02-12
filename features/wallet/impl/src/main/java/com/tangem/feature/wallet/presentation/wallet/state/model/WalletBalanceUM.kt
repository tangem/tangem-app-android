package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.PersistentList

/** Wallet card state */
@Immutable
internal sealed interface WalletBalanceUM {

    /** Id */
    val id: UserWalletId

    /** Title */
    val title: TextReference

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val title: TextReference,
        val balance: TextReference,
        val isBalanceFlickering: Boolean,
        val isZeroBalance: Boolean?,

        ) : WalletBalanceUM

    /**
     * Wallet card error state
     *
     * @property id            wallet id
     * @property title         wallet name
     */
    data class Error(
        override val id: UserWalletId,
        override val title: TextReference,
    ) : WalletBalanceUM

    /**
     * Wallet card loading state
     *
     * @property id            wallet id
     * @property title         wallet name
     */
    data class Loading(
        override val id: UserWalletId,
        override val title: TextReference,
    ) : WalletBalanceUM

    fun copySealed(title: String): WalletBalanceUM {
        return when (this) {
            is Content -> copy(title = stringReference(title))
            is Error -> copy(title = stringReference(title))
            is Loading -> copy(title = stringReference(title))
        }
    }
}