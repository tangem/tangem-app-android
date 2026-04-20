package com.tangem.tap.domain.scanCard.chains

import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ResultChain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.features.disclaimer.createDisclaimer

/**
 * Handles disclaimer display after the card scanning operation.
 *
 * @param disclaimerWillShow an optional function to be invoked when a disclaimer is about to be shown. Default is an
 * empty function.
 *
 * @see Chain for more information about the Chain interface.
 */
internal class DisclaimerChain(
    private val appRouter: AppRouter,
    private val cardRepository: CardRepository,
    private val disclaimerWillShow: () -> Unit = {},
) : ResultChain<ScanCardException, ScanResponse>() {

    override suspend fun launch(previousChainResult: ScanResponse): ScanChainResult {
        val disclaimer = previousChainResult.card.createDisclaimer(cardRepository)

        return if (disclaimer.isAccepted()) {
            previousChainResult.right()
        } else {
            disclaimerWillShow()

            // TODO: [REDACTED_JIRA]
            appRouter.push(route = AppRoute.Disclaimer(isTosAccepted = false))

            previousChainResult.right()
        }
    }
}