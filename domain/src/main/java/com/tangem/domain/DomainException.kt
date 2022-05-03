package com.tangem.domain

import com.tangem.common.module.FbConsumeException
import com.tangem.common.module.ModuleException

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenException(override val message: String) : Throwable(message), ModuleException {

    data class SelectTokeNetworkException(val networkId: String) : AddCustomTokenException(
        "Unknown network [$networkId] should not be included in the network selection dialog."
    ), FbConsumeException

    data class UnAppropriateInitializationException(
        val of: String,
        val info: String? = null
    ) : AddCustomTokenException("The [$of], must be properly initialized. Info [$info]")
}