package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.domain.common.ProductType
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.features.demo.DemoHelper

/**
* [REDACTED_AUTHOR]
 */
class CardContextInterceptor(
    private val scanResponse: ScanResponse?,
) : ParamsInterceptor {

    override fun id(): String = CardContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean = true

    override fun intercept(params: MutableMap<String, String>) {
        scanResponse ?: return

        val card = scanResponse.card
        params[AnalyticsParam.Batch] = card.batchId
        params[AnalyticsParam.ProductType] = getProductType(scanResponse)
        params[AnalyticsParam.Firmware] = card.firmwareVersion.stringValue

        ParamCardCurrencyConverter().convert(scanResponse)?.let {
            params[AnalyticsParam.Currency] = it.value
        }
    }

    private fun getProductType(scanResponse: ScanResponse): String {
        return when (scanResponse.productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.Wallet -> "Wallet"
            ProductType.SaltPay -> when (scanResponse.isSaltPayVisa()) {
                true -> "Visa"
                else -> "Visa Backup"
            }
            ProductType.Start2Coin -> "Start2Coin"
            else -> when (DemoHelper.isDemoCard(scanResponse)) {
// [REDACTED_TODO_COMMENT]
                true -> "Demo Wallet | Demo Note"
                else -> "Other"
            }
        }
    }

    companion object {
        fun id(): String = CardContextInterceptor::class.java.simpleName
    }
}
