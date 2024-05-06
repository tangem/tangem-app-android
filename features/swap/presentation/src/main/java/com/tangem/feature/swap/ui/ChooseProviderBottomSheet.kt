package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ChooseProviderBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ChooseProviderBottomSheetConfig ->
        ChooseProviderBottomSheetContent(content = content)
    }
}

@Suppress("LongMethod")
@Composable
private fun ChooseProviderBottomSheetContent(content: ChooseProviderBottomSheetConfig) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.express_choose_providers_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10),
        )
        Text(
            text = stringResource(R.string.express_choose_providers_subtitle),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .padding(horizontal = TangemTheme.dimens.spacing56),
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .padding(
                    top = TangemTheme.dimens.spacing16,
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing14,
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
            text = stringResource(R.string.express_more_providers_soon),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing6, bottom = TangemTheme.dimens.spacing16)
                .padding(horizontal = TangemTheme.dimens.spacing56),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun ChooseProviderBottomSheet_Preview() {
    val providers = listOf(
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
    TangemThemePreview(isDark = false) {
        ChooseProviderBottomSheetContent(
            ChooseProviderBottomSheetConfig(
                selectedProviderId = "1",
                providers = providers.toImmutableList(),
            ),
        )
    }
}
