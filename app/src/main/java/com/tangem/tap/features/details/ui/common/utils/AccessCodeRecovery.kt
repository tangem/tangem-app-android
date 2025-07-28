package com.tangem.tap.features.details.ui.common.utils

import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO

internal fun isAccessCodeRecoveryAllowed(typeResolver: CardTypesResolver): Boolean = typeResolver.isWallet2()

internal fun isAccessCodeRecoveryEnabled(typeResolver: CardTypesResolver, card: CardDTO): Boolean =
    if (typeResolver.isWallet2()) {
        card.userSettings?.isUserCodeRecoveryAllowed ?: false
    } else {
        false
    }