package com.tangem.domain.walletconnect.usecase.method

import com.tangem.domain.walletconnect.model.WcPsbtOutput

/**
 * WalletConnect use case that can expose the outputs of a PSBT (`signPsbt` method) for display before signing.
 *
 */
interface WcPsbtUseCase {

    /**
     * Parses the PSBT of the current request and returns its outputs (recipient + amount).
     *
     * Returns an empty list if the PSBT cannot be parsed (the UI then falls back to the raw request data).
     */
    suspend fun parsePsbtOutputs(): List<WcPsbtOutput>
}