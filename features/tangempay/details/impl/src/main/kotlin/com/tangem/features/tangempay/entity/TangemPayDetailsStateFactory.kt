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
import com.tangem.domain.models.StatusSource
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
    private val isRemoveAccountEnabled: Boolean,
    private val isTiersPlusPlanEnabled: Boolean,
) {
    private val notificationFactory = TangemPayDetailsNotificationFactory(
        intents = intents,
        isRemoveAccountEnabled = isRemoveAccountEnabled,
        isTiersPlusPlanEnabled = isTiersPlusPlanEnabled,
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
                    onAddCardClick = {},
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
        val tiersNotification = notificationFactory.createTiersConfig(status.tariffPlan)
        val tiersNotificationType = status.tariffPlan?.let { plan ->
            TangemPayTiersBannerType.fromPlan(isTiersPlusPlanEnabled, plan)
        }
        val issueCardNotificationType = status.cards.resolveProgressBanner().takeIf { type ->
            tiersNotificationType != TangemPayTiersBannerType.TopUpForTierUpgrade ||
                type != CardsProgressBannerUM.Issuing
        }
        val fiatBalance = status.balance.fiatBalance
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
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                isBalanceFlickering = false,
                fiatBalance = DetailsBalanceTransformer.getFiatBalanceText(fiatBalance),
                isMuted = !isFresh,
                isNegative = fiatBalance.availableBalance.signum() < 0,
                isInactive = false,
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
                    onAddCardClick = { intents.onAddCardClick(status.tariffPlan) },
                    isAddCardEnabled = isAddCardEnabled,
                    progressBanner = issueCardNotificationType,
                ),
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            errorNotificationConfig = errorNotification ?: tiersNotification,
            accountDeactivatedNotificationConfig = null,
            cashbackBlockState = null,
        )
    }

    private fun List<TangemPayCard>.resolveProgressBanner(): CardsProgressBannerUM? = when {
        any { it.state == TangemPayCardState.Reissuing } -> CardsProgressBannerUM.Reissuing
        any { it.state == TangemPayCardState.Issuing } -> CardsProgressBannerUM.Issuing
        else -> null
    }

    fun getDeactivatedState(status: PaymentAccountStatusValue.Deactivated): TangemPayDetailsUM {
        val hasWithdrawableBalance: Boolean = status.balance.hasWithdrawableAmount
        val accountDeactivatedNotification = notificationFactory.createAccountDeactivatedConfig()
        val fiatBalance = status.balance.fiatBalance
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
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                isBalanceFlickering = false,
                fiatBalance = DetailsBalanceTransformer.getFiatBalanceText(fiatBalance),
                isMuted = status.source != StatusSource.ACTUAL,
                isNegative = fiatBalance.availableBalance.signum() < 0,
                isInactive = false,
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
        val notification = notificationFactory.createTiersConfig(status.tariffPlan)
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
                    progressBanner = CardsProgressBannerUM.Issuing.takeIf { notification == null },
                    onAddCardClick = {},
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
                val isPlanChanging = tariffPlan.order?.step is TangemPayTariffPlanState.OrderStep.AwaitingDeposit ||
                    tariffPlan.tariff.status == TangemPayCustomerTariffPlan.Status.TRANSITIONING
                add(
                    TangemPayDropDownItemUM(
                        title = resourceReference(R.string.tangempay_current_plan_title),
                        onClick = { intents.onClickCurrentPlan(tariffPlan.tariff) },
                        icon = TangemIconUM.Icon(
                            iconRes = if (isPlanChanging) {
                                CoreUiR.drawable.ic_arrow_refresh_20
                            } else {
                                CoreUiR.drawable.ic_information_24
                            },
                            tintReference = { TangemTheme.colors3.icon.primary },
                        ),
                        subtitle = if (isPlanChanging) {
                            resourceReference(R.string.tangempay_changing_plan)
                        } else {
                            stringReference(tariffPlan.tariff.plan.name)
                        },
                        isEnabled = !isPlanChanging,
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
                    iconResId = R.drawable.ic_arrow_down_24,
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