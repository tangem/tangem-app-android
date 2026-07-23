package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.preview.SecurityScorePreviewData
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreBottomSheetContent

@Composable
internal fun SecurityScoreBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<SecurityScoreBottomSheetContent>(
        config = config,
        type = TangemBottomSheetType.Modal,
        title = { content ->
            TangemTopNavigation(
                title = content.title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                blurBackground = false,
                onClose = config.onDismissRequest,
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = content.description.resolveReference(),
                    style = TangemTheme.typography3.body.medium.copy(
                        color = TangemTheme.colors3.text.secondary,
                    ),
                )
                SpacerH12()
                content.providers.fastForEach { provider ->
                    SecurityScoreProviderRow(
                        providerUM = provider,
                        onLinkClick = { content.onProviderLinkClick(provider) },
                    )
                    SpacerH(8.dp)
                }

                SpacerH16()
                SpacerH(bottomBarHeight)
            }
        },
    )
}

@Composable
private fun SecurityScoreProviderRow(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    onLinkClick: () -> Unit,
) {
    TangemRowContainer(
        modifier = Modifier
            .background(
                color = TangemTheme.colors3.bg.tertiary,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.primary,
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .layoutId(TangemRowLayoutId.HEAD),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerUM.iconUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { TangemShimmer(modifier = Modifier.fillMaxSize(), radius = 8.dp) },
            error = { TangemShimmer(modifier = Modifier.fillMaxSize(), radius = 8.dp) },
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.START_TOP),
            text = providerUM.name,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )

        providerUM.lastAuditDate?.let { auditDate ->
            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .layoutId(TangemRowLayoutId.START_BOTTOM),
                text = auditDate,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }

        StarBlock(
            score = providerUM.score,
            modifier = Modifier
                .padding(start = 8.dp)
                .clickableSingle(onClick = onLinkClick)
                .layoutId(TangemRowLayoutId.END_TOP),
        )

        UrlBlock(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickableSingle(onClick = onLinkClick)
                .layoutId(TangemRowLayoutId.END_BOTTOM),
            providerUM = providerUM,
        )
    }
}

@Composable
private fun UrlBlock(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    modifier: Modifier = Modifier,
) {
    val urlData = providerUM.urlData
    val rootHost = urlData?.rootHost
    if (urlData != null && rootHost != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = urlData.rootHost,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Icon(
                modifier = Modifier
                    .size(16.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
fun StarBlock(score: Float, modifier: Modifier = Modifier) {
    val grayColor = TangemTheme.colors3.icon.secondary
    val percentage = score / 5
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = score.toString(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Icon(
            modifier = Modifier
                .requiredSize(20.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            color = grayColor,
                            topLeft = Offset(x = size.width * percentage, y = 0f),
                            size = Size(size.width * (1 - percentage), size.height),
                            blendMode = BlendMode.SrcIn,
                        )
                    }
                },
            imageVector = ImageVector.vectorResource(R.drawable.ic_star_filled_20),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.brand,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScoreBottomSheetPreviewV2() {
    TangemThemePreviewRedesign {
        SecurityScoreBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = SecurityScorePreviewData.bottomSheetContent,
            ),
        )
    }
}