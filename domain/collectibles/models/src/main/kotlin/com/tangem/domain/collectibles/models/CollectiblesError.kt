package com.tangem.domain.collectibles.models

sealed class CollectiblesError {

    data class Api(val cause: Throwable) : CollectiblesError()

    data class Unknown(val cause: Throwable) : CollectiblesError()
}