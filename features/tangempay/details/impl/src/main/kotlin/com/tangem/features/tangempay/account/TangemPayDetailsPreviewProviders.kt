package com.tangem.features.tangempay.account

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.txhistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.tangempay.txhistory.TangemPayTxHistoryUM
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayDetailsUM>(
    collection = listOf(
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = {},
                onOpenMenu = {},
                items = persistentListOf(),
                subtitle = resourceReference(R.string.tangempay_multinetwork),
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
            accountDeactivatedBannerState = TangemMessageBanner.State(
                title = resourceReference(R.string.tangempay_account_deactivated_message_title),
                variant = TangemMessageBanner.Variant.Error,
                description = resourceReference(R.string.tangempay_account_deactivated_message_subtitle),
                secondaryButton = TangemMessageBanner.Button(
                    text = resourceReference(R.string.tangempay_remove_account),
                    onClick = {},
                ),
                iconStart = TangemIconUM.Icon(
                    imageVector = Icons.ic_error_20,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
            ),
            errorNotificationConfig = null,
        ),
        TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(
                onBackClick = {},
                onOpenMenu = {},
                items = persistentListOf(),
                subtitle = resourceReference(R.string.tangempay_multinetwork),
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
            accountDeactivatedBannerState = null,
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