package com.tangem.domain.transaction.error

sealed interface SolanaGaslessSendError {
    data class Signing(val error: SendTransactionError) : SolanaGaslessSendError
    data class Service(val error: SolanaGaslessError) : SolanaGaslessSendError
}