package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.exchange

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusItem
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.PersistentList

internal fun LazyListScope.swapTransactionsItems(
    swapTxs: PersistentList<SwapTransactionsState>,
    modifier: Modifier = Modifier,
) {
    if (swapTxs.isNotEmpty()) {
        items(
            count = swapTxs.size,
            key = { swapTxs[it].txId },
            contentType = { swapTxs[it]::class.java },
        ) {
            val item = swapTxs[it]
            val (iconRes, tint) = when (item.activeStatus) {
                ExchangeStatus.Verifying -> R.drawable.ic_alert_triangle_20 to TangemTheme.colors.icon.attention
                ExchangeStatus.Failed, ExchangeStatus.Cancelled -> {
                    R.drawable.ic_alert_circle_24 to TangemTheme.colors.icon.warning
                }
                else -> null to null
            }

            ExpressStatusItem(
                title = resourceReference(id = R.string.express_exchange_by, wrappedList(item.provider.name)),
                fromTokenIconState = item.fromCurrencyIcon,
                toTokenIconState = item.toCurrencyIcon,
                fromAmount = item.fromCryptoAmount,
                fromSymbol = item.fromCryptoCurrency.symbol,
                toSymbol = item.toCryptoCurrency.symbol,
                onClick = item.onClick,
                infoIconRes = iconRes,
                infoIconTint = tint,
                modifier = modifier.animateItem(),
            )
        }
    }
}