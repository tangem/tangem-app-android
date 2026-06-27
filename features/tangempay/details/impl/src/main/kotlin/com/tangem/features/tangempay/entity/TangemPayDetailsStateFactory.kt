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
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isFrozen
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.utils.hasWithdrawableAmount
import com.tangem.features.tangempay.utils.isFresh
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val isRedesignEnabled: Boolean,
    private val isRemoveAccountEnabled: Boolean,
    private val isMultipleCardsEnabled: Boolean,
) {
    fun getLoadingState(): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(),
                itemsV2 = getTopBarMenuItemsV2(),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(),
                    onAddCardClick = intents::onAddCardClick,
                    isAddCardEnabled = false,
                ),
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            errorNotificationConfig = null,
            accountDeactivatedNotificationConfig = null,
        )
    }

    fun getLoadedState(status: PaymentAccountStatusValue.Loaded): TangemPayDetailsUM {
        val isFresh = status.isFresh
        val hasUnfrozenCard = status.cards.any { it.frozenState == TangemPayCardFrozenState.Unfrozen }
        val hasIssuingCard = status.cards.any { it.state == TangemPayCardState.Issuing }
        val isAddCardEnabled = isFresh && !hasIssuingCard
        val areActionButtonsEnabled = isFresh && hasUnfrozenCard
        val hasWithdrawableBalance = status.balance.hasWithdrawableAmount
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(),
                itemsV2 = getTopBarMenuItemsV2(),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = getActionButtonsConfig(
                    isAddFundsEnabled = areActionButtonsEnabled,
                    isWithdrawEnabled = areActionButtonsEnabled && hasWithdrawableBalance,
                ),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = status.cards
                        .let { if (isMultipleCardsEnabled) it else it.take(1) }
                        .map { cardItem ->
                            TangemPayDetailsBalanceBlockState.Card(
                                lastDigits = cardItem.lastDigits,
                                onClick = { intents.onCardClick(cardItem.id) },
                                isEnabled = status.error == null,
                                isFrozen = cardItem.isFrozen,
                                state = cardItem.state.toUiState(),
                            )
                        }
                        .toImmutableList(),
                    onAddCardClick = intents::onAddCardClick,
                    isAddCardEnabled = isAddCardEnabled,
                    progressBanner = status.cards.resolveProgressBanner(),
                ),
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            errorNotificationConfig = when (status.error) {
                null -> null
                PaymentAccountStatusValue.Error.NotSynced -> createRenewSessionNotificationConfig(isRedesignEnabled)
                else -> createAccountUnavailableConfig(isRedesignEnabled)
            },
            accountDeactivatedNotificationConfig = null,
        )
    }

    private fun List<TangemPayCard>.resolveProgressBanner(): CardsProgressBannerUM? = when {
        any { it.state == TangemPayCardState.Reissuing } -> CardsProgressBannerUM.Reissuing
        any { it.state == TangemPayCardState.Issuing } -> CardsProgressBannerUM.Issuing
        else -> null
    }

    fun getDeactivatedState(hasWithdrawableBalance: Boolean): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getDeactivatedMenuItems(),
                itemsV2 = getDeactivatedMenuItemsV2(),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = getActionButtonsConfig(
                    isAddFundsEnabled = true,
                    isWithdrawEnabled = hasWithdrawableBalance,
                ),
                cardsBlockState = null,
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            errorNotificationConfig = null,
            accountDeactivatedNotificationConfig = createAccountDeactivatedConfig(isRedesignEnabled),
        )
    }

    private fun createAccountUnavailableConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_temporarily_unavailable),
        subtitle = resourceReference(R.string.tangempay_service_unreachable_try_later),
        iconResId = if (isRedesignEnabled) R.drawable.ic_alert_circle_24 else R.drawable.img_attention_20,
    )

    private fun createAccountDeactivatedConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_account_deactivated_message_title),
        subtitle = resourceReference(R.string.tangempay_account_deactivated_message_subtitle),
        iconResId = if (isRedesignEnabled) R.drawable.ic_alert_circle_24 else R.drawable.img_attention_20,
        buttonsState = if (isRemoveAccountEnabled) {
            NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.tangempay_remove_account),
                onClick = intents::onRemoveAccount,
            )
        } else {
            null
        },
    )

    private fun createRenewSessionNotificationConfig(isRedesignEnabled: Boolean) = NotificationConfig(
        title = resourceReference(R.string.tangempay_sync_needed_title),
        subtitle = resourceReference(R.string.tangempay_sync_needed_body),
        iconResId = if (isRedesignEnabled) 0 else R.drawable.img_attention_20,
        buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
            text = resourceReference(R.string.tangempay_sync_needed_button),
            onClick = intents::onRenewSession,
            iconResId = R.drawable.ic_tangem_24,
        ),
    )

    private fun getTopBarMenuItems(): ImmutableList<TangemDropdownMenuItem> {
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

    private fun getDeactivatedMenuItems(): ImmutableList<TangemDropdownMenuItem> {
        return buildList {
            add(
                TangemDropdownMenuItem(
                    title = resourceReference(R.string.tangempay_pay_support),
                    textColor = themedColor { TangemTheme.colors.text.primary1 },
                    onClick = intents::onContactSupportClicked,
                ),
            )
            if (isRemoveAccountEnabled) {
                add(
                    TangemDropdownMenuItem(
                        title = resourceReference(R.string.tangempay_remove_account),
                        textColor = themedColor { TangemTheme.colors.text.warning },
                        onClick = intents::onRemoveAccount,
                    ),
                )
            }
        }.toImmutableList()
    }

    private fun getDeactivatedMenuItemsV2(): ImmutableList<TangemPayDropDownItemUM> {
        return buildList {
            add(
                TangemPayDropDownItemUM(
                    title = resourceReference(R.string.tangempay_pay_support),
                    onClick = intents::onContactSupportClicked,
                    icon = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_mail_20,
                        tintReference = { TangemTheme.colors3.icon.primary },
                    ),
                ),
            )
            if (isRemoveAccountEnabled) {
                add(
                    TangemPayDropDownItemUM(
                        title = resourceReference(R.string.tangempay_remove_account),
                        onClick = intents::onRemoveAccount,
                        icon = TangemIconUM.Icon(
                            iconRes = CoreUiR.drawable.ic_trash_24,
                            tintReference = { TangemTheme.colors3.icon.status.error },
                        ),
                        titleColor = { TangemTheme.colors3.text.status.error },
                    ),
                )
            }
        }.toImmutableList()
    }

    private fun getTopBarMenuItemsV2(): ImmutableList<TangemPayDropDownItemUM> {
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

    fun getActionButtonsConfig(
        isAddFundsEnabled: Boolean,
        isWithdrawEnabled: Boolean,
    ): ImmutableList<TangemPayActionButtonUM> {
        return persistentListOf(
            TangemPayActionButtonUM(
                action = TangemPayAction.AddFunds,
                config = ActionButtonConfig(
                    text = resourceReference(id = R.string.tangempay_card_details_add_funds),
                    iconResId = if (isRedesignEnabled) {
                        R.drawable.ic_arrow_down_24
                    } else {
                        R.drawable.ic_plus_24
                    },
                    onClick = intents::onClickAddFunds,
                    isEnabled = isAddFundsEnabled,
                ),
            ),
            TangemPayActionButtonUM(
                action = TangemPayAction.Withdraw,
                config = ActionButtonConfig(
                    text = resourceReference(id = R.string.tangempay_card_details_withdraw),
                    iconResId = R.drawable.ic_arrow_up_24,
                    onClick = intents::onClickWithdraw,
                    isEnabled = isWithdrawEnabled,
                ),
            ),
        )
    }
}