package com.tangem.domain.tangempay.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class MoneyAccountActivationData(
    val address: String,
    val signature: String,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
    val initialCredentials: TangemPayInitialCredentials,
)