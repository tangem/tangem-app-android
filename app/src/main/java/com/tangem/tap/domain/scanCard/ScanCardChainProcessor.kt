package com.tangem.tap.domain.scanCard

import com.tangem.common.core.TangemError
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.Chain
import com.tangem.tap.common.ChainProcessor
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.redux.navigation.AppScreen
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
class ScanCardChainProcessor : ChainProcessor<ChainResult<ScanResponse>> {

    private val chains = mutableListOf<Chain<ChainResult<ScanResponse>>>()

    override fun addChain(chain: Chain<ChainResult<ScanResponse>>) {
        chains.add(chain)
    }

    override suspend fun launch(): ChainResult<ScanResponse> {
        Timber.d("launch")
        var chainResult: ChainResult<ScanResponse> = ChainResult.Failure(ScanChainError.None)

        chains.forEach chainsPointer@{
            val chainName = "[${it::class.java.simpleName}]"
            Timber.d("launch the chain $chainName")

            val result = it.invoke(chainResult).apply { chainResult = this }

            if (result is ChainResult.Failure && result.error is ScanChainError.InterruptBy) {
                Timber.d("$chainName is Interrupted")
                return chainResult
            }
        }

        Timber.d("all chains are finished")
        return chainResult
    }
}

sealed class ScanChainError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Throwable() {

    object None : ScanChainError()

    sealed class InterruptBy(throwable: Throwable? = null) : ScanChainError("Chain is interrupted.", throwable) {
        object SaltPayError : InterruptBy(Throwable("SaltPay error"))
        object DisclaimerDismissed : InterruptBy(Throwable("Disclaimer dismissed"))
        class Navigation(screen: AppScreen) : InterruptBy(Throwable("Navigate to: $screen"))
        data class ScanFailed(val tangemError: TangemError) : InterruptBy(tangemError)
    }
}
