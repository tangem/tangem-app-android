package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.ImmutableList

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

    /** Wallet drop down items */
    val dropDownItems: ImmutableList<WalletDropDownItems>

    /**
     * Wallet card content state
     *
     * @property id             wallet id
     * @property title          wallet name
     * @property imageResId     wallet image resource id
     * @property dropDownItems  wallet dropdown items
     * @property additionalInfo wallet additional info
     * @property cardCount      number of cards in the wallet
     * @property balance        wallet balance
     */
    data class Content(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo,
        override val imageResId: Int?,
        override val dropDownItems: ImmutableList<WalletDropDownItems>,
        val isBalanceFlickering: Boolean,
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
     * @property dropDownItems  wallet dropdown items
     */
    data class LockedContent(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo,
        override val imageResId: Int?,
        override val dropDownItems: ImmutableList<WalletDropDownItems>,
    ) : WalletCardState

    /**
     * Wallet card error state
     *
     * @property id            wallet id
     * @property title         wallet name
     * @property imageResId    wallet image resource id
     * @property dropDownItems  wallet dropdown items
     */
    data class Error(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo? = defaultAdditionalInfo,
        override val imageResId: Int?,
        override val dropDownItems: ImmutableList<WalletDropDownItems>,
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
     * @property dropDownItems  wallet dropdown items
     */
    data class Loading(
        override val id: UserWalletId,
        override val title: String,
        override val additionalInfo: WalletAdditionalInfo? = null,
        override val imageResId: Int?,
        override val dropDownItems: ImmutableList<WalletDropDownItems>,
    ) : WalletCardState

    fun copySealed(
        title: String = this.title,
        dropDownItems: ImmutableList<WalletDropDownItems> = this.dropDownItems,
    ): WalletCardState {
        return when (this) {
            is Content -> copy(title = title, dropDownItems = dropDownItems)
            is Error -> copy(title = title, dropDownItems = dropDownItems)
            is Loading -> copy(title = title, dropDownItems = dropDownItems)
            is LockedContent -> copy(title = title, dropDownItems = dropDownItems)
        }
    }

    companion object {
        val EMPTY_BALANCE_TEXT by lazy { TextReference.Str(value = DASH_SIGN) }
    }
}