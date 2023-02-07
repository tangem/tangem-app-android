package com.tangem.lib.wallet.impl.scan

import com.tangem.lib.wallet.impl.data.CardDTO
import com.tangem.lib.wallet.impl.data.ScanResponse

class ScanProcessorImpl : ScanProcessor {
    override suspend fun scan(): ScanResponse {
        return ScanResponse(CardDTO("test"))
    }
}