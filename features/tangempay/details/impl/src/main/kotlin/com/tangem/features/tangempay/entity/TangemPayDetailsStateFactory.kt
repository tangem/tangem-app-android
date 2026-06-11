package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val cardFrozenState: TangemPayCardFrozenState,
    private val isRedesignEnabled: Boolean,
) {
    @Suppress("LongMethod")
    fun getInitialState(
        isTangemPayDeactivated: Boolean,
        cardNumberEnd: String,
        isReissuing: Boolean,
        isFrozen: Boolean,
    ): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(isTangemPayDeactivated),
                itemsV2 = getTopBarMenuItemsV2(isTangemPayDeactivated),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = getActionButtonsConfig(),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = cardNumberEnd,
                            onClick = {},
                            isReissuing = isReissuing,
                            isFrozen = isFrozen,
                        ),
                    ),
                    onAddCardClick = intents::onAddCardClick,
                ).takeIf { !isTangemPayDeactivated },
            ),
            isBalanceHidden = false,
            addFundsEnabled = true,
            addToWalletBlockState = null,
            accountDeactivatedNotificationConfig = NotificationConfig(
                title = resourceReference(R.string.tangempay_account_deactivated_message_title),
                subtitle = resourceReference(R.string.tangempay_account_deactivated_message_subtitle),
                iconResId = R.drawable.img_attention_20,
            ).takeIf { isTangemPayDeactivated },
        )
    }

    private fun getTopBarMenuItems(isTangemPayDeactivated: Boolean): ImmutableList<TangemDropdownMenuItem> {
        if (isTangemPayDeactivated) return persistentListOf()

        return persistentListOf(
            TangemDropdownMenuItem(
                title = resourceReference(R.string.tangem_pay_terms_limits),
                textColor = themedColor { TangemTheme.colors.text.primary1 },
                onClick = intents::onClickTermsAndLimits,
            ),
            TangemDropdownMenuItem(
                title = resourceReference(R.string.tangempay_pay_support),
                textColor = themedColor { TangemTheme.colors.text.primary1 },
                onClick = intents::onContactSupportClicked,
            ),
        )
    }

    private fun getTopBarMenuItemsV2(isTangemPayDeactivated: Boolean): ImmutableList<TangemPayDropDownItemUM> {
        if (isTangemPayDeactivated) return persistentListOf()

        return persistentListOf(
            TangemPayDropDownItemUM(
                title = resourceReference(R.string.tangem_pay_terms_limits),
                onClick = intents::onClickTermsAndLimits,
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_document_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
            ),
            TangemPayDropDownItemUM(
                title = resourceReference(R.string.tangempay_pay_support),
                onClick = intents::onContactSupportClicked,
                icon = TangemIconUM.Icon(
                    iconRes = R.drawable.ic_mail_20,
                    tintReference = {
                        TangemTheme.colors3.icon.primary
                    },
                ),
            ),
        )
    }

    private fun getActionButtonsConfig(): ImmutableList<ActionButtonConfig> {
        return persistentListOf(
            ActionButtonConfig(
                text = resourceReference(id = R.string.tangempay_card_details_add_funds),
                iconResId = if (isRedesignEnabled) {
                    R.drawable.ic_arrow_down_24
                } else {
                    R.drawable.ic_plus_24
                },
                onClick = intents::onClickAddFunds,
                isEnabled = cardFrozenState == TangemPayCardFrozenState.Unfrozen,
            ),
            ActionButtonConfig(
                text = resourceReference(id = R.string.tangempay_card_details_withdraw),
                iconResId = R.drawable.ic_arrow_up_24,
                onClick = intents::onClickWithdraw,
                isEnabled = cardFrozenState == TangemPayCardFrozenState.Unfrozen,
            ),
        )
    }
}