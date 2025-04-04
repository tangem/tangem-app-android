package com.tangem.core.error.ext

import com.tangem.common.core.TangemSdkError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemError
import com.tangem.core.error.UniversalError

val TangemError.universalError: UniversalError
    get() = when (this) {
        is TangemSdkError -> object : UniversalError {
            override val errorCode: Int = 101000000 + code
        }
        else -> object : UniversalError {
            override val errorCode: Int = code
        }
    }

val BlockchainSdkError.universalError: UniversalError
    get() = object : UniversalError {
        override val errorCode: Int = 102000000 + code
    }

val UniversalError.tangemError: TangemError
    get() = object : TangemError(errorCode) {
        override var customMessage: String = errorCode.toString()
        override val messageResId: Int? = null
    }