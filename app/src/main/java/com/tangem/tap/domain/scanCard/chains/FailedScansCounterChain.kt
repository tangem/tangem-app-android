package com.tangem.tap.domain.scanCard.chains

import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse

internal class FailedScansCounterChain(
    private val onMaxUnsuccessfulScansReached: () -> Unit,
    private val maxUnsuccessfulScans: Int = 3,
) : Chain<ScanCardException, ScanResponse> {

    override suspend fun launch(previousChainResult: ScanChainResult): ScanChainResult {
        if (previousChainResult.isLeft()) {
            unsuccessfulScansCounter = unsuccessfulScansCounter.inc().coerceAtMost(maxUnsuccessfulScans)
            if (unsuccessfulScansCounter == maxUnsuccessfulScans) {
                onMaxUnsuccessfulScansReached()
            }
        } else {
            unsuccessfulScansCounter = 0
        }

        return previousChainResult
    }

    private companion object {

        var unsuccessfulScansCounter = 0
    }
}