package com.tangem.tap.domain.walletregistration

import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.models.scan.CardDTO

/**
 * Whether the card's wallet is registered with the Tangem Auth Service: a twin's id is derived from both cards, so it
 * can't be built from one.
 */
internal val CardDTO.isColdRegistrationSupported: Boolean
    get() = !isTangemTwins