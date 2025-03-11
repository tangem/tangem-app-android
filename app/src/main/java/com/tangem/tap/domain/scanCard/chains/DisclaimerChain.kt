package com.tangem.tap.domain.scanCard.chains

import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ResultChain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.disclaimer.createDisclaimer
import org.rekotlin.Store

/**
 * Handles disclaimer display after the card scanning operation.
 *
 * Returns [ScanChainException.DisclaimerWasCanceled] if the disclaimer is dismissed by the user.
 *
 * @param store the [Store] that holds the state of the app, used here to dispatch actions related to disclaimers.
 * @param disclaimerWillShow an optional function to be invoked when a disclaimer is about to be shown. Default is an
 * empty function.
 *
 * @see Chain for more information about the Chain interface.
 */
internal class DisclaimerChain(
    private val store: Store<AppState>,
    private val disclaimerWillShow: () -> Unit = {},
) : ResultChain<ScanCardException, ScanResponse>() {

    override suspend fun launch(previousChainResult: ScanResponse): ScanChainResult {
        val disclaimer = previousChainResult.card.createDisclaimer()

        return if (disclaimer.isAccepted()) {
            previousChainResult.right()
        } else {
            disclaimerWillShow()

            // TODO: [REDACTED_JIRA]
            store.dispatchNavigationAction {
                push(route = AppRoute.Disclaimer(isTosAccepted = false))
            }

            previousChainResult.right()
        }
    }
}