package com.tangem.domain.card

import com.tangem.domain.demo.DemoConfig

class IsDemoCardUseCase(private val config: DemoConfig) {

    operator fun invoke(cardId: String): Boolean = config.isDemoCardId(cardId)
}
