package com.tangem.core.error

import com.tangem.common.core.TangemSdkError
import com.tangem.blockchain.common.BlockchainSdkError

val TangemSdkError.universalError: UniversalError
    get() = object : UniversalError {
        override val errorCode: Int = 101000000 + code
    }

val BlockchainSdkError.universalError: UniversalError
    get() = object : UniversalError {
        override val errorCode: Int = 102000000 + code
    }