package com.tangem.network.common

import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleMessage

/**
[REDACTED_AUTHOR]
 */
sealed interface NetworkModuleMessage : ModuleMessage

sealed class NetworkError(
    subCode: Int,
    override val message: String,
    override val data: Any?,
) : NetworkModuleMessage, ModuleError {
    override val code: Int = ERROR_CODE_NETWORK + subCode

    companion object {
        // base code used for all error in the module
        const val ERROR_CODE_NETWORK = 20000
        // const val CODE_ANY_OTHER = 100..199
    }
}