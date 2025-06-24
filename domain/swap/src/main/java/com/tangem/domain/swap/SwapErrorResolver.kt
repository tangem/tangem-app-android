package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressError

interface SwapErrorResolver {
    fun resolve(throwable: Throwable): ExpressError
}