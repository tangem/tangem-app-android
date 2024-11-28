package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.StringsSigns.DASH_SIGN

/** Wallet card state */
@Immutable
internal sealed interface WalletCardState {

    /** Id */
    val id: UserWalletId

    /** Title */
    val title: String

    /** Wallet additional info */
    val additionalInfo: WalletAdditionalInfo?

    /** Wallet image resource id */
    @get:DrawableRes
    val imageResId: Int?

    /** Lambda be invoked when Rename button is clicked */
    val onRenameClick: (UserWalletId) -> Unit

    /** Lambda be invoked when Delete button is clicked */
    val onDeleteClick: (UserWalletId) -> Unit

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     * @property additionalInfo wallet additional info
     * @property cardCount      number of cards in the wallet
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
        val cardCount: Int?,
        val balance: String,
        val isZeroBalance: Boolean?,
    ) : WalletCardState

    /**
     * Wallet card locked state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     */
    data class LockedContent(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState

    /**
     * Wallet card error state
     *
     * @property id            wallet id
     * @property title         wallet name
     * @property imageResId    wallet image resource id
     * @property onRenameClick lambda be invoked when Rename button is clicked
     * @property onDeleteClick lambda be invoked when Delete button is clicked
     */
    data class Error(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo? = defaultAdditionalInfo,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState {

        private companion object {
            val defaultAdditionalInfo: WalletAdditionalInfo
                get() = WalletAdditionalInfo(hideable = true, content = EMPTY_BALANCE_TEXT)
        }
    }

    /**
     * Wallet card loading state
     *
     * @property id            wallet id
     * @property title         wallet name
     * @property imageResId    wallet image resource id
     * @property onRenameClick lambda be invoked when Rename button is clicked
     * @property onDeleteClick lambda be invoked when Delete button is clicked
     */
    data class Loading(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo? = null,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState

    fun copySealed(title: String = this.title): WalletCardState {
        return when (this) {
            is Content -> copy(title = title)
            is Error -> copy(title = title)
            is Loading -> copy(title = title)
            is LockedContent -> copy(title = title)
        }
    }

    companion object {
        val EMPTY_BALANCE_TEXT by lazy { TextReference.Str(value = DASH_SIGN) }
    }
}