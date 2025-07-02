package com.tangem.domain.express

import com.tangem.domain.express.models.ExpressError

/**
 * Common express error resolver
 */
interface ExpressErrorResolver {

    fun resolve(throwable: Throwable): ExpressError
}