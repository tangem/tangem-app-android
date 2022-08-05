package com.tangem.tap.common.analytics

import com.shopify.buy3.Storefront
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.extensions.filterNotNull


abstract class AnalyticsHandler {
    abstract fun triggerEvent(
        event: AnalyticsEvent,
        card: Card? = null,
        blockchain: String? = null,
        params: Map<String, String> = emptyMap()
    )

    abstract fun triggerEvent(
        event: String,
        params: Map<String, String>
    )

    abstract fun logCardSdkError(
        error: TangemSdkError,
        actionToLog: Analytics.ActionToLog,
        parameters: Map<AnalyticsParam, String>? = null,
        card: Card? = null
    )

    abstract fun logError(
        error: Throwable,
        params: Map<String, String> = emptyMap()
    )

    protected fun prepareParams(
        card: Card?,
        blockchain: String? = null,
        params: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return mapOf(
            AnalyticsParam.FIRMWARE.param to card?.firmwareVersion?.stringValue,
            AnalyticsParam.BATCH_ID.param to card?.batchId,
            AnalyticsParam.BLOCKCHAIN.param to blockchain,
        ).filterNotNull() + params
    }

    fun logEventWithParams(event: Analytics.AnalyticsWithParametersEvent){
        when(event){
            is Analytics.AnalyticsWithParametersEvent.SearchToken -> {
                triggerEvent(
                    event = AnalyticsEvent.SEARCH_TOKEN,
                    params = mapOf(
                        AnalyticsParam.FIRST_SCREEN_TOKEN_NAME.param to event.tokenName
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.BuyTokenTapped -> {
                triggerEvent(
                    event = AnalyticsEvent.BUY_TOKEN_TAPPED,
                    params = mapOf(
                        AnalyticsParam.BUY_TOKEN_TAPPED_TOKEN_NAME.param to event.buyTokenTapped
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.CurrencyChanged -> {
                triggerEvent(
                    event = AnalyticsEvent.CURRENCY_CHANGED,
                    params = mapOf(
                        AnalyticsParam.CURRENCY.param to event.currencyChanged
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.FirstScan -> {
                triggerEvent(
                    event = AnalyticsEvent.FIRST_SCAN,
                    params = mapOf(
                        AnalyticsParam.FIRST_SCAN_SUCCESS.param to event.firstScan.toString()
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.FirstScreenAccessCodeEntered -> {
                triggerEvent(
                    event = AnalyticsEvent.FIRST_SCREEN_ACCESS_CODE_ENTERED,
                    params = mapOf(
                        AnalyticsParam.FIRST_SCREEN_TOKEN_NAME.param to event.accessCode.toString()
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.P2pInstructionTapped -> {
                triggerEvent(
                    event = AnalyticsEvent.P2P_INSTRUCTION_TAPPED,
                    params = mapOf(
                        AnalyticsParam.TYPE.param to event.p2pInstructionTapped
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.SecondScan -> {
                triggerEvent(
                    event = AnalyticsEvent.SECOND_SCAN,
                    params = mapOf(
                        AnalyticsParam.SECOND_SCAN_SUCCESS.param to event.secondScan.toString()
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.TokenSearch -> {
                triggerEvent(
                    event = AnalyticsEvent.TOKEN_SEARCH,
                    params = mapOf(
                        AnalyticsParam.TOKEN_SEARCH_TOKEN_NAME.param to event.tokenSearch.toString()
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.TokenSwitchOff -> {
                triggerEvent(
                    event = AnalyticsEvent.TOKEN_SWITCH_OFF,
                    params = mapOf(
                        AnalyticsParam.TOKEN_SWITCH_OFF_TOKEN_NAME.param to event.tokenSwitchOff
                    )
                )
            }
            is Analytics.AnalyticsWithParametersEvent.TokenSwitchOn -> {
                triggerEvent(
                    event = AnalyticsEvent.TOKEN_SWITCH_ON,
                    params = mapOf(
                        AnalyticsParam.TOKEN_SWITCH_ON_TOKEN_NAME.param to event.tokenSwitchOn
                    )
                )
            }
        }
    }

    fun logWcEvent(event: Analytics.WcAnalyticsEvent) {
        when (event) {
            is Analytics.WcAnalyticsEvent.Action -> {
                triggerEvent(
                    event = AnalyticsEvent.WC_SUCCESS_RESPONSE,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action.name
                    )
                )
            }
            is Analytics.WcAnalyticsEvent.Error -> {
                val params = mapOf(
                    AnalyticsParam.WALLET_CONNECT_ACTION.param to event.action?.name,
                    AnalyticsParam.ERROR_DESCRIPTION.param to event.error.message
                )
                    .filterNotNull()
                logError(event.error, params)
            }
            is Analytics.WcAnalyticsEvent.InvalidRequest ->
                triggerEvent(
                    event = AnalyticsEvent.WC_INVALID_REQUEST,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_REQUEST.param to event.json
                    ).filterNotNull()
                )
            is Analytics.WcAnalyticsEvent.Session -> {
                val analyticsEvent = when (event.event) {
                    Analytics.WcSessionEvent.Disconnect -> AnalyticsEvent.WC_SESSION_DISCONNECTED
                    Analytics.WcSessionEvent.Connect -> AnalyticsEvent.WC_NEW_SESSION
                }
                triggerEvent(
                    event = analyticsEvent,
                    params = mapOf(
                        AnalyticsParam.WALLET_CONNECT_DAPP_URL.param to event.url
                    ).filterNotNull()
                )
            }
        }
    }

    fun logShopifyOrder(order: Storefront.Order) {
        triggerEvent(getOrderEvent(), getOrderParams(order))
    }

    protected abstract fun getOrderEvent(): String

    protected abstract fun getOrderParams(order: Storefront.Order): Map<String, String>
}
