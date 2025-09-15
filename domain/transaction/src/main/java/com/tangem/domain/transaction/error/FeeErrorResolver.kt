package com.tangem.domain.transaction.error

interface FeeErrorResolver {

    fun resolve(throwable: Throwable): GetFeeError
}