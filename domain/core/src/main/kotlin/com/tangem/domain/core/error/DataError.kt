package com.tangem.domain.core.error

sealed class DataError : Exception() {

    sealed class NetworkError : DataError() {

        object NoInternetConnection : NetworkError()
    }

    sealed class PersistenceError : DataError() {

        data class UnableToReadFile(override val cause: Throwable) : DataError()

        data class UnableToWriteFile(override val cause: Throwable) : DataError()
    }
}