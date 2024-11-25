package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.onramp

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressStatusItem
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.PersistentList

internal fun LazyListScope.onrampTransactionsItems(
    onrampTxs: PersistentList<ExpressTransactionStateUM.OnrampUM>,
    modifier: Modifier = Modifier,
) {
    items(
        count = onrampTxs.size,
        key = { onrampTxs[it].info.txId },
        contentType = { onrampTxs[it]::class.java },
    ) {
        val item = onrampTxs[it]
        val itemInfo = item.info

        val (iconRes, tint) = when (item.activeStatus) {
            OnrampStatus.Status.Verifying -> R.drawable.ic_alert_triangle_20 to TangemTheme.colors.icon.attention
            OnrampStatus.Status.Failed -> {
                R.drawable.ic_alert_circle_24 to TangemTheme.colors.icon.warning
            }
            else -> null to null
        }

        ExpressStatusItem(
            title = resourceReference(id = R.string.express_status_buying, wrappedList(item.cryptoCurrencyName)),
            fromTokenIconState = itemInfo.fromCurrencyIcon,
            toTokenIconState = itemInfo.toCurrencyIcon,
            fromAmount = itemInfo.fromAmount,
            fromSymbol = itemInfo.fromAmountSymbol,
            toAmount = itemInfo.toAmount,
            toSymbol = itemInfo.toAmountSymbol,
            onClick = item.info.onClick,
            infoIconRes = iconRes,
            infoIconTint = tint,
            modifier = modifier.animateItem(),
        )
    }
}