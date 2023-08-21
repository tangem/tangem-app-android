package com.tangem.domain.core.error

sealed class DataError : Exception() {

    sealed class NetworkError : DataError() {

        object NoInternetConnection : NetworkError()
    }

    sealed class UserWalletError : DataError() {

        data class WrongUserWallet(override val message: String) : UserWalletError()
    }
}
