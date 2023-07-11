package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import arrow.core.right
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor

/**
 * Handles the analytics event interception and sending event after the scan card operation. Always returns result of
 * previous chain.
 *
 * @param event the analytics event to be handled by this chain.
 *
 * @see Chain for more information about the Chain interface.
 */
class AnalyticsChain(
    private val event: AnalyticsEvent,
) : Chain<ScanCardException.ChainException, ScanResponse> {

    override suspend fun invoke(
        previousChainResult: ScanResponse,
    ): Either<ScanCardException.ChainException, ScanResponse> {
        val interceptor = CardContextInterceptor(previousChainResult)
        val params = event.params.toMutableMap()
        interceptor.intercept(params)
        event.params = params.toMap()

        Analytics.send(event)

        return previousChainResult.right()
    }
}
