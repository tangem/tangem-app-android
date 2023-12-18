package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
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

@Composable
private fun ChooseProviderBottomSheetContent(content: ChooseProviderBottomSheetConfig) {
    Column {
        Text(
            text = stringResource(R.string.express_choose_providers_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = stringResource(R.string.express_choose_providers_subtitle),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .padding(horizontal = TangemTheme.dimens.spacing56)
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing16)
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
                        .padding(TangemTheme.dimens.spacing12),
                )
            }
        }
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
            percentLowerThenBest = -1.0f,
            selectionType = ProviderState.SelectionType.SELECT,
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
    TangemTheme(isDark = false) {
        ChooseProviderBottomSheetContent(
            ChooseProviderBottomSheetConfig(
                selectedProviderId = "1",
                providers = providers.toImmutableList(),
            ),
        )
    }
}