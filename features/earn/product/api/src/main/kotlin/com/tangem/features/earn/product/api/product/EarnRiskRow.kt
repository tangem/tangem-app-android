package com.tangem.features.earn.product.api.product

import androidx.annotation.StringRes
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference

fun EarnRisk.detailsRow(): EarnRow = EarnRow(
    label = resourceReference(R.string.earn_product_details_risk),
    value = EarnValue.Text(resourceReference(texts().level)),
    info = EarnRowInfo.Items(
        title = resourceReference(R.string.earn_product_details_risk_info_title),
        items = EarnRisk.entries.map { risk ->
            EarnTextBlock(
                title = resourceReference(risk.texts().title),
                description = resourceReference(risk.texts().description),
            )
        },
    ),
    kind = EarnRowKind.Risk,
)

private class RiskTexts(@StringRes val level: Int, @StringRes val title: Int, @StringRes val description: Int)

private fun EarnRisk.texts(): RiskTexts = when (this) {
    EarnRisk.Low -> RiskTexts(
        level = R.string.earn_risk_level_low,
        title = R.string.earn_risk_low,
        description = R.string.earn_product_details_risk_low_description,
    )
    EarnRisk.Medium -> RiskTexts(
        level = R.string.earn_risk_level_medium,
        title = R.string.earn_risk_medium,
        description = R.string.earn_product_details_risk_medium_description,
    )
    EarnRisk.High -> RiskTexts(
        level = R.string.earn_risk_level_high,
        title = R.string.earn_risk_high,
        description = R.string.earn_product_details_risk_high_description,
    )
}