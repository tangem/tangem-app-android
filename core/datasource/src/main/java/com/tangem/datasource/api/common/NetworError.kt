package com.tangem.datasource.api.common

/**
[REDACTED_AUTHOR]
 */
// sealed interface NetworkModuleMessage : ModuleMessage
//
// sealed class NetworkError(
//     subCode: Int,
//     override val message: String,
//     override val data: Any?,
// ) : NetworkModuleMessage, ModuleError() {
//     override val code: Int = ModuleErrorCode.NETWORK + subCode
//
//     companion object {
//         // base code used for all error in the module
//         // const val CODE_ANY_OTHER = 100..199, 200..299
//     }
// }