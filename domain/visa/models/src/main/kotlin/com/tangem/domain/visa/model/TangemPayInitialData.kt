package com.tangem.domain.visa.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class TangemPayInitialData(
    val initialCredentials: TangemPayInitialCredentials,
    val moneyAccountDerivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap>,
)