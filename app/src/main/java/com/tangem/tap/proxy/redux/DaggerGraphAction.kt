package com.tangem.tap.proxy.redux

import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetActivityDependencies(
        val scanCardUseCase: ScanCardUseCase,
        val cardSdkConfigRepository: CardSdkConfigRepository,
    ) : DaggerGraphAction
}