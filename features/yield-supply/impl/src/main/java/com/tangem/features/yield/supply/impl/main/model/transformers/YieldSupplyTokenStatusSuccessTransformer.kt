package com.tangem.features.yield.supply.impl.main.model.transformers

import com.tangem.domain.yield.supply.models.YieldMarketTokenStatus
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.LoadingStatusMode
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.utils.transformer.Transformer

internal class YieldSupplyTokenStatusSuccessTransformer(
    private val tokenStatus: YieldMarketTokenStatus,
    private val onStartEarningClick: () -> Unit,
    private val mode: LoadingStatusMode,
) : Transformer<YieldSupplyUM> {

    override fun transform(prevState: YieldSupplyUM): YieldSupplyUM {
        if (!tokenStatus.isActive) return YieldSupplyUM.Unavailable

        return when (mode) {
            LoadingStatusMode.Initial -> {
                YieldSupplyUM.Available(
                    title = resourceReference(
                        id = R.string.yield_module_token_details_earn_notification_title,
                        formatArgs = wrappedList(tokenStatus.apy),
                    ),
                    onClick = onStartEarningClick,
                )
            }
            LoadingStatusMode.LoadApy -> {
                if (prevState is YieldSupplyUM.Content) {
                    prevState.copy(
                        rewardsApy = stringReference("${tokenStatus.apy}%"),
                    )
                } else {
                    prevState
                }
            }
        }
    }
}