package com.tangem.features.tangempay.entity

import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.model.transformers.TangemPayCardFrozenStateConverter
import com.tangem.features.tangempay.utils.TangemPayDetailIntents
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Suppress("LongParameterList")
internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onOpenMenu: () -> Unit,
    private val intents: TangemPayDetailIntents,
    private val cardFrozenState: TangemPayCardFrozenState,
    private val converter: TangemPayCardFrozenStateConverter,
) {

    @Suppress("LongMethod")
    fun getInitialState(): TangemPayDetailsUM {
        val cardFrozenStateItem = when (cardFrozenState) {
            is TangemPayCardFrozenState.Pending -> null
            is TangemPayCardFrozenState.Frozen -> TangemPayDetailsTopBarMenuItem(
                type = TangemPayDetailsTopBarMenuItemType.UnfreezeCard,
                dropdownItem = TangemDropdownMenuItem(
                    title = resourceReference(R.string.tangempay_card_details_unfreeze_card),
                    textColor = themedColor { TangemTheme.colors.text.primary1 },
                    onClick = intents::onClickUnfreezeCard,
                ),
            )
            is TangemPayCardFrozenState.Unfrozen -> TangemPayDetailsTopBarMenuItem(
                type = TangemPayDetailsTopBarMenuItemType.FreezeCard,
                dropdownItem = TangemDropdownMenuItem(
                    title = resourceReference(R.string.tangempay_card_details_freeze_card),
                    textColor = themedColor { TangemTheme.colors.text.primary1 },
                    onClick = intents::onClickFreezeCard,
                ),
            )
        }
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = onBack,
                onOpenMenu = onOpenMenu,
                items = listOfNotNull(
                    TangemPayDetailsTopBarMenuItem(
                        type = TangemPayDetailsTopBarMenuItemType.ChangePin,
                        dropdownItem = TangemDropdownMenuItem(
                            title = resourceReference(R.string.tangempay_card_details_pin_code),
                            textColor = themedColor { TangemTheme.colors.text.primary1 },
                            onClick = intents::onClickPinCode,
                        ),
                    ),
                    TangemPayDetailsTopBarMenuItem(
                        type = TangemPayDetailsTopBarMenuItemType.TermsAndLimits,
                        dropdownItem = TangemDropdownMenuItem(
                            title = resourceReference(R.string.tangem_pay_terms_limits),
                            textColor = themedColor { TangemTheme.colors.text.primary1 },
                            onClick = intents::onClickTermsAndLimits,
                        ),
                    ),
                    cardFrozenStateItem,
                ).toPersistentList(),
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
            ),
            addToWalletBlockState = null,
            isBalanceHidden = false,
            addFundsEnabled = true,
            cardFrozenState = converter.convert(cardFrozenState),
            betaNotificationConfig = NotificationConfig(
                title = resourceReference(R.string.tangem_pay_beta_notification_title),
                subtitle = resourceReference(R.string.tangem_pay_beta_notification_subtitle),
                iconResId = R.drawable.img_visa_notification,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.common_contact_support),
                    onClick = intents::onContactSupportClicked,
                ),
                iconSize = 36.dp,
            ),
        )
    }
}