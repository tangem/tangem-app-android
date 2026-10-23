package com.tangem.tap.domain.twins

import com.tangem.common.core.TangemError

class IncompatibleTwinCard(override var customMessage: String) : TangemError(code = 50005) {
    override val messageResId: Int? = null
}