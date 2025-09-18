package com.tangem.features.yield.supply.impl.subcomponents.stopearning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyActionContent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.model.YieldSupplyStopEarningModel
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyStopEarningComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyStopEarningModel = getOrCreateModel(params = params)

    @Composable
    override fun Title() {
        TangemModalBottomSheetTitle(
            startIconRes = R.drawable.ic_back_24,
            onStartClick = params.callback::onBackClick,
        )
    }

    @Suppress("MagicNumber")
    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        YieldSupplyActionContent(
            yieldSupplyActionUM = state,
            modifier = modifier,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(TangemTheme.colors.icon.attention.copy(0.1f), CircleShape),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_alert_triangle_20),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.attention,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(32.dp),
                )
            }
        }
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()

        PrimaryButtonIconEnd(
            text = stringResourceSafe(R.string.common_confirm),
            onClick = model::onClick,
            iconResId = R.drawable.ic_tangem_24,
            enabled = state.isPrimaryButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onBackClick()
        fun onTransactionSent()
    }
}