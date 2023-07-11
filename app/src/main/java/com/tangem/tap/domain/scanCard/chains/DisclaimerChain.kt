package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.navigation.AppScreen
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.disclaimer.Disclaimer
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import org.rekotlin.Store
import kotlin.coroutines.resume

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
) : Chain<ScanCardException.ChainException, ScanResponse> {

    override suspend fun invoke(
        previousChainResult: ScanResponse,
    ): Either<ScanCardException.ChainException, ScanResponse> {
        val disclaimer = previousChainResult.card.createDisclaimer()

        return if (disclaimer.isAccepted()) {
            previousChainResult.right()
        } else {
            disclaimerWillShow()
            showDisclaimer(disclaimer, previousChainResult)
        }
    }

    private suspend fun showDisclaimer(
        disclaimer: Disclaimer,
        response: ScanResponse,
    ): Either<ScanCardException.ChainException, ScanResponse> {
        store.dispatchOnMain(DisclaimerAction.SetDisclaimer(disclaimer))

        return suspendCancellableCoroutine { continuation ->
            store.dispatchOnMain(
                DisclaimerAction.Show(
                    fromScreen = AppScreen.Home,
                    callback = DisclaimerCallback(
                        onAccept = {
                            if (continuation.isActive) {
                                continuation.resume(response.right())
                            }
                        },
                        onDismiss = {
                            if (continuation.isActive) {
                                continuation.resume(ScanChainException.DisclaimerWasCanceled.left())
                            }
                        },
                    ),
                ),
            )
        }
    }
}