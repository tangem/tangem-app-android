package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.selectedBorder
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
    TangemModalBottomSheet<ChooseProviderBottomSheetConfig>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.express_choose_providers_title),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = config.onDismissRequest,
            )
        },
        content = { content ->
            ChooseProviderBottomSheetContent(content = content)
        },
    )
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
                .padding(bottom = 14.dp)
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
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content.providers.forEach { provider ->
                val isSelected = provider.id == content.selectedProviderId
                ProviderItem(
                    state = provider,
                    isSelected = false,
                    modifier = Modifier
                        .selectedBorder(isSelected = isSelected)
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                        .background(color = TangemTheme.colors.background.action)
                        .clickable(
                            enabled = provider.onProviderClick != null,
                            onClick = { provider.onProviderClick?.invoke(provider.id) },
                        )
                        .padding(
                            vertical = 16.dp,
                            horizontal = 2.dp,
                        ),
                )
            }
        }
        SpacerH(6.dp)
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
                .padding(top = 4.dp, bottom = 32.dp)
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