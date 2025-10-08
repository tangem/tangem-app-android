package com.tangem.features.markets.portfolio.add.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.portfolio.add.impl.ui.state.ChooseNetworkUM
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewAddToPortfolioBSContentProvider
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import kotlinx.collections.immutable.toPersistentList

private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun ChooseNetworkContent(state: ChooseNetworkUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.action),
    ) {
        state.networks.fastForEachIndexed { index, model ->
            key(model.id) {
                BlockchainRow(
                    model = model,
                    itemPadding = PaddingValues(
                        horizontal = TangemTheme.dimens.spacing12,
                        vertical = TangemTheme.dimens.spacing14,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (model.isEnabled) it.clickable(onClick = { state.onNetworkClick(model) }) else it },
                ) {
                    if (!model.isEnabled) {
                        Label(
                            modifier = Modifier.alpha(DISABLED_ALPHA),
                            state = LabelUM(
                                text = stringReference("Added"),
                                style = LabelStyle.REGULAR,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview(
    @PreviewParameter(PreviewAddToPortfolioBSContentProvider::class) content: AddToPortfolioBSContentUM,
) {
    TangemThemePreview {
        ChooseNetworkContent(
            state = ChooseNetworkUM(
                networks = content.selectNetworkUM.networks
                    .mapIndexed { index, um -> if (index % 2 == 1) um.copy(isEnabled = false) else um }
                    .toPersistentList(),
                onNetworkClick = {},
            ),
        )
    }
}