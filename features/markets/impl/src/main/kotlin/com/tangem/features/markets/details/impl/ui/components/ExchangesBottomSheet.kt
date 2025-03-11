package com.tangem.features.markets.details.impl.ui.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.details.impl.ui.state.ExchangesBottomSheetContent
import com.tangem.features.markets.impl.R
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
    Text(
        text = textReference.resolveReference(),
        color = TangemTheme.colors.text.tertiary,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.body2,
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
        ExchangesBottomSheetContent.Content(
            exchangeItems = List(size = 13) { index ->
                TokenItemState.Content(
                    id = index.toString(),
                    iconState = CurrencyIconState.CoinIcon(
                        url = null,
                        fallbackResId = R.drawable.ic_facebook_24,
                        isGrayscale = false,
                        showCustomBadge = false,
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