package com.tangem.sdk.api

import com.tangem.common.card.Card
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.KeyWalletPublicKey
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class CreateProductWalletTaskResponse(
    val card: CardDTO,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
) : CommandResponse {
    constructor(
        card: Card,
        derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
        primaryCard: PrimaryCard? = null,
    ) : this(
        card = CardDTO(card),
        derivedKeys = derivedKeys,
        primaryCard = primaryCard,
    )
}