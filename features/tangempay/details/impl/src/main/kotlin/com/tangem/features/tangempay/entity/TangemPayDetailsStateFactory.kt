package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val cardFrozenState: TangemPayCardFrozenState,
) {

    @Suppress("LongMethod")
    fun getInitialState(cardNumberEnd: String): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = persistentListOf(
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
                ),
            ),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = intents::onRefreshSwipe,
            ),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(
                    ActionButtonConfig(
                        text = resourceReference(id = R.string.tangempay_card_details_add_funds),
                        iconResId = R.drawable.ic_plus_24,
                        onClick = intents::onClickAddFunds,
                        isEnabled = cardFrozenState == TangemPayCardFrozenState.Unfrozen,
                    ),
                    ActionButtonConfig(
                        text = resourceReference(id = R.string.tangempay_card_details_withdraw),
                        iconResId = R.drawable.ic_arrow_up_24,
                        onClick = intents::onClickWithdraw,
                        isEnabled = cardFrozenState == TangemPayCardFrozenState.Unfrozen,
                    ),
                ),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = cardNumberEnd,
                            onClick = intents::onCardClick,
                        ),
                    ),
                    onAddCardClick = intents::onAddCardClick,
                ),
            ),
            isBalanceHidden = false,
            addFundsEnabled = true,
        )
    }
}