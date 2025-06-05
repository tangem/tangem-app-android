package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChooseProviderBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        titleText = resourceReference(R.string.express_choose_providers_title),
    ) { content: ChooseProviderBottomSheetConfig ->
        ChooseProviderBottomSheetContent(content = content)
    }
}

@Suppress("LongMethod")
@Composable
private fun ChooseProviderBottomSheetContent(content: ChooseProviderBottomSheetConfig) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResourceSafe(R.string.express_choose_providers_subtitle),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .padding(
                    top = 10.dp,
                    bottom = 16.dp,
                )
                .padding(horizontal = TangemTheme.dimens.spacing56),
            textAlign = TextAlign.Center,
        )
        if (content.notification != null) {
            Notification(
                config = content.notification.config,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 12.dp,
                    ),
                iconTint = TangemTheme.colors.icon.warning,
            )
        }

        Column(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 14.dp,
                )
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium),
        ) {
            content.providers.forEach { provider ->
                val isSelected = provider.id == content.selectedProviderId
                ProviderItem(
                    state = provider,
                    isSelected = isSelected,
                    modifier = Modifier
                        .clickable(
                            enabled = provider.onProviderClick != null,
                            onClick = { provider.onProviderClick?.invoke(provider.id) },
                        )
                        .padding(
                            top = TangemTheme.dimens.spacing12,
                            bottom = TangemTheme.dimens.spacing12,
                            end = TangemTheme.dimens.spacing12,
                        ),
                )
            }
        }
        Icon(
            painterResource(id = R.drawable.ic_lightning_16),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
        )
        Text(
            text = stringResourceSafe(R.string.express_more_providers_soon),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing6, bottom = TangemTheme.dimens.spacing16)
                .padding(horizontal = TangemTheme.dimens.spacing56),
            textAlign = TextAlign.Center,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_ChooseProviderBottomSheet() {
    val providers = persistentListOf(
        ProviderState.Content(
            id = "1",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            subtitle = stringReference("1 000 000"),
            additionalBadge = ProviderState.AdditionalBadge.BestTrade,
            percentLowerThenBest = PercentDifference.Value(-1.0f),
            selectionType = ProviderState.SelectionType.SELECT,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = {},
        ),
        ProviderState.Unavailable(
            id = "2",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            selectionType = ProviderState.SelectionType.SELECT,
            alertText = stringReference("Unavailable"),
        ),
    )
    val content = ChooseProviderBottomSheetConfig(
        selectedProviderId = "1",
        notification = NotificationUM.Error(
            title = resourceReference(R.string.warning_express_providers_fca_warning_title),
            subtitle = resourceReference(R.string.warning_express_providers_fca_warning_description),
        ),
        providers = providers,
    )
    TangemThemePreview {
        ChooseProviderBottomSheet(
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = content,
            ),
        )
    }
}
// endregion Preview