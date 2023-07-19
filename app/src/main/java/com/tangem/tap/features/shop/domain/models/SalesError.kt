package com.tangem.tap.features.shop.domain.models

sealed class SalesError {
    data class DataError(val cause: Throwable) : SalesError()
}