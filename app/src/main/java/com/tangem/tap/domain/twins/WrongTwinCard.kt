package com.tangem.tap.domain.twins

import com.tangem.common.core.TangemError

data class WrongTwinCard(override var customMessage: String) : TangemError(code = 50005) {
    override val messageResId: Int? = null
}