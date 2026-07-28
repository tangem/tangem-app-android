package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.ds.button.PrimaryInverseTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterUM

@Composable
internal fun FilterButtons(
    earnFilterUM: EarnFilterUM,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterButtonsV2(
        earnFilterUM = earnFilterUM,
        onNetworkFilterClick = onNetworkFilterClick,
        onTypeFilterClick = onTypeFilterClick,
        modifier = modifier,
    )
}

@Composable
private fun FilterButtonsV2(
    earnFilterUM: EarnFilterUM,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        PrimaryInverseTangemButton(
            text = when (earnFilterUM.selectedNetworkFilter) {
                is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                is EarnFilterNetworkUM.Network -> TextReference.Str(earnFilterUM.selectedNetworkFilter.text)
            },
            onClick = onNetworkFilterClick,
            tangemIconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_chewron_down_20,
                tintReference = {
                    if (earnFilterUM.isNetworkFilterEnabled) {
                        TangemTheme.colors2.graphic.neutral.primary
                    } else {
                        TangemTheme.colors2.graphic.neutral.quaternary
                    }
                },
            ),
            iconPosition = com.tangem.core.ui.ds.button.TangemButtonIconPosition.End,
            size = TangemButtonSize.X9,
            shape = TangemButtonShape.Rounded,
            isEnabled = earnFilterUM.isNetworkFilterEnabled,
        )

        SpacerWMax()

        PrimaryInverseTangemButton(
            text = earnFilterUM.selectedTypeFilter.text,
            onClick = onTypeFilterClick,
            tangemIconUM = TangemIconUM.Icon(
                iconRes = R.drawable.ic_chewron_down_20,
                tintReference = {
                    if (earnFilterUM.isTypeFilterEnabled) {
                        TangemTheme.colors2.graphic.neutral.primary
                    } else {
                        TangemTheme.colors2.graphic.neutral.quaternary
                    }
                },
            ),
            iconPosition = com.tangem.core.ui.ds.button.TangemButtonIconPosition.End,
            size = TangemButtonSize.X9,
            shape = TangemButtonShape.Rounded,
            isEnabled = earnFilterUM.isTypeFilterEnabled,
        )
    }
}