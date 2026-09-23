package com.tangem.features.tangempay.account

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.feeCurrencyOrDefault
import com.tangem.domain.models.account.isPlanTransitioningState
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.domain.models.pay.isAwaitingActivation
import com.tangem.domain.models.pay.isFrozen
import com.tangem.domain.models.pay.thumbnailUrl
import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import com.tangem.features.tangempay.common.canAddFunds
import com.tangem.features.tangempay.common.hasWithdrawableAmount
import com.tangem.features.tangempay.common.isFresh
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import com.tangem.core.ui.R as CoreUiR

@Suppress("LongParameterList", "LargeClass")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val isTiersPlusPlanEnabled: Boolean,
    private val isMultichainEnabled: Boolean,
) {
    private val notificationFactory = TangemPayDetailsNotificationFactory(
        intents = intents,
        isTiersPlusPlanEnabled = isTiersPlusPlanEnabled,
    )

    private val subtitle = resourceReference(
        if (isMultichainEnabled) R.string.tangempay_multinetwork else R.string.tangempay_usdc_on_polygon_network,
    )

    fun getLoadingState(): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(tariffPlan = null),
                subtitle = subtitle,
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
            errorNotificationConfig = null,
            statusBannerState = null,
            cashbackBlockState = null,
        )
    }

    fun getLoadedState(status: PaymentAccountStatusValue.Loaded): TangemPayDetailsUM {
        val isFresh = status.isFresh
        val hasUnfrozenCard = status.cards.any { it.frozenState == TangemPayCardFrozenState.Unfrozen }
        val hasIssuingCard = status.cards.any { it.state == TangemPayCardState.Issuing }
        val isAddCardEnabled = isFresh && !hasIssuingCard
        val areActionButtonsEnabled = isFresh && hasUnfrozenCard
        val balance = status.balance
        val hasWithdrawableBalance = status.hasWithdrawableAmount
        val canAddFunds = status.canAddFunds()
        val errorNotification = notificationFactory.createErrorConfig(status.error)
        val tiersNotification = notificationFactory.createTiersConfig(status.tariffPlan)
        val tiersNotificationType = status.tariffPlan?.let { plan ->
            TangemPayTiersBannerType.fromPlan(isTiersPlusPlanEnabled, plan)
        }
        val issueCardNotificationType = status.cards.resolveProgressBanner().takeIf { type ->
            tiersNotificationType != TangemPayTiersBannerType.TopUpForTierUpgrade ||
                type != CardsProgressBannerUM.Issuing
        }
        val cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
            cards = status.cards
                .map { cardItem ->
                    val isAwaitingActivation = cardItem.state.isAwaitingActivation
                    TangemPayDetailsBalanceBlockState.Card(
                        lastDigits = if (isAwaitingActivation) "" else cardItem.lastDigits,
                        imageUrl = if (isAwaitingActivation) null else cardItem.thumbnailUrl,
                        cardType = cardItem.cardType,
                        onClick = { intents.onCardClick(cardItem.id) },
                        isEnabled = status.error == null,
                        isFrozen = cardItem.isFrozen && !isAwaitingActivation,
                        state = cardItem.state.toUiState(),
                    )
                }
                .toImmutableList(),
            onAddCardClick = { intents.onAddCardClick(status.tariffPlan) },
            isAddCardEnabled = isAddCardEnabled,
            progressBanner = issueCardNotificationType,
        )
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(tariffPlan = status.tariffPlan),
                subtitle = subtitle,
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = getBalanceBlockState(
                fiatBalance = balance?.fiatBalance,
                cardsBlockState = cardsBlockState,
                isMuted = !isFresh,
                isAddFundsEnabled = areActionButtonsEnabled && canAddFunds,
                isWithdrawEnabled = areActionButtonsEnabled && hasWithdrawableBalance,
            ),
            isBalanceHidden = false,
            errorNotificationConfig = errorNotification ?: tiersNotification,
            statusBannerState = null,
            cashbackBlockState = null,
        )
    }

    /**
     * Balance block for an account-bearing status. A `null` [fiatBalance] means the account is operational but
     * its balances are unavailable: render the placeholder [TangemPayDetailsBalanceBlockState.Error] instead of
     * a fabricated zero and keep the money actions out of reach.
     */
    private fun getBalanceBlockState(
        fiatBalance: PaymentAccountStatusValue.FiatBalance?,
        cardsBlockState: TangemPayDetailsBalanceBlockState.CardsBlockState?,
        isMuted: Boolean,
        isAddFundsEnabled: Boolean,
        isWithdrawEnabled: Boolean,
    ): TangemPayDetailsBalanceBlockState {
        if (fiatBalance == null) {
            return TangemPayDetailsBalanceBlockState.Error(
                actionButtons = getActionButtonsConfig(isAddFundsEnabled = false, isWithdrawEnabled = false),
                cardsBlockState = cardsBlockState,
            )
        }
        return TangemPayDetailsBalanceBlockState.Content(
            isBalanceFlickering = false,
            fiatBalance = DetailsBalanceTransformer.getFiatBalanceText(fiatBalance),
            isMuted = isMuted,
            isNegative = fiatBalance.availableBalance.signum() < 0,
            isInactive = false,
            actionButtons = getActionButtonsConfig(
                isAddFundsEnabled = isAddFundsEnabled,
                isWithdrawEnabled = isWithdrawEnabled,
            ),
            cardsBlockState = cardsBlockState,
        )
    }

    private fun List<TangemPayCard>.resolveProgressBanner(): CardsProgressBannerUM? {
        val deliveringCards = filter { it.state == TangemPayCardState.Delivering }
        return when {
            any { it.state == TangemPayCardState.Reissuing } -> CardsProgressBannerUM.Reissuing
            any { it.state == TangemPayCardState.Issuing } -> CardsProgressBannerUM.Issuing
            any { it.state == TangemPayCardState.Activating } -> CardsProgressBannerUM.Activating
            deliveringCards.size == 1 -> {
                val card = deliveringCards.first()
                CardsProgressBannerUM.Delivering(
                    onActivateClick = { intents.onActivateCardClick(card.id) }.takeUnless { card.isPlaceholder },
                )
            }
            deliveringCards.size > 1 -> CardsProgressBannerUM.DeliveringMultiple
            else -> null
        }
    }

    fun getDeactivatedState(status: PaymentAccountStatusValue.Deactivated): TangemPayDetailsUM {
        val balance = status.balance
        val hasWithdrawableBalance = status.hasWithdrawableAmount
        val accountDeactivatedBanner = notificationFactory.createAccountDeactivatedBannerState()
        val fiatBalance = balance?.fiatBalance
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getDeactivatedMenuItems(),
                subtitle = subtitle,
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = getBalanceBlockState(
                fiatBalance = fiatBalance,
                cardsBlockState = null,
                isMuted = status.source != StatusSource.ACTUAL,
                isAddFundsEnabled = status.canAddFunds(),
                isWithdrawEnabled = hasWithdrawableBalance,
            ),
            isBalanceHidden = false,
            errorNotificationConfig = null,
            statusBannerState = MessageBannerUM(state = accountDeactivatedBanner),
            cashbackBlockState = null,
        )
    }

    fun getInactiveState(status: PaymentAccountStatusValue.Inactive): TangemPayDetailsUM {
        val notification = notificationFactory.createTiersConfig(status.tariffPlan)
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(tariffPlan = null),
                subtitle = subtitle,
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = getActionButtonsConfig(
                    isAddFundsEnabled = false,
                    isWithdrawEnabled = false,
                ),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "",
                            imageUrl = null,
                            cardType = TangemPayCardType.UNDEFINED,
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
            errorNotificationConfig = notification,
            statusBannerState = null,
            cashbackBlockState = null,
        )
    }

    fun getCardIssueFailedState(
        status: PaymentAccountStatusValue.Error.CardIssueFailed,
        isBannerDismissed: Boolean,
    ): TangemPayDetailsUM {
        val tariffPlan = status.tariffPlan
        val fiatBalance = status.balance?.fiatBalance ?: zeroBalanceOf(tariffPlan)
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = getTopBarMenuItems(tariffPlan = tariffPlan),
                subtitle = subtitle,
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = getActionButtonsConfig(
                    isAddFundsEnabled = status.canAddFunds(),
                    isWithdrawEnabled = false,
                ),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(),
                    onAddCardClick = { intents.onAddCardClick(tariffPlan) },
                    isAddCardEnabled = true,
                ),
                fiatBalance = DetailsBalanceTransformer.getFiatBalanceText(fiatBalance),
                isInactive = false,
                isNegative = fiatBalance.availableBalance.signum() < 0,
                isBalanceFlickering = false,
            ),
            isBalanceHidden = false,
            errorNotificationConfig = null,
            statusBannerState = MessageBannerUM(
                state = notificationFactory.createCardIssueFailedBannerState(),
                onClose = intents::onCardIssueFailedBannerDismissed,
            ).takeUnless { isBannerDismissed },
            cashbackBlockState = null,
        )
    }

    private fun zeroBalanceOf(tariffPlan: TangemPayTariffPlanState?) = PaymentAccountStatusValue.FiatBalance(
        availableBalance = BigDecimal.ZERO,
        currency = tariffPlan?.tariff?.plan?.feeCurrencyOrDefault(FALLBACK_FIAT_CURRENCY)
            ?: FALLBACK_FIAT_CURRENCY,
    )

    private fun getDeactivatedMenuItems(): ImmutableList<TangemPayDropDownItemUM> {
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
        }.toImmutableList()
    }

    private fun getTopBarMenuItems(tariffPlan: TangemPayTariffPlanState?): ImmutableList<TangemPayDropDownItemUM> {
        return buildList {
            if (isTiersPlusPlanEnabled && tariffPlan != null) {
                val isPlanChanging = tariffPlan.isPlanTransitioningState
                add(
                    TangemPayDropDownItemUM(
                        title = resourceReference(R.string.tangempay_current_plan_title),
                        onClick = { intents.onClickCurrentPlan(tariffPlan) },
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

    private companion object {
        const val FALLBACK_FIAT_CURRENCY = "USD"
    }
}