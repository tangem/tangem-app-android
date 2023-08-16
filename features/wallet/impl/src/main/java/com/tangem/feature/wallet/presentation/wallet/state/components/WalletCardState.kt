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

    /** Wallet image resource id */
    @get:DrawableRes
    val imageResId: Int?

    /** Lambda be invoked when card is clicked */
    val onClick: (() -> Unit)?

    /** Additional text availability */
    sealed interface AdditionalTextAvailability {

        /** Additional wallet information */
        val additionalInfo: TextReference
    }

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     * @property additionalInfo wallet additional info
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val title: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)? = null,
        override val additionalInfo: TextReference,
        val balance: String,
    ) : WalletCardState, AdditionalTextAvailability

    /**
     * Wallet card loading state
     *
     * @property id         wallet id
     * @property title      wallet name
     * @property imageResId wallet image resource id
     * @property onClick    lambda be invoked when wallet card is clicked
     */
    data class Loading(
        override val id: UserWalletId,
        override val title: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)? = null,
    ) : WalletCardState

    /**
     * Wallet card hidden content state
     *
     * @property id         wallet id
     * @property title      wallet name
     * @property imageResId wallet image resource id
     * @property onClick    lambda be invoked when wallet card is clicked
     */
    data class HiddenContent(
        override val id: UserWalletId,
        override val title: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)?,
    ) : WalletCardState, AdditionalTextAvailability {

        override val additionalInfo: TextReference = HIDDEN_BALANCE_TEXT
    }

    /**
     * Wallet card error state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     * @property additionalInfo wallet additional info
     */
    data class Error(
        override val id: UserWalletId,
        override val title: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)?,
        override val additionalInfo: TextReference = EMPTY_BALANCE_TEXT,
    ) : WalletCardState, AdditionalTextAvailability

    companion object {
        val HIDDEN_BALANCE_TEXT by lazy { TextReference.Str(value = "•••") }
        val EMPTY_BALANCE_TEXT by lazy { TextReference.Str(value = "—") }
    }
}