package com.tangem.common.ui.expressStatus

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.R
import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateInfoUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

fun LazyListScope.expressTransactionsItems(
    expressTxs: PersistentList<ExpressTransactionStateUM>,
    modifier: Modifier = Modifier,
) {
    items(
        count = expressTxs.size,
        key = { index -> expressTxs[index].info.txId },
        contentType = { index -> expressTxs[index]::class.java },
    ) { index ->
        val itemInfo = expressTxs[index].info
        val (iconRes, tint) = when (itemInfo.iconState) {
            ExpressTransactionStateIconUM.Warning ->
                R.drawable.ic_attention_default_24 to TangemTheme.colors2.graphic.status.attention
            ExpressTransactionStateIconUM.Error ->
                R.drawable.ic_alert_circle_24 to TangemTheme.colors2.graphic.status.warning
            ExpressTransactionStateIconUM.None -> null to null
        }
        ExpressTransactionItem(
            state = expressTxs[index],
            infoIconRes = iconRes,
            infoIconTint = tint,
            modifier = modifier.animateItem(),
        )
    }
}

@Composable
private fun ExpressTransactionItem(
    state: ExpressTransactionStateUM,
    infoIconRes: Int?,
    infoIconTint: Color?,
    modifier: Modifier = Modifier,
) {
    val info = state.info
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors2.surface.level3)
            .clickable(onClick = info.onClick)
            .padding(TangemTheme.dimens2.x4)
            .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM),
    ) {
        TitleRow(
            title = info.title.resolveReference(),
            infoIconRes = infoIconRes,
            infoIconTint = infoIconTint,
        )
        if (!info.subtitle.isNullOrEmpty()) {
            Text(
                text = info.subtitle.resolveReference(),
                style = TangemTheme.typography2.captionRegular13,
                color = TangemTheme.colors3.text.tertiary,
            )
        }
        Spacer(Modifier.size(TangemTheme.dimens2.x3))
        AmountsRow(info = info)
    }
}

@Composable
private fun TitleRow(title: String, infoIconRes: Int?, infoIconTint: Color?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors3.text.primary,
            modifier = Modifier
                .weight(1f)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TITLE),
        )
        if (infoIconRes != null && infoIconTint != null) {
            Icon(
                painter = painterResource(infoIconRes),
                contentDescription = null,
                tint = infoIconTint,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x3),
            )
        }
    }
}

@Composable
private fun AmountsRow(info: ExpressTransactionStateInfoUM) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1_5),
    ) {
        CurrencyIcon(
            state = info.fromCurrencyIcon,
            shouldDisplayNetwork = false,
            modifier = Modifier
                .size(TangemTheme.dimens.size18)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_FROM_ICON),
        )
        EllipsisText(
            text = info.fromAmount.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors3.text.primary,
            ellipsis = TextEllipsis.OffsetEnd(info.fromAmountSymbol.length),
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_FROM_AMOUNT),
        )
        Icon(
            painter = painterResource(R.drawable.ic_forward_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.tertiary,
            modifier = Modifier
                .size(TangemTheme.dimens.size18)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_SWAP_ICON),
        )
        CurrencyIcon(
            state = info.toCurrencyIcon,
            shouldDisplayNetwork = false,
            modifier = Modifier
                .size(TangemTheme.dimens.size18)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TO_ICON),
        )
        EllipsisText(
            text = info.toAmount.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors3.text.primary,
            ellipsis = TextEllipsis.OffsetEnd(info.toAmountSymbol.length),
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .testTag(TokenDetailsScreenTestTags.EXPRESS_STATUS_ITEM_TO_AMOUNT),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpressTransactionItemPreview() {
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
            modifier = Modifier.padding(TangemTheme.dimens2.x4),
        ) {
            ExpressTransactionItem(
                state = PreviewExpressTransactionState,
                infoIconRes = null,
                infoIconTint = null,
            )
            ExpressTransactionItem(
                state = PreviewExpressTransactionState,
                infoIconRes = R.drawable.ic_attention_default_24,
                infoIconTint = TangemTheme.colors2.graphic.status.attention,
            )
            ExpressTransactionItem(
                state = PreviewExpressTransactionState,
                infoIconRes = R.drawable.ic_alert_circle_24,
                infoIconTint = TangemTheme.colors2.graphic.status.warning,
            )
        }
    }
}

private val PreviewExpressTransactionState: ExpressTransactionStateUM = object : ExpressTransactionStateUM {
    override val info = ExpressTransactionStateInfoUM(
        title = stringReference("Exchange by ChangeHero"),
        status = ExpressStatusUM(
            title = stringReference(""),
            link = ExpressLinkUM.Empty,
            statuses = persistentListOf(),
        ),
        notification = null,
        txId = "preview",
        txExternalId = null,
        txExternalUrl = null,
        timestamp = 0L,
        timestampFormatted = stringReference(""),
        timestampAgoFormatted = stringReference("Confirming ~ 59 min ago"),
        activeStatus = stringReference(""),
        onGoToProviderClick = {},
        onClick = {},
        onDisposeExpressStatus = {},
        iconState = ExpressTransactionStateIconUM.None,
        toAmount = stringReference("0,11441958 BTC"),
        toFiatAmount = null,
        toAmountSymbol = "BTC",
        toCurrencyIcon = CurrencyIconState.Loading,
        fromAmount = stringReference("100 SOL"),
        fromFiatAmount = null,
        fromAmountSymbol = "SOL",
        fromCurrencyIcon = CurrencyIconState.Loading,
    )
}
// endregion