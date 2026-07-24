package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_20
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterUM

@Composable
internal fun FilterButtons(
    earnFilterUM: EarnFilterUM,
    onNetworkFilterClick: () -> Unit,
    onTypeFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            text = when (earnFilterUM.selectedNetworkFilter) {
                is EarnFilterNetworkUM.AllNetworks -> TextReference.Res(R.string.earn_filter_all_networks)
                is EarnFilterNetworkUM.MyNetworks -> TextReference.Res(R.string.earn_filter_my_networks)
                is EarnFilterNetworkUM.Network -> TextReference.Str(earnFilterUM.selectedNetworkFilter.text)
            },
            onClick = onNetworkFilterClick,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_chevron_down_20),
            isEnabled = earnFilterUM.isNetworkFilterEnabled,
            size = TangemButton.Size.X9,
        )

        SpacerWMax()

        TangemButton(
            variant = TangemButton.Variant.Secondary,
            text = earnFilterUM.selectedTypeFilter.text,
            onClick = onTypeFilterClick,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_chevron_down_20),
            isEnabled = earnFilterUM.isTypeFilterEnabled,
            size = TangemButton.Size.X9,
        )
    }
}