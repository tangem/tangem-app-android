package com.tangem.domain.managetokens.model.exceptoin

sealed class SupportedBlockchainException {

    data object EmptyList : SupportedBlockchainException()

    data class DataError(val cause: Throwable) : SupportedBlockchainException()
}