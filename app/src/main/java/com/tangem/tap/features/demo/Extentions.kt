package com.tangem.tap.features.demo

import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse

/**
[REDACTED_AUTHOR]
 */
fun ScanResponse.isDemoCard(): Boolean = DemoHelper.isDemoCardId(card.cardId)
fun Card.isDemoCard(): Boolean = DemoHelper.isDemoCardId(cardId)