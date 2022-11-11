package com.tangem.tap.features.demo

import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse

/**
 * Created by Anton Zhilenkov on 21/02/2022.
 */
fun ScanResponse.isDemoCard(): Boolean = DemoHelper.isDemoCardId(card.cardId)
fun CardDTO.isDemoCard(): Boolean = DemoHelper.isDemoCardId(cardId)
