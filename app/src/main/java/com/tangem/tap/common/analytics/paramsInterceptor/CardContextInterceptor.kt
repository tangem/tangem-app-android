package com.tangem.tap.common.analytics.paramsInterceptor

import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.converters.ParamCardCurrencyConverter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import kotlinx.coroutines.runBlocking

/**
[REDACTED_AUTHOR]
 */
class CardContextInterceptor(
    private val scanResponse: ScanResponse,
) : ParamsInterceptor {

    private val walletsRepository = store.inject(DaggerGraphState::walletsRepository)

    private val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

    override fun id(): String = CardContextInterceptor.id()

    override fun canBeAppliedTo(event: AnalyticsEvent): Boolean {
        return when (event) {
            is IntroductionProcess.ButtonScanCard -> false
            else -> true
        }
    }

    override fun intercept(params: MutableMap<String, String>) {
        val card = scanResponse.card
        params[AnalyticsParam.BATCH] = card.batchId
        params[AnalyticsParam.PRODUCT_TYPE] = getProductType()
        params[AnalyticsParam.FIRMWARE] = card.firmwareVersion.stringValue
        if (userWalletId != null) {
            params[AnalyticsParam.USER_WALLET_ID] = userWalletId.stringValue
        }

        ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver)?.let {
            params[AnalyticsParam.CURRENCY] = it.value
        }
    }

    private fun getProductType(): String {
        if (scanResponse.productType != ProductType.Ring && userWalletId != null) {
            val isWalletWithRing = runBlocking { walletsRepository.isWalletWithRing(userWalletId) }

            if (isWalletWithRing) return "Ring"
        }

        return when (scanResponse.productType) {
            ProductType.Note -> "Note"
            ProductType.Twins -> "Twin"
            ProductType.Wallet -> "Wallet"
            ProductType.Wallet2 -> "Wallet 2.0"
            ProductType.Ring -> "Ring"
            ProductType.Start2Coin -> "Start2Coin"
            ProductType.Visa -> "VISA"
            else -> if (DemoHelper.isDemoCard(scanResponse)) getDemoCardProductType() else "Other"
        }
    }

    private fun getDemoCardProductType(): String {
        return if (DemoHelper.isTestDemoCard(scanResponse)) {
            "Demo Test"
        } else {
            when (scanResponse.card.cardId.substring(0..1)) {
                "AC" -> "Demo Wallet"
                "AB" -> "Demo Note"
                else -> "Demo Other"
            }
        }
    }

    companion object {
        fun id(): String = CardContextInterceptor::class.java.simpleName
    }
}