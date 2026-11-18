package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnRowKind

internal sealed class EarnPickerAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : EarnAnalyticsEvent(category = EARN_ANALYTICS_CATEGORY, event = event, params = params) {

    class ScreenOpened(
        source: EarnEntrySource,
        token: String,
        product: String,
        currentApy: String?,
        products: Int,
    ) : EarnPickerAnalyticsEvent(
        event = "Yield Picker Screen Opened",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source.toAnalyticsValue())
            put(AnalyticsParam.TOKEN_PARAM, token)
            put(EarnAnalyticsParam.PRODUCT, product)
            currentApy?.let { put(EarnAnalyticsParam.CURRENT_APY, it) }
            put(EarnAnalyticsParam.PRODUCTS, products.toString())
        },
    )

    class ProductChip(
        product: String,
        position: Int,
        currentApy: String?,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Product Chip",
        params = buildMap {
            put(EarnAnalyticsParam.PRODUCT, product)
            put(EarnAnalyticsParam.POSITION, position.toString())
            currentApy?.let { put(EarnAnalyticsParam.CURRENT_APY, it) }
        },
    )

    class Tab(
        product: String,
        tab: EarnPickerTab,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Yield Picker Tab",
        params = mapOf(
            EarnAnalyticsParam.PRODUCT to product,
            EarnAnalyticsParam.TAB to tab.toAnalyticsValue(),
        ),
    )

    class CalculatorHorizon(
        product: String,
        horizon: EarnHorizon,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Calculator Horizon",
        params = mapOf(
            EarnAnalyticsParam.PRODUCT to product,
            EarnAnalyticsParam.HORIZON to horizon.toAnalyticsValue(),
        ),
    )

    class CalculatorInput(
        product: String,
        input: EarnCalculatorComponent.Input,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Calculator Input",
        params = mapOf(
            EarnAnalyticsParam.PRODUCT to product,
            EarnAnalyticsParam.INPUT_TYPE to input.toAnalyticsValue(),
        ),
    )

    class DetailRow(
        product: String,
        row: EarnRowKind?,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Detail Row",
        params = buildMap {
            put(EarnAnalyticsParam.PRODUCT, product)
            row?.let { put(EarnAnalyticsParam.ROW, it.toAnalyticsValue()) }
        },
    )

    class StartEarning(
        product: String,
        currentApy: String?,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Start Earning",
        params = buildMap {
            put(EarnAnalyticsParam.PRODUCT, product)
            currentApy?.let { put(EarnAnalyticsParam.CURRENT_APY, it) }
        },
    )

    class NoFundsNotice(
        token: String,
        product: String,
    ) : EarnPickerAnalyticsEvent(
        event = "Notice - No Funds",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            EarnAnalyticsParam.PRODUCT to product,
        ),
    )

    class AddFunds(
        token: String,
        source: EarnAddFundsSource,
    ) : EarnPickerAnalyticsEvent(
        event = "Button - Add Funds",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            AnalyticsParam.SOURCE to source.value,
        ),
    )
}