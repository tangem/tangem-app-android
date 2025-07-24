package com.tangem.domain.demo

import com.tangem.domain.demo.models.DemoConfig

class IsDemoCardUseCase(private val config: DemoConfig) {

    operator fun invoke(cardId: String): Boolean = config.isDemoCardId(cardId)
}