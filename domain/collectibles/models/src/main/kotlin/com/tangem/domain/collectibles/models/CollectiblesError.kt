package com.tangem.domain.collectibles.models

sealed class CollectiblesError {

    abstract val cause: Throwable

    data class Api(override val cause: Throwable) : CollectiblesError()

    data class Unknown(override val cause: Throwable) : CollectiblesError()
}