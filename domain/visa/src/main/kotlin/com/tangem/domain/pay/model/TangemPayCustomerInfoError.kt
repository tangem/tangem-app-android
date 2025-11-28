package com.tangem.domain.pay.model

sealed interface TangemPayCustomerInfoError {

    data object UnavailableError : TangemPayCustomerInfoError
    data object RefreshNeededError : TangemPayCustomerInfoError
    data object UnknownError : TangemPayCustomerInfoError
}