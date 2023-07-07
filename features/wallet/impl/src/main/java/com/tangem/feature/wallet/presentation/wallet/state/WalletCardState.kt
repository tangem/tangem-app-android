package com.tangem.feature.wallet.presentation.wallet.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/** Wallet card state */
@Immutable
internal sealed interface WalletCardState {

    /** Id */
    val id: String

    /** Title */
    val title: String

    /** Additional wallet information */
    val additionalInfo: String

    /** Wallet image resource id */
    @get:DrawableRes
    val imageResId: Int?

    /** Lambda be invoked when card is clicked */
    val onClick: (() -> Unit)?

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     * @property balance        wallet balance
     */
    data class Content(
        override val id: String,
        override val title: String,
        override val additionalInfo: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)? = null,
        val balance: String,
    ) : WalletCardState

    /**
     * Wallet card loading state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     */
    data class Loading(
        override val id: String,
        override val title: String,
        override val additionalInfo: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)? = null,
    ) : WalletCardState

    /**
     * Wallet card hidden content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     */
    data class HiddenContent(
        override val id: String,
        override val title: String,
        override val additionalInfo: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)?,
    ) : WalletCardState

    /**
     * Wallet card error state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property additionalInfo wallet additional info
     * @property imageResId     wallet image resource id
     * @property onClick        lambda be invoked when wallet card is clicked
     */
    data class Error(
        override val id: String,
        override val title: String,
        override val additionalInfo: String,
        override val imageResId: Int?,
        override val onClick: (() -> Unit)?,
    ) : WalletCardState
}
