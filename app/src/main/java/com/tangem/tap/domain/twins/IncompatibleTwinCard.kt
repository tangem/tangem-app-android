package com.tangem.tap.domain.twins

import com.tangem.common.core.TangemError
import com.tangem.tap.tangemSdkManager
import com.tangem.wallet.R

object IncompatibleTwinCard : TangemError(code = 50005) {
    override var customMessage: String = tangemSdkManager.getString(
        R.string.twin_error_wrong_twin,
    )
    override val messageResId: Int? = null
}
