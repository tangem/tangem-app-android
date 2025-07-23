package com.tangem.features.send.v2.api

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

interface SendNotificationsComponent {

    val state: StateFlow<ImmutableList<NotificationUM>>

    fun LazyListScope.content(
        state: ImmutableList<NotificationUM>,
        modifier: Modifier = Modifier,
        hasPaddingAbove: Boolean = false,
        isClickDisabled: Boolean = false,
    )

    data class Params(
        val analyticsCategoryName: String,
        val userWalletId: UserWalletId,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrency: AppCurrency,
        val notificationData: NotificationData,
        val callback: ModelCallback,
    ) {
        data class NotificationData(
            val destinationAddress: String,
            val memo: String?,
            val amountValue: BigDecimal,
            val reduceAmountBy: BigDecimal,
            val isIgnoreReduce: Boolean,
            val fee: Fee?,
            val feeError: GetFeeError?,
        )
    }

    interface Factory : ComponentFactory<Params, SendNotificationsComponent>

    interface ModelCallback {
        fun onFeeReload()

        fun onAmountReduceTo(reduceTo: BigDecimal) {}

        fun onAmountReduceBy(reduceBy: BigDecimal, reduceByDiff: BigDecimal) {}

        fun onAmountIgnore() {}
    }
}