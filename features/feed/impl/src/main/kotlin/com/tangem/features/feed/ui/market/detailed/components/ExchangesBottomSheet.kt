package com.tangem.features.feed.ui.market.detailed.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.market.detailed.state.ExchangeItemUM
import com.tangem.features.feed.ui.market.detailed.state.ExchangesBottomSheetContent
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet as TangemBottomSheetV2

/**
 * Exchanges bottom sheet
 *
 * @param config bottom sheet config
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ExchangesBottomSheet(config: TangemBottomSheetConfig) {
    if (LocalRedesignEnabled.current) {
        ExchangesBottomSheetV2(config)
    } else {
        ExchangesBottomSheetV1(config)
    }
}

@Composable
private fun ExchangesBottomSheetV1(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheet<ExchangesBottomSheetContent>(
        config = config,
        addBottomInsets = false,
        title = { Title(textResId = it.titleResId, onBackClick = config.onDismissRequest) },
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
                            modifier = Modifier.padding(
                                start = TangemTheme.dimens.spacing16,
                                top = TangemTheme.dimens.spacing12,
                                end = TangemTheme.dimens.spacing16,
                                bottom = TangemTheme.dimens.spacing8,
                            ),
                        )
                    }

                    items(
                        items = content.exchangeItems,
                        key = TokenItemState::id,
                        itemContent = { TokenItem(state = it, isBalanceHidden = false) },
                    )
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
private fun ExchangesBottomSheetV2(config: TangemBottomSheetConfig) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    TangemBottomSheetV2<ExchangesBottomSheetContent>(
        config = config,
        title = { content ->
            TangemTopBar(
                title = resourceReference(content.titleResId),
                type = TangemTopBarType.BottomSheet,
                startContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(onClick = config.onDismissRequest)
                            .padding(TangemTheme.dimens2.x2),
                    )
                },
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
                            modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x7),
                        )
                    }

                    if (content.exchangeItemsV2.isNotEmpty()) {
                        val lastIndex = content.exchangeItemsV2.lastIndex
                        itemsIndexed(
                            items = content.exchangeItemsV2,
                            key = { _, item -> item.id },
                        ) { index, item ->
                            ExchangeItemRow(
                                exchangeItemUM = item,
                                modifier = Modifier
                                    .roundedShapeItemDecoration(
                                        currentIndex = index,
                                        lastIndex = lastIndex,
                                        backgroundColor = TangemTheme.colors2.surface.level3,
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
private fun Title(@StringRes textResId: Int, onBackClick: () -> Unit) {
    TangemTopAppBar(
        title = stringResourceSafe(id = textResId),
        startButton = TopAppBarButtonUM.Back(onBackClicked = onBackClick),
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
    if (LocalRedesignEnabled.current) {
        Text(
            text = textReference.resolveReference(),
            color = TangemTheme.colors2.text.neutral.secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography2.captionSemibold12,
        )
    } else {
        Text(
            text = textReference.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun Error(content: ExchangesBottomSheetContent.Error, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        ErrorV2(content, modifier)
    } else {
        ErrorV1(content, modifier)
    }
}

@Composable
private fun ErrorV1(content: ExchangesBottomSheetContent.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = content.message),
            color = TangemTheme.colors.text.tertiary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography.caption1,
        )

        SpacerH12()

        SecondarySmallButton(
            config = SmallButtonConfig(
                text = resourceReference(id = R.string.alert_button_try_again),
                onClick = content.onRetryClick,
            ),
        )
    }
}

@Composable
private fun ErrorV2(content: ExchangesBottomSheetContent.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(id = content.message),
            color = TangemTheme.colors2.text.neutral.tertiary,
            textAlign = TextAlign.Center,
            style = TangemTheme.typography2.bodyRegular14,
        )

        SpacerH(8.dp)

        TangemButton(
            buttonUM = TangemButtonUM(
                text = resourceReference(com.tangem.core.ui.R.string.try_to_load_data_again_button_title),
                onClick = content.onRetryClick,
                type = TangemButtonType.Secondary,
                size = TangemButtonSize.X8,
                shape = TangemButtonShape.Rounded,
            ),
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
                .size(TangemTheme.dimens2.x10),
        )

        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .layoutId(TangemRowLayoutId.START_TOP),
            text = exchangeItemUM.title.resolveReference(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
        )

        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .layoutId(TangemRowLayoutId.START_BOTTOM),
            text = exchangeItemUM.subTitle.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
        )

        Text(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x2)
                .layoutId(TangemRowLayoutId.END_TOP),
            text = exchangeItemUM.volumeInUsd.resolveReference(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
        )

        Text(
            modifier = Modifier
                .background(
                    color = getColorByTrustValue(exchangeItemUM.auditLabel.type).copy(alpha = .1f),
                    shape = CircleShape,
                )
                .padding(vertical = 2.dp, horizontal = 6.dp)
                .layoutId(TangemRowLayoutId.END_BOTTOM),
            text = exchangeItemUM.auditLabel.text.resolveReference(),
            style = TangemTheme.typography2.captionSemibold11,
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
        CircleShimmer(
            modifier = Modifier
                .size(40.dp)
                .layoutId(TangemRowLayoutId.HEAD),
        )
        RectangleShimmer(
            modifier = Modifier
                .width(108.dp)
                .height(20.dp)
                .padding(start = 4.dp)
                .layoutId(TangemRowLayoutId.START_TOP),
        )
        RectangleShimmer(
            modifier = Modifier
                .width(52.dp)
                .height(16.dp)
                .padding(start = 4.dp)
                .layoutId(TangemRowLayoutId.START_BOTTOM),
        )
        RectangleShimmer(
            modifier = Modifier
                .width(106.dp)
                .height(20.dp)
                .padding(start = 4.dp)
                .layoutId(TangemRowLayoutId.END_TOP),
        )
        RectangleShimmer(
            modifier = Modifier
                .width(52.dp)
                .height(16.dp)
                .padding(start = 4.dp)
                .layoutId(TangemRowLayoutId.END_BOTTOM),
        )
    }
}

@Composable
private fun getColorByTrustValue(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors2.fill.status.attention
        AuditLabelUM.Type.Warning -> TangemTheme.colors2.fill.status.warning
        AuditLabelUM.Type.Permit,
        AuditLabelUM.Type.Info,
        AuditLabelUM.Type.General,
        -> TangemTheme.colors2.fill.status.accent
    }
}

@Preview
@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ExchangesBottomSheet(
    @PreviewParameter(ExchangesBottomSheetContentProvider::class) content: ExchangesBottomSheetContent,
) {
    TangemThemePreview {
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
        ExchangesBottomSheetContent.ContentV1(
            exchangeItems = List(size = 13) { index ->
                TokenItemState.Content(
                    id = index.toString(),
                    iconState = CurrencyIconState.CoinIcon(
                        url = null,
                        fallbackResId = R.drawable.ic_facebook_24,
                        isGrayscale = false,
                        shouldShowCustomBadge = false,
                    ),
                    titleState = TokenItemState.TitleState.Content(text = stringReference(value = "OKX")),
                    fiatAmountState = TokenItemState.FiatAmountState.Content(text = "$67.52M"),
                    subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference(value = "CEX")),
                    subtitle2State = TokenItemState.Subtitle2State.LabelContent(
                        auditLabelUM = AuditLabelUM(
                            text = stringReference("Caution"),
                            type = AuditLabelUM.Type.Warning,
                        ),
                    ),
                    onItemClick = {},
                    onItemLongClick = {},
                )
            }
                .toImmutableList(),
        ),
    ),
)