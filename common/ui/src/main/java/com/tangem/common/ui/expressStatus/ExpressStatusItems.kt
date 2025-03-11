package com.tangem.common.ui.expressStatus

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.common.ui.R
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateIconUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.PersistentList

fun LazyListScope.expressTransactionsItems(
    expressTxs: PersistentList<ExpressTransactionStateUM>,
    modifier: Modifier = Modifier,
) {
    items(
        count = expressTxs.size,
        key = { expressTxs[it].info.txId },
        contentType = { expressTxs[it]::class.java },
    ) {
        val itemInfo = expressTxs[it].info
        val (iconRes, tint) = when (itemInfo.iconState) {
            ExpressTransactionStateIconUM.Warning -> {
                R.drawable.ic_alert_triangle_20 to TangemTheme.colors.icon.attention
            }
            ExpressTransactionStateIconUM.Error -> {
                R.drawable.ic_alert_circle_24 to TangemTheme.colors.icon.warning
            }
            ExpressTransactionStateIconUM.None -> null to null
        }

        ExpressStatusItem(
            title = itemInfo.title,
            fromTokenIconState = itemInfo.fromCurrencyIcon,
            toTokenIconState = itemInfo.toCurrencyIcon,
            fromAmount = itemInfo.fromAmount,
            fromSymbol = itemInfo.fromAmountSymbol,
            toAmount = itemInfo.toAmount,
            toSymbol = itemInfo.toAmountSymbol,
            onClick = itemInfo.onClick,
            infoIconRes = iconRes,
            infoIconTint = tint,
            modifier = modifier.animateItem(),
        )
    }
}