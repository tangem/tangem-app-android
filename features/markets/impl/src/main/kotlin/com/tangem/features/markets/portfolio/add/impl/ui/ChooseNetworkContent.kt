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
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.impl.ui.state.ChooseNetworkUM
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

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
                        .clickable(enabled = model.isEnabled, onClick = { state.onNetworkClick(model) }),
                ) {
                    if (!model.isEnabled) {
                        Label(
                            modifier = Modifier.alpha(DISABLED_ALPHA),
                            state = LabelUM(
                                text = resourceReference(R.string.common_added),
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
private fun Preview(@PreviewParameter(ChooseNetworkContentProvider::class) content: ChooseNetworkUM) {
    TangemThemePreview {
        ChooseNetworkContent(
            state = content,
        )
    }
}

internal class ChooseNetworkContentProvider : PreviewParameterProvider<ChooseNetworkUM> {

    private val blockchainRow = BlockchainRowUM(
        id = UUID.randomUUID().toString(),
        name = "Etherium 3",
        type = "TEST",
        iconResId = R.drawable.img_eth_22,
        isMainNetwork = false,
        isSelected = true,
        isEnabled = true,
    )

    override val values: Sequence<ChooseNetworkUM>
        get() = sequenceOf(
            ChooseNetworkUM(
                onNetworkClick = {},
                networks = persistentListOf(
                    blockchainRow.copy(
                        type = "MAIN",
                        isMainNetwork = true,
                    ),
                    blockchainRow.copy(
                        iconResId = R.drawable.ic_bsc_16,
                        isEnabled = false,
                    ),
                    blockchainRow.copy(iconResId = R.drawable.img_polygon_22),
                    blockchainRow.copy(iconResId = R.drawable.img_optimism_22),
                ),
            ),
        )
}