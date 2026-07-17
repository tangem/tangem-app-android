package com.tangem.features.tangempay.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.AddToWalletBlockState
import com.tangem.features.tangempay.entity.CardsProgressBannerUM
import com.tangem.features.tangempay.entity.TangemPayAction
import com.tangem.features.tangempay.entity.TangemPayActionButtonUM
import com.tangem.features.tangempay.entity.TangemPayCardUiState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayDetailsUM>(
    collection = listOf(
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = {},
                onOpenMenu = {},
                items = persistentListOf(),
                itemsV2 = persistentListOf(),
            ),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Content(
                actionButtons = persistentListOf(
                    TangemPayActionButtonUM(
                        action = TangemPayAction.AddFunds,
                        config = ActionButtonConfig(
                            text = resourceReference(id = R.string.common_receive),
                            iconResId = R.drawable.ic_arrow_down_24,
                            onClick = {},
                        ),
                    ),
                ),
                fiatBalance = combinedReference(
                    stringReference("1,234"),
                    styledStringReference(
                        ".56",
                        { SpanStyle(color = TangemTheme.colors3.text.primary) },
                    ),
                    stringReference(" $"),
                ),
                isBalanceFlickering = false,
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "1234",
                            imageUrl = null,
                            onClick = {},
                            isEnabled = false,
                            isFrozen = false,
                            state = TangemPayCardUiState.Active,
                        ),
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "3456",
                            imageUrl = null,
                            onClick = {},
                            isEnabled = true,
                            isFrozen = false,
                            state = TangemPayCardUiState.Active,
                        ),
                    ),
                    onAddCardClick = {},
                    isAddCardEnabled = true,
                ),
                isNegative = false,
                isInactive = false,
            ),
            isBalanceHidden = false,
            addToWalletBlockState = AddToWalletBlockState(
                onClick = {},
                onClickClose = {},
            ),
            accountDeactivatedNotificationConfig = null,
            errorNotificationConfig = NotificationConfig(
                title = stringReference("Your account has been closed"),
                subtitle = stringReference("For questions about account, please contact support"),
                iconResId = R.drawable.ic_alert_circle_24,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = stringReference("Remove account"),
                    onClick = {},
                ),
            ),
        ),
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = {},
                onOpenMenu = {},
                items = persistentListOf(),
                itemsV2 = persistentListOf(),
            ),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
                actionButtons = persistentListOf(),
                cardsBlockState = TangemPayDetailsBalanceBlockState.CardsBlockState(
                    cards = persistentListOf(
                        TangemPayDetailsBalanceBlockState.Card(
                            lastDigits = "1234",
                            imageUrl = null,
                            onClick = {},
                            isFrozen = false,
                            isEnabled = true,
                            state = TangemPayCardUiState.InProgress,
                        ),
                    ),
                    onAddCardClick = {},
                    isAddCardEnabled = true,
                    progressBanner = CardsProgressBannerUM.Reissuing,
                ),
            ),
            isBalanceHidden = false,
            addToWalletBlockState = null,
            accountDeactivatedNotificationConfig = null,
            errorNotificationConfig = NotificationConfig(
                title = TextReference.Str("Error title"),
                subtitle = TextReference.Str("Error subtitle"),
                iconResId = R.drawable.ic_alert_circle_24,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = TextReference.Str("Error btn text"),
                    onClick = {},
                    iconResId = R.drawable.ic_tangem_24,
                ),
            ),
        ),
    ),
)

internal class TangemPayDetailsTxHistoryProvider : CollectionPreviewParameterProvider<TangemPayTxHistoryUM>(
    collection = listOf(
        PreviewTangemPayTxHistoryComponent.loadingUM,
        PreviewTangemPayTxHistoryComponent.contentUM,
        PreviewTangemPayTxHistoryComponent.emptyUM,
        PreviewTangemPayTxHistoryComponent.errorUM,
    ),
)