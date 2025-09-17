package com.tangem.features.yield.supply.impl.subcomponents.startearning

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyActionContent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.YieldSupplyStartEarningModel

internal class YieldSupplyStartEarningComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyStartEarningModel = getOrCreateModel(params = params)

    @Composable
    override fun Title() {
        TangemModalBottomSheetTitle(
            endIconRes = R.drawable.ic_close_24,
            onEndClick = params.callback::onBackClick,
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
                    .width(80.dp)
                    .padding(vertical = 1.dp),
            ) {
                CurrencyIcon(
                    state = state.currencyIconState,
                    shouldDisplayNetwork = false,
                    iconSize = 48.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .align(Alignment.CenterStart),
                )
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_aave_36),
                    contentDescription = null,
                    modifier = Modifier
                        .background(TangemTheme.colors.background.tertiary, RoundedCornerShape(51.dp))
                        .padding(3.dp)
                        .size(48.dp)
                        .align(Alignment.CenterEnd),
                )
            }
        }
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()

        PrimaryButtonIconEnd(
            text = stringResourceSafe(R.string.yield_module_start_earning),
            onClick = model::onClick,
            enabled = state.isPrimaryButtonEnabled,
            iconResId = R.drawable.ic_tangem_24,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onBackClick()
        fun onFeePolicyClick()
        fun onTransactionSent()
    }
}