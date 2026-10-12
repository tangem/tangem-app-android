package com.tangem.features.yield.supply.impl.apy.about

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.yield.supply.impl.apy.about.ui.YieldSupplyAboutApyContent
import java.math.BigDecimal

internal class YieldSupplyAboutApyComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        YieldSupplyAboutApyContent(
            baseApy = params.baseApy,
            rewardsApr = params.rewardsApr,
            onDismiss = params.onDismiss,
        )
    }

    data class Params(
        val baseApy: BigDecimal,
        val rewardsApr: BigDecimal,
        val onDismiss: () -> Unit,
    )
}