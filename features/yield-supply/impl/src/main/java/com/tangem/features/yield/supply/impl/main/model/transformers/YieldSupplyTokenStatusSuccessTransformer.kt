package com.tangem.features.yield.supply.impl.main.model.transformers

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.main.entity.YieldSupplyUM
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

internal class YieldSupplyTokenStatusSuccessTransformer(
    private val tokenStatus: YieldMarketToken,
    private val onStartEarningClick: () -> Unit,
    private val onLearnMoreClick: () -> Unit,
    private val boostedApy: BigDecimal? = null,
) : Transformer<YieldSupplyUM> {

    override fun transform(prevState: YieldSupplyUM): YieldSupplyUM {
        if (!tokenStatus.isActive) return YieldSupplyUM.Unavailable

        val boost = boostedApy
        return YieldSupplyUM.Available(
            title = if (boost != null) {
                resourceReference(R.string.yield_apy_boost_banner_title)
            } else {
                resourceReference(R.string.yield_module_token_details_earn_notification_earning_on_your_balance_title)
            },
            onClick = onStartEarningClick,
            onLearnMoreClick = onLearnMoreClick,
            isBoostAvailable = boost != null,
            apy = tokenStatus.apy.toString(),
            apyText = if (boost != null) {
                annotatedReference(buildBoostedApyText(baseApy = tokenStatus.apy, boostedApy = boost))
            } else {
                combinedReference(
                    resourceReference(R.string.yield_module_token_details_earn_notification_apy),
                    stringReference(" ${tokenStatus.apy}%"),
                )
            },
        )
    }

    private fun buildBoostedApyText(baseApy: BigDecimal, boostedApy: BigDecimal) = buildAnnotatedString {
        append("APY ")
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            append("$baseApy%")
        }
        append(" x3 → $boostedApy%")
    }
}