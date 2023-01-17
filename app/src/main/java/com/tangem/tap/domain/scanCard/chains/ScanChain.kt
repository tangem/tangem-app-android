package com.tangem.tap.domain.scanCard.chains

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.doOnSuccess
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.Chain
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.ScanChainError
import com.tangem.tap.store
import com.tangem.tap.userTokensRepository

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
interface ScanCardChain : Chain<ChainResult<ScanResponse>>

class ScanChain(
    private val tangemSdkManager: TangemSdkManager,
    private val cardId: String? = null,
    private val additionalBlockchainsToDerive: Collection<Blockchain>? = null,
    private val onScanStateChange: suspend (inProgress: Boolean) -> Unit = {},
) : ScanCardChain {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        onScanStateChange(true)
        tangemSdkManager.changeDisplayedCardIdNumbersCount(null)

        val result = com.tangem.tap.tangemSdkManager.scanProduct(
            userTokensRepository = userTokensRepository,
            cardId = cardId,
            additionalBlockchainsToDerive = additionalBlockchainsToDerive,
        ).doOnSuccess {
            tangemSdkManager.changeDisplayedCardIdNumbersCount(it)
        }

        store.dispatchOnMain(GlobalAction.ScanFailsCounter.ChooseBehavior(result))
        onScanStateChange(false)

        return result.mapToChainResult()
    }
}

private fun <T> CompletionResult<T>.mapToChainResult(): ChainResult<T> {
    return when (this) {
        is CompletionResult.Success -> ChainResult.Success(this.data)
        is CompletionResult.Failure -> ChainResult.Failure(ScanChainError.InterruptBy.ScanFailed(this.error))
    }
}
