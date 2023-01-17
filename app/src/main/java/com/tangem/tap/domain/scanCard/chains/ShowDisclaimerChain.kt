package com.tangem.tap.domain.scanCard.chains

import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.successOr
import com.tangem.tap.domain.scanCard.ScanChainError
import com.tangem.tap.features.disclaimer.DisclaimerType
import com.tangem.tap.features.disclaimer.createDisclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerCallback
import com.tangem.tap.store
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ShowDisclaimerChain(
    private val chainData: DisclaimerChainData,
) : ScanCardChain {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }

        val disclaimer = DisclaimerType.get(scanResponse.card).createDisclaimer(scanResponse.card)
        val type = disclaimer.type()
        Timber.d("Disclaimer.get -> [$type]")
        store.dispatchOnMain(DisclaimerAction.SetDisclaimer(disclaimer))

        return if (disclaimer.isAccepted()) {
            Timber.d("[${disclaimer.type()}] is accepted")
            data
        } else {
            Timber.d("[${disclaimer.type()}] NOT accepted, show Disclaimer")
            chainData.willShow.invoke()

            suspendCoroutine { continuation ->
                store.dispatchOnMain(
                    DisclaimerAction.Show(
                        fromScreen = chainData.fromScreen,
                        callback = DisclaimerCallback(
                            onAccept = {
                                Timber.d("[$type] onAccept")
                                continuation.resume(data)
                            },
                            onDismiss = {
                                Timber.d("[$type] onDismiss")
                                continuation.resume(
                                    ChainResult.Failure(ScanChainError.InterruptBy.DisclaimerDismissed),
                                )
                            },
                        ),
                    ),
                )
            }
        }
    }
}

data class DisclaimerChainData(
    val fromScreen: AppScreen,
    val willShow: () -> Unit = {},
)
