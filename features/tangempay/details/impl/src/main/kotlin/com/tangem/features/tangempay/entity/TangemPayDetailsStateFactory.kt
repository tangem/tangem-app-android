package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isFrozen
import com.tangem.domain.models.pay.thumbnailUrl
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.model.transformers.DetailsBalanceTransformer
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import com.tangem.features.tangempay.utils.hasWithdrawableAmount
import com.tangem.features.tangempay.utils.isFresh
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList", "LargeClass")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val isRedesignEnabled: Boolean,
    private val isRemoveAccountEnabled: Boolean,
    private val isTiersPlusPlanEnabled: Boolean,
) {
    private val notificationFactory = TangemPayDetailsNotificationFactory(
        intents = intents,
        isRemoveAccountEnabled = isRemoveAccountEnabled,
        isTiersPlusPlanEnabled = isTiersPlusPlanEnabled,
        isRedesignEnabled = isRedesignEnabled,
    )

    fun getLoadingState(): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(),
                itemsV2 = getTopBarMenuItemsV2(tariffPlan = null),
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
            cashbackBlockState = null,
        )
    }

    fun getLoadedState(status: PaymentAccountStatusValue.Loaded): TangemPayDetailsUM {
        val isFresh = status.isFresh
        val hasUnfrozenCard = status.cards.any { it.frozenState == TangemPayCardFrozenState.Unfrozen }
        val hasIssuingCard = status.cards.any { it.state == TangemPayCardState.Issuing }
        val isAddCardEnabled = isFresh && !hasIssuingCard
        val areActionButtonsEnabled = isFresh && hasUnfrozenCard
        val hasWithdrawableBalance = status.balance.hasWithdrawableAmount
        val errorNotification = notificationFactory.createErrorConfig(status.error)
        val awaitingDepositNotification = notificationFactory.createAwaitingDepositConfig(status.tariffPlan)
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(),
                itemsV2 = getTopBarMenuItemsV2(tariffPlan = status.tariffPlan),
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
                        .map { cardItem ->
                            TangemPayDetailsBalanceBlockState.Card(
                                lastDigits = cardItem.lastDigits,
                                imageUrl = cardItem.thumbnailUrl,
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
            errorNotificationConfig = errorNotification ?: awaitingDepositNotification,
            accountDeactivatedNotificationConfig = null,
            cashbackBlockState = null,
        )
    }

    private fun List<TangemPayCard>.resolveProgressBanner(): CardsProgressBannerUM? = when {
        any { it.state == TangemPayCardState.Reissuing } -> CardsProgressBannerUM.Reissuing
        any { it.state == TangemPayCardState.Issuing } -> CardsProgressBannerUM.Issuing
        else -> null
    }

    fun getDeactivatedState(hasWithdrawableBalance: Boolean): TangemPayDetailsUM {
        val accountDeactivatedNotification = notificationFactory.createAccountDeactivatedConfig(isRedesignEnabled)
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
            accountDeactivatedNotificationConfig = accountDeactivatedNotification,
            cashbackBlockState = null,
        )
    }

    fun getInactiveState(status: PaymentAccountStatusValue.Inactive): TangemPayDetailsUM {
        val notification = notificationFactory.createAwaitingDepositConfig(status.tariffPlan)
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(),
                itemsV2 = getTopBarMenuItemsV2(tariffPlan = null),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = getActionButtonsConfig(
                    isAddFundsEnabled = true,
                    isWithdrawEnabled = false,
                ),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "",
                            imageUrl = null,
                            onClick = {},
                            isEnabled = false,
                            isFrozen = false,
                            state = TangemPayCardUiState.InProgress,
                        ),
                    ),
                    onAddCardClick = intents::onAddCardClick,
                    isAddCardEnabled = false,
                ),
                fiatBalance = DetailsBalanceTransformer.getFiatBalanceText(status.fiatBalance),
                isInactive = true,
                isNegative = false,
                isBalanceFlickering = false,
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            errorNotificationConfig = notification,
            accountDeactivatedNotificationConfig = null,
            cashbackBlockState = null,
        )
    }

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

    private fun getTopBarMenuItemsV2(tariffPlan: TangemPayTariffPlanState?): ImmutableList<TangemPayDropDownItemUM> {
        return buildList {
            if (isTiersPlusPlanEnabled && tariffPlan != null) {
                add(
                    TangemPayDropDownItemUM(
                        title = resourceReference(R.string.tangempay_current_plan_title),
                        onClick = { intents.onClickCurrentPlan(tariffPlan.tariff) },
                        icon = TangemIconUM.Icon(
                            iconRes = CoreUiR.drawable.ic_information_24,
                            tintReference = { TangemTheme.colors3.icon.primary },
                        ),
                        subtitle = stringReference(tariffPlan.tariff.plan.name),
                        isEnabled = tariffPlan.order?.step !is TangemPayTariffPlanState.OrderStep.AwaitingDeposit &&
                            tariffPlan.tariff.status != TangemPayCustomerTariffPlan.Status.TRANSITIONING,
                    ),
                )
            }
            if (isTiersPlusPlanEnabled && tariffPlan != null && !tariffPlan.tariff.plan.isBasicTier) {
                add(
                    TangemPayDropDownItemUM(
                        title = resourceReference(R.string.tangempay_visa_benefits),
                        onClick = intents::onClickVisaBenefits,
                        icon = TangemIconUM.Icon(
                            iconRes = CoreUiR.drawable.ic_heart_20,
                            tintReference = { TangemTheme.colors3.icon.primary },
                        ),
                    ),
                )
            }
            add(
                TangemPayDropDownItemUM(
                    title = resourceReference(R.string.tangem_pay_terms_limits),
                    onClick = intents::onClickTermsAndLimits,
                    icon = TangemIconUM.Icon(
                        imageVector = Icons.ic_document_20,
                        tintReference = { TangemTheme.colors3.icon.primary },
                    ),
                ),
            )
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
        }.toImmutableList()
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