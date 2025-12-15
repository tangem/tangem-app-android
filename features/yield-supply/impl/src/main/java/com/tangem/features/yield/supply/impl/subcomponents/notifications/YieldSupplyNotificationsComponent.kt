package com.tangem.features.yield.supply.impl.subcomponents.notifications

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.yield.supply.impl.subcomponents.notifications.model.YieldSupplyNotificationsModel
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyNotificationsComponent(
    private val appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyNotificationsModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            state.forEachIndexed { index, item ->
                Notification(
                    config = item.config,
                    modifier = Modifier.conditional(index == 0) {
                        Modifier.padding(top = 16.dp)
                    },
                    containerColor = TangemTheme.colors.background.action,
                )
            }
        }
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onFeeReload()
    }
}