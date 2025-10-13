package com.tangem.features.yield.supply.impl.main.model.transformers

import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.transformer.Transformer

internal class YieldSupplyTokenStatusSuccessTransformer(
    private val tokenStatus: YieldMarketToken,
    private val onStartEarningClick: () -> Unit,
) : Transformer<YieldSupplyUM> {

    override fun transform(prevState: YieldSupplyUM): YieldSupplyUM {
        if (!tokenStatus.isActive) return YieldSupplyUM.Unavailable

        return YieldSupplyUM.Available(
            title = resourceReference(
                id = R.string.yield_module_token_details_earn_notification_title,
                formatArgs = wrappedList(tokenStatus.apy),
            ),
            onClick = onStartEarningClick,
        )
    }
}