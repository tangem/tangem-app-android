package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.tap.domain.tasks.product.ScanProductTask

class FinalizeTwinTask(
    private val twinPublicKey: ByteArray,
    private val issuerKeys: KeyPair,
    private val isDynamicAddressesEnabled: Boolean,
) : CardSessionRunnable<ScanResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(session: CardSession, callback: (result: CompletionResult<ScanResponse>) -> Unit) {
        WriteProtectedIssuerDataTask(twinPublicKey, issuerKeys).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(
                        readMode = PreflightReadMode.FullCardRead,
                        secureStorage = session.environment.secureStorage,
                    ).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success ->
                                ScanProductTask(
                                    card = readResult.data,
                                    blockchainToDeriveFinder = null,
                                    visaCardScanHandler = null,
                                    visaCoroutineScope = null,
                                    shouldCheckIsAlreadyActivated = false,
                                    isDynamicAddressesEnabled = false,
                                    onboardingV2FeatureToggles = null,
                                ).run(session, callback)
                            is CompletionResult.Failure ->
                                callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}