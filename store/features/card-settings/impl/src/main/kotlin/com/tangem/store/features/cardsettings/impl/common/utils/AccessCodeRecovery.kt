package com.tangem.store.features.cardsettings.impl.common.utils

import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.models.scan.CardDTO

internal fun isAccessCodeRecoveryEnabled(typeResolver: CardTypesResolver, card: CardDTO): Boolean =
    if (typeResolver.isWallet2()) {
        card.userSettings?.isUserCodeRecoveryAllowed == true
    } else {
        false
    }