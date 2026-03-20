package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetTitle
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.preview.SecurityScorePreviewData
import com.tangem.features.feed.ui.market.detailed.state.SecurityScoreBottomSheetContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet as TangemBottomSheetV2

@Composable
internal fun SecurityScoreBottomSheet(config: TangemBottomSheetConfig) {
    if (LocalRedesignEnabled.current) {
        SecurityScoreBottomSheetV2(config)
    } else {
        SecurityScoreBottomSheetV1(config)
    }
}

@Composable
private fun SecurityScoreBottomSheetV1(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<SecurityScoreBottomSheetContent>(
        config = config,
        addBottomInsets = false,
        title = { TangemBottomSheetTitle(title = it.title) },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens.spacing16),
            ) {
                Text(
                    text = content.description.resolveReference(),
                    style = TangemTheme.typography.body2.copy(
                        color = TangemTheme.colors.text.secondary,
                    ),
                )

                SpacerH12()
                content.providers.fastForEachIndexed { index, provider ->
                    DividerContainer(
                        modifier = Modifier
                            .roundedShapeItemDecoration(
                                currentIndex = index,
                                lastIndex = content.providers.lastIndex,
                                addDefaultPadding = false,
                            )
                            .background(TangemTheme.colors.background.action),
                        showDivider = index != content.providers.lastIndex,
                    ) {
                        SecurityScoreProviderRow(
                            providerUM = provider,
                            onLinkClick = { content.onProviderLinkClick(provider) },
                        )
                    }
                }

                SpacerH16()
                SpacerH(bottomBarHeight)
            }
        },
    )
}

@Composable
private fun SecurityScoreBottomSheetV2(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheetV2<SecurityScoreBottomSheetContent>(
        config = config,
        type = TangemBottomSheetType.Modal,
        title = { content ->
            TangemTopBar(
                title = content.title,
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_close_24),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(onClick = config.onDismissRequest)
                            .padding(TangemTheme.dimens2.x2_5),
                    )
                },
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens2.x4),
            ) {
                Text(
                    text = content.description.resolveReference(),
                    style = TangemTheme.typography2.bodyRegular15.copy(
                        color = TangemTheme.colors2.text.neutral.tertiary,
                    ),
                )
                SpacerH12()
                content.providers.fastForEach { provider ->
                    SecurityScoreProviderRowV2(
                        providerUM = provider,
                        onLinkClick = { content.onProviderLinkClick(provider) },
                    )
                    SpacerH(TangemTheme.dimens2.x2)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing12)
            .heightIn(min = TangemTheme.dimens.size68),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerUM.iconUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            error = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
            contentDescription = null,
        )

        Column(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                text = providerUM.name,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            providerUM.lastAuditDate?.let { auditDate ->
                Text(
                    text = auditDate,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }

        SpacerWMax()

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
            modifier = Modifier.clickable(
                enabled = providerUM.urlData != null,
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onLinkClick,
            ),
        ) {
            ScoreStarsBlock(
                score = providerUM.score,
                scoreTextStyle = TangemTheme.typography.body2,
                horizontalSpacing = TangemTheme.dimens.spacing3,
            )

            UrlBlockV1(providerUM)
        }
    }
}

@Composable
private fun SecurityScoreProviderRowV2(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    onLinkClick: () -> Unit,
) {
    TangemRowContainer(
        modifier = Modifier
            .background(
                color = TangemTheme.colors2.surface.level4,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.primary,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(size = TangemTheme.dimens.size40)
                .clip(TangemTheme.shapes.roundedCorners8)
                .layoutId(TangemRowLayoutId.HEAD),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerUM.iconUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = { RectangleShimmer(radius = TangemTheme.dimens2.x3) },
            error = { RectangleShimmer(radius = TangemTheme.dimens2.x3) },
            contentDescription = null,
        )

        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .layoutId(TangemRowLayoutId.START_TOP),
            text = providerUM.name,
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
        )

        providerUM.lastAuditDate?.let { auditDate ->
            Text(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens2.x2)
                    .layoutId(TangemRowLayoutId.START_BOTTOM),
                text = auditDate,
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
            )
        }

        StarBlock(
            score = providerUM.score,
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .clickableSingle(onClick = onLinkClick)
                .layoutId(TangemRowLayoutId.END_TOP),
        )

        UrlBlockV2(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .clickableSingle(onClick = onLinkClick)
                .layoutId(TangemRowLayoutId.END_BOTTOM),
            providerUM = providerUM,
        )
    }
}

@Composable
private fun UrlBlockV1(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    modifier: Modifier = Modifier,
) {
    val urlData = providerUM.urlData
    val rootHost = urlData?.rootHost
    if (urlData != null && rootHost != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        ) {
            Text(
                text = urlData.rootHost,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
            Icon(
                modifier = Modifier
                    .size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun UrlBlockV2(
    providerUM: SecurityScoreBottomSheetContent.SecurityScoreProviderUM,
    modifier: Modifier = Modifier,
) {
    val urlData = providerUM.urlData
    val rootHost = urlData?.rootHost
    if (urlData != null && rootHost != null) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5),
        ) {
            Text(
                text = urlData.rootHost,
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
            )
            Icon(
                modifier = Modifier
                    .size(TangemTheme.dimens2.x4),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_top_right_24),
                contentDescription = null,
                tint = TangemTheme.colors2.markers.iconGray,
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
fun StarBlock(score: Float, modifier: Modifier = Modifier) {
    val grayColor = TangemTheme.colors2.markers.iconGray
    val percentage = score / 5
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = score.toString(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
        )
        Icon(
            modifier = Modifier
                .requiredSize(TangemTheme.dimens2.x5)
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
            tint = TangemTheme.colors2.markers.iconBlue,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScoreBottomSheetPreviewV1() {
    TangemThemePreview {
        SecurityScoreBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = SecurityScorePreviewData.bottomSheetContent,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityScoreBottomSheetPreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            SecurityScoreBottomSheet(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = SecurityScorePreviewData.bottomSheetContent,
                ),
            )
        }
    }
}