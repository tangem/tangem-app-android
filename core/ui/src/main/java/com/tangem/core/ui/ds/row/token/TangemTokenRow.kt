package com.tangem.core.ui.ds.row.token

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds.row.token.internal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenElementsTestTags
import org.burnoutcrew.reorderable.ReorderableLazyListState

/**
 * Composable function that represents a Tangem token row in a list.
 *
 * [Token Row](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8207-17583&t=k8dyaykorsNocGVq-4)
 *
 * @param tokenRowUM                The user model containing the data for the token row.
 * @param isBalanceHidden           A boolean indicating whether the balance should be hidden.
 * @param reorderableTokenListState The state of the reorderable lazy list, if applicable.
 * @param modifier                  The modifier to be applied to the row.
 */
@Composable
fun TangemTokenRow(
    tokenRowUM: TangemTokenRowUM,
    isBalanceHidden: Boolean,
    reorderableTokenListState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
) {
    TangemRowContainer(
        content = {
            TangemIcon(
                tangemIconUM = tokenRowUM.headIconUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_ICON),
            )

            TokenRowPromoBanner(
                promoBannerUM = tokenRowUM.promoBannerUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.EXTRA_TOP)
                    .testTag(tag = TokenElementsTestTags.TOKEN_YIELD_PROMO_BANNER)
                    .padding(horizontal = TangemTheme.dimens2.x3)
                    .fillMaxWidth(),
            )

            TokenRowTitle(
                titleUM = tokenRowUM.titleUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_TOP)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_TITLE),
            )

            TokenRowSubtitle(
                subtitleUM = tokenRowUM.subtitleUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_PRICE),
            )

            TokenRowEndTopContent(
                endContentUM = tokenRowUM.topEndContentUM,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.END_TOP)
                    .testTag(tag = TokenElementsTestTags.TOKEN_FIAT_AMOUNT),
            )

            TokenRowEndBottomContent(
                endContentUM = tokenRowUM.bottomEndContentUM,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.END_BOTTOM)
                    .testTag(tag = TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT),
            )

            TokenRowTail(
                tailUM = tokenRowUM.tailUM,
                reorderableTokenListState = reorderableTokenListState,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.TAIL)
                    .testTag(tag = TokenElementsTestTags.TOKEN_NON_FIAT_BLOCK),
            )
        },
        modifier = modifier.tokenClickable(tokenRowUM = tokenRowUM),
    )
}

/**
 * Composable function that represents a Tangem token row in a list.
 *
 * [Token Row](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8207-17583&t=k8dyaykorsNocGVq-4)
 *
 * @param tokenRowUM                The user model containing the data for the token row.
 * @param headComponent             The composable function representing the head component.
 * @param titleComponent            The composable function representing the title component.
 * @param isBalanceHidden           A boolean indicating whether the balance should be hidden.
 * @param reorderableTokenListState The state of the reorderable lazy list, if applicable.
 * @param modifier                  The modifier to be applied to the row.
 */
@Composable
fun TangemTokenRow(
    tokenRowUM: TangemTokenRowUM,
    isBalanceHidden: Boolean,
    reorderableTokenListState: ReorderableLazyListState?,
    modifier: Modifier = Modifier,
    headComponent: @Composable (Modifier) -> Unit,
    titleComponent: @Composable (Modifier) -> Unit,
) {
    TangemRowContainer(
        content = {
            headComponent(
                Modifier
                    .layoutId(layoutId = TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_ICON),
            )

            TokenRowPromoBanner(
                promoBannerUM = tokenRowUM.promoBannerUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.EXTRA_TOP)
                    .testTag(tag = TokenElementsTestTags.TOKEN_YIELD_PROMO_BANNER)
                    .padding(horizontal = TangemTheme.dimens2.x3)
                    .fillMaxWidth(),
            )

            titleComponent(
                Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_TOP)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_TITLE),
            )

            TokenRowSubtitle(
                subtitleUM = tokenRowUM.subtitleUM,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.START_BOTTOM)
                    .padding(end = TangemTheme.dimens2.x2)
                    .testTag(tag = TokenElementsTestTags.TOKEN_PRICE),
            )

            TokenRowEndTopContent(
                endContentUM = tokenRowUM.topEndContentUM,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.END_TOP)
                    .testTag(tag = TokenElementsTestTags.TOKEN_FIAT_AMOUNT),
            )

            TokenRowEndBottomContent(
                endContentUM = tokenRowUM.bottomEndContentUM,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.END_BOTTOM)
                    .testTag(tag = TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT),
            )

            TokenRowTail(
                tailUM = tokenRowUM.tailUM,
                reorderableTokenListState = reorderableTokenListState,
                modifier = Modifier
                    .layoutId(layoutId = TangemRowLayoutId.TAIL)
                    .testTag(tag = TokenElementsTestTags.TOKEN_NON_FIAT_BLOCK),
            )
        },
        modifier = modifier.tokenClickable(tokenRowUM = tokenRowUM),
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tokenClickable(tokenRowUM: TangemTokenRowUM): Modifier = composed {
    val hapticFeedback = LocalHapticFeedback.current

    val onClick = tokenRowUM.onItemClick
    val onLongClick = tokenRowUM.onItemLongClick
    val onHapticLongClick = if (onLongClick != null) {
        {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongClick()
        }
    } else {
        null
    }

    when {
        onClick == null && onLongClick == null -> this
        onClick == null && onLongClick != null -> combinedClickable(onClick = {}, onLongClick = onHapticLongClick)
        onClick != null && onLongClick == null -> combinedClickable(onClick = onClick)
        onClick != null && onLongClick != null -> {
            combinedClickable(onClick = onClick, onLongClick = onHapticLongClick)
        }
        else -> this
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemTokenRow_Preview(
    @PreviewParameter(TangemTokenRowPreviewProvider::class) tokenRowUM: TangemTokenRowUM,
) {
    TangemThemePreviewRedesign {
        TangemTokenRow(
            tokenRowUM = tokenRowUM,
            isBalanceHidden = false,
            reorderableTokenListState = null,
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
        )
    }
}

private class TangemTokenRowPreviewProvider : CollectionPreviewParameterProvider<TangemTokenRowUM>(
    collection = listOf(
        TangemTokenRowPreviewData.defaultState,
        TangemTokenRowPreviewData.defaultEllipsisState,
        TangemTokenRowPreviewData.tokenState,
        TangemTokenRowPreviewData.customTokenState,
        TangemTokenRowPreviewData.draggableState,
        TangemTokenRowPreviewData.draggableStateV2,
        TangemTokenRowPreviewData.loadingState,
        TangemTokenRowPreviewData.unreachableState,
        TangemTokenRowPreviewData.accountState,
        TangemTokenRowPreviewData.accountLetterState,
        TangemTokenRowPreviewData.accountEllipsisState,
        TangemTokenRowPreviewData.promoBannerState,
    ),
)
// endregion