package com.tangem.feature.onboarding.data.model

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
[REDACTED_AUTHOR]
 */
data class CreateWalletResponse(
    val card: CardDTO,
    val derivedKeys: Map<ByteArrayKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
)