package com.tangem.domain.demo

class IsDemoCardUseCase(private val config: DemoConfig) {

    operator fun invoke(cardId: String): Boolean = config.isDemoCardId(cardId)
}
