package com.tangem.lib.wallet.impl.scan

import com.tangem.lib.wallet.impl.data.ScanResponse

interface ScanProcessor {

    suspend fun scan(): ScanResponse
}