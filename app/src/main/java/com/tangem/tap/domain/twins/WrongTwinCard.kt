package com.tangem.tap.domain.twins

import com.tangem.common.core.TangemError
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R

data class WrongTwinCard(private val twinCardNumber: TwinCardNumber) : TangemError(code = 50005) {
    override var customMessage: String = tangemSdkManager.getString(
        R.string.twin_error_same_card,
        twinCardNumber.number,
    )
    override val messageResId: Int? = null
}
