package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.error.OnrampError

interface OnrampErrorResolver {

    fun resolve(throwable: Throwable): OnrampError
}