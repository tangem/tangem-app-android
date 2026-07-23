package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.ExchangeItemUM
import com.tangem.features.feed.ui.market.detailed.state.ExchangesBottomSheetContent
import kotlinx.collections.immutable.toImmutableList

/**
 * Exchanges bottom sheet
 *
 * @param config bottom sheet config
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ExchangesBottomSheet(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<ExchangesBottomSheetContent>(
        config = config,
        containerColor = TangemTheme.colors3.bg.primary,
        title = { content ->
            TangemTopNavigation(
                title = resourceReference(content.titleResId),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                windowInsets = WindowInsets(0),
                blurBackground = false,
                onBack = config.onDismissRequest,
            )
        },
        content = { content ->
            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomBarHeight),
                ) {
                    item(key = "subtitle") {
                        Subtitle(
                            subtitleRes = content.subtitleResId,
                            volumeReference = content.volumeReference,
                            modifier = Modifier.padding(horizontal = 28.dp),
                        )
                    }

                    if (content.exchangeItems.isNotEmpty()) {
                        val lastIndex = content.exchangeItems.lastIndex
                        itemsIndexed(
                            items = content.exchangeItems,
                            key = { _, item -> item.id },
                        ) { index, item ->
                            ExchangeItemRow(
                                exchangeItemUM = item,
                                modifier = Modifier
                                    .roundedShapeItemDecoration(
                                        currentIndex = index,
                                        lastIndex = lastIndex,
                                        backgroundColor = TangemTheme.colors3.bg.secondary,
                                    ),
                            )
                        }
                    }
                }

                if (content is ExchangesBottomSheetContent.Error) {
                    Error(
                        content = content,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        },
    )
}

@Composable
private fun Subtitle(@StringRes subtitleRes: Int, volumeReference: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SubtitleText(textReference = resourceReference(id = subtitleRes))

        SubtitleText(textReference = volumeReference)
    }
}

@Composable
private fun SubtitleText(textReference: TextReference) {
    Text(
        text = textReference.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Error(content: ExchangesBottomSheetContent.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = content.message),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.subheading.medium,
            textAlign = TextAlign.Center,
        )

        SpacerH(8.dp)

        TangemButton(
            variant = TangemButton.Variant.Secondary,
            text = resourceReference(com.tangem.core.ui.R.string.try_to_load_data_again_button_title),
            size = TangemButton.Size.X8,
            onClick = content.onRetryClick,
        )
    }
}

@Composable
private fun ExchangeItemRow(exchangeItemUM: ExchangeItemUM, modifier: Modifier = Modifier) {
    when (exchangeItemUM) {
        is ExchangeItemUM.Content -> {
            ExchangeItemRowContent(exchangeItemUM, modifier)
        }
        is ExchangeItemUM.Loading -> {
            ExchangeItemRowPlaceholder(modifier)
        }
    }
}

@Composable
private fun ExchangeItemRowContent(exchangeItemUM: ExchangeItemUM.Content, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    ) {
        TangemIcon(
            tangemIconUM = exchangeItemUM.icon,
            modifier = Modifier
                .layoutId(layoutId = TangemRowLayoutId.HEAD)
                .size(40.dp)
                .testTag(TokenElementsTestTags.TOKEN_ICON),
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.START_TOP)
                .testTag(TokenElementsTestTags.TOKEN_TITLE),
            text = exchangeItemUM.title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .testTag(TokenElementsTestTags.TOKEN_PRICE),
            text = exchangeItemUM.subTitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.END_TOP)
                .testTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT_TEXT),
            text = exchangeItemUM.volumeInUsd.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )

        Text(
            modifier = Modifier
                .background(
                    color = getBgColorByTrustValue(exchangeItemUM.auditLabel.type),
                    shape = CircleShape,
                )
                .padding(vertical = 2.dp, horizontal = 6.dp)
                .layoutId(TangemRowLayoutId.END_BOTTOM)
                .testTag(TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT),
            text = exchangeItemUM.auditLabel.text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = getColorByTrustValue(exchangeItemUM.auditLabel.type),
        )
    }
}

@Composable
private fun ExchangeItemRowPlaceholder(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    ) {
        TangemShimmer(
            radius = 999.dp,
            modifier = Modifier
                .size(40.dp)
                .layoutId(TangemRowLayoutId.HEAD),
        )
        ExchangeRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 108.dp,
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.START_TOP),
        )
        ExchangeRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 52.dp,
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.START_BOTTOM),
        )
        ExchangeRowShimmerLine(
            style = TangemTheme.typography3.body.medium,
            width = 106.dp,
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.END_TOP),
        )
        ExchangeRowShimmerLine(
            style = TangemTheme.typography3.caption.medium,
            width = 52.dp,
            modifier = Modifier
                .padding(start = 8.dp)
                .layoutId(TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun ExchangeRowShimmerLine(style: TextStyle, width: Dp, modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(width)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun getBgColorByTrustValue(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors3.bg.status.errorSubtle
        AuditLabelUM.Type.Warning -> TangemTheme.colors3.bg.status.warningSubtle
        AuditLabelUM.Type.Permit,
        AuditLabelUM.Type.Info,
        AuditLabelUM.Type.General,
        -> TangemTheme.colors3.bg.status.infoSubtle
    }
}

@Composable
private fun getColorByTrustValue(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors3.text.status.error
        AuditLabelUM.Type.Warning -> TangemTheme.colors3.text.status.warning
        AuditLabelUM.Type.Permit,
        AuditLabelUM.Type.Info,
        AuditLabelUM.Type.General,
        -> TangemTheme.colors3.text.status.info
    }
}

@Preview
@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ExchangesBottomSheet(
    @PreviewParameter(ExchangesBottomSheetContentProvider::class) content: ExchangesBottomSheetContent,
) {
    TangemThemePreviewRedesign {
        ExchangesBottomSheet(
            config = TangemBottomSheetConfig(
                onDismissRequest = {},
                content = content,
                isShown = true,
            ),
        )
    }
}

private class ExchangesBottomSheetContentProvider : CollectionPreviewParameterProvider<ExchangesBottomSheetContent>(
    listOf(
        ExchangesBottomSheetContent.Loading(exchangesCount = 13),
        ExchangesBottomSheetContent.Error(onRetryClick = {}),
        ExchangesBottomSheetContent.Content(
            exchangeItems = List(size = 13) { index ->
                ExchangeItemUM.Content(
                    id = index.toString(),
                    title = stringReference(value = "OKX"),
                    subTitle = stringReference(value = "CEX"),
                    icon = TangemIconUM.Url(url = null, fallbackRes = R.drawable.ic_facebook_24),
                    volumeInUsd = stringReference(value = "$67.52M"),
                    auditLabel = AuditLabelUM(
                        text = stringReference("Caution"),
                        type = AuditLabelUM.Type.Warning,
                    ),
                )
            }
                .toImmutableList(),
        ),
    ),
)