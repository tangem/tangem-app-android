package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId

/** Wallet card state */
@Immutable
internal sealed interface WalletCardState {

    /** Id */
    val id: UserWalletId

    /** Title */
    val title: String

    /** Additional text */
    val additionalInfo: TextReference?

    /** Wallet image resource id */
    @get:DrawableRes
    val imageResId: Int?

    /** Lambda be invoked when Rename button is clicked */
    val onRenameClick: (UserWalletId, String) -> Unit

    /** Lambda be invoked when Delete button is clicked */
    val onDeleteClick: (UserWalletId) -> Unit

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: TextReference,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId, String) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
        val balance: String,
    ) : WalletCardState

    /**
     * Wallet card hidden content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     */
    data class HiddenContent(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: TextReference = HIDDEN_BALANCE_TEXT,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId, String) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
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
        override val additionalInfo: TextReference? = null,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId, String) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState

    /**
     * Wallet card error state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     */
    data class Error(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: TextReference = EMPTY_BALANCE_TEXT,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId, String) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState

    /**
     * Wallet card loading state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onRenameClick  lambda be invoked when Rename button is clicked
     * @property onDeleteClick  lambda be invoked when Delete button is clicked
     */
    data class Loading(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: TextReference? = null,
        override val imageResId: Int?,
        override val onRenameClick: (UserWalletId, String) -> Unit,
        override val onDeleteClick: (UserWalletId) -> Unit,
    ) : WalletCardState

    fun copySealed(title: String = this.title): WalletCardState {
        return when (this) {
            is Content -> copy(title = title)
            is Error -> copy(title = title)
            is HiddenContent -> copy(title = title)
            is Loading -> copy(title = title)
            is LockedContent -> copy(title = title)
        }
    }

    companion object {
        val HIDDEN_BALANCE_TEXT by lazy { TextReference.Str(value = "•••") }
        val EMPTY_BALANCE_TEXT by lazy { TextReference.Str(value = "—") }
    }
}