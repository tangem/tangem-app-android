package com.tangem.domain.tokens.operations

class CurrenciesStatusesOperations {

    sealed class Error {

        data object EmptyCurrencies : Error()

        data object EmptyQuotes : Error()

        data object EmptyNetworksStatuses : Error()

        data object EmptyAddresses : Error()

        data object UnableToCreateCurrencyStatus : Error()

        data class DataError(val cause: Throwable) : Error()

        data object EmptyYieldBalances : Error()
    }
}