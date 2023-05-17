package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.models.scan.ScanResponse

/**
 * Responsible for invoking the callback at the end of the card scanning operation. Should be passed as last chain in
 * after card scanning chains. Always returns result of previous chain.
 *
 * @param onScanningFinished a suspending function to be called when the scanning process has finished.
 *
 * @see Chain for more information about the Chain interface.
 */
internal class ScanningFinishedChain(
    private val onScanningFinished: suspend () -> Unit,
) : Chain<ScanCardException.ChainException, ScanResponse> {

    override suspend fun invoke(previousChainResult: ScanResponse): Either<ScanChainException, ScanResponse> {
        onScanningFinished()
        return previousChainResult.right()
    }
}