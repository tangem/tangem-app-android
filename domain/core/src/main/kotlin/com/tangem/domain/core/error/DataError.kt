package com.tangem.domain.core.error

sealed class DataError : Throwable() {

    sealed class NetworkError : DataError() {

        class NoInternetConnection : NetworkError()
    }

    sealed class UserWalletError : DataError() {

        data class WrongUserWallet(override val message: String) : UserWalletError()
    }
}