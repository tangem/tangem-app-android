package com.tangem.data.source.card.utils

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.data.source.card.model.ScanException

internal fun TangemError.toScanException(): ScanException {
    return when (this) {
        is TangemSdkError -> TODO("Add card SDK errors mappings")
        is BlockchainSdkError -> TODO("Add blockchain SDK errors mappings")
        else -> TODO("Add other errors mappings")
    }
}
