package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.ProviderState
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ChooseProviderBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: ChooseProviderBottomSheetConfig ->
        ChooseProviderBottomSheetContent(content = content)
    }
}

@Composable
private fun ChooseProviderBottomSheetContent(content: ChooseProviderBottomSheetConfig) {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
    ) {
        Text(
            text = "Choose provider",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = "Providers facilitate transactions, ensuring smooth and efficient token exchanges",
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
                ),
        ) {
            content.providers.forEach {
                val isSelected = it.id == content.selectedProviderId
                ProviderItem(
                    state = it,
                    isSelected = isSelected,
                    modifier = Modifier.padding(
                        horizontal = TangemTheme.dimens.spacing12,
                        vertical = TangemTheme.dimens.spacing12,
                    ),
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
            rate = "1 000 000",
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
            alertText = "Unavailable",
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
