package com.tangem.features.commonfeatures.impl.addtoportfolio.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.ChooseNetworkUM
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

private const val DISABLED_ALPHA = 0.4f

@Composable
internal fun ChooseNetworkContent(state: ChooseNetworkUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        ChooseNetworkContentV2(
            state = state,
            modifier = modifier,
        )
    } else {
        ChooseNetworkContentV1(
            state = state,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ChooseNetworkContentV1(state: ChooseNetworkUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.action),
    ) {
        state.networks.fastForEach { model ->
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
internal fun ChooseNetworkContentV2(state: ChooseNetworkUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(34.dp),
            )
            .padding(horizontal = TangemTheme.dimens2.x4, vertical = TangemTheme.dimens2.x2),
    ) {
        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2),
            text = stringResourceSafe(R.string.common_choose_network),
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )

        state.networks.fastForEach { model ->
            key(model.id) {
                TangemRowContainer(
                    modifier = Modifier.clickable(
                        enabled = model.isEnabled,
                        onClick = { state.onNetworkClick(model) },
                    ),
                    contentPadding = PaddingValues(vertical = TangemTheme.dimens2.x3),
                    content = {
                        NetworkIcon(
                            modifier = Modifier
                                .layoutId(layoutId = TangemRowLayoutId.HEAD)
                                .padding(end = TangemTheme.dimens2.x3),
                            model = model,
                        )

                        NetworkText(
                            modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
                            model = model,
                        )
                        if (!model.isEnabled) {
                            TangemBadge(
                                modifier = Modifier
                                    .layoutId(layoutId = TangemRowLayoutId.TAIL)
                                    .padding(start = TangemTheme.dimens2.x2),
                                text = resourceReference(R.string.common_added),
                                size = TangemBadgeSize.X6,
                                shape = TangemBadgeShape.Rounded,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NetworkIcon(model: BlockchainRowUM, modifier: Modifier = Modifier) {
    if (model.isSelected && model.isEnabled) {
        Image(
            modifier = modifier
                .size(TangemTheme.dimens2.x10),
            painter = painterResource(id = model.iconResId),
            contentDescription = null,
        )
    } else {
        Icon(
            modifier = modifier
                .background(
                    color = TangemTheme.colors2.button.backgroundSecondary,
                    shape = CircleShape,
                )
                .size(TangemTheme.dimens2.x10),
            painter = painterResource(id = model.iconResId),
            tint = TangemTheme.colors2.graphic.neutral.tertiary,
            contentDescription = null,
        )
    }
}

@Composable
private fun NetworkText(model: BlockchainRowUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        Text(
            modifier = Modifier.weight(weight = 10f, fill = false),
            text = model.name,
            style = TangemTheme.typography2.bodyMedium16,
            color = when {
                model.isEnabled && model.isMainNetwork -> TangemTheme.colors2.text.neutral.primary
                model.isEnabled -> TangemTheme.colors2.text.neutral.secondary
                else -> TangemTheme.colors2.text.status.disabled
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.weight(weight = 5f, fill = false),
            text = model.type,
            style = TangemTheme.typography2.captionMedium12,
            color = if (model.isEnabled) {
                TangemTheme.colors2.text.neutral.tertiary
            } else {
                TangemTheme.colors2.text.status.disabled
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewV2(@PreviewParameter(ChooseNetworkContentProvider::class) content: ChooseNetworkUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            ChooseNetworkContent(
                state = content,
            )
        }
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