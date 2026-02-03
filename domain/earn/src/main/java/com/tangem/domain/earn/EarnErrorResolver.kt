package com.tangem.domain.earn

import com.tangem.domain.models.earn.EarnError

interface EarnErrorResolver {

    fun resolve(throwable: Throwable?): EarnError
}