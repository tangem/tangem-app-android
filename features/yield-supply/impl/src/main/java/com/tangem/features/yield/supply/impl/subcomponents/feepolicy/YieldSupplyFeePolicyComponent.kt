package com.tangem.features.yield.supply.impl.subcomponents.feepolicy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.subcomponents.feepolicy.ui.YieldSupplyFeePolicyContent
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyFeePolicyComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Title() {
        TangemModalBottomSheetTitle(
            startIconRes = R.drawable.ic_back_24,
            onStartClick = params.callback::onBackClick,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by params.yieldSupplyActionUMFlow.collectAsStateWithLifecycle()
        YieldSupplyFeePolicyContent(
            yieldSupplyFeeUM = uiState.yieldSupplyFeeUM,
            tokenSymbol = params.cryptoCurrency.symbol,
            modifier = modifier,
        )
    }

    @Composable
    override fun Footer() {
        SecondaryButton(
            text = stringResourceSafe(R.string.common_got_it),
            onClick = params.callback::onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val yieldSupplyActionUMFlow: StateFlow<YieldSupplyActionUM>,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onBackClick()
    }
}