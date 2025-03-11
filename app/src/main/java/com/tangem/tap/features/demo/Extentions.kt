package com.tangem.tap.features.demo

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse

/**
[REDACTED_AUTHOR]
 */
fun ScanResponse.isDemoCard(): Boolean = DemoHelper.isDemoCardId(card.cardId)
fun CardDTO.isDemoCard(): Boolean = DemoHelper.isDemoCardId(cardId)