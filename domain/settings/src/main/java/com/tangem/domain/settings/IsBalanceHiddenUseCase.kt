package com.tangem.domain.settings

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class IsBalanceHiddenUseCase {

    // TODO add flip trigger https://tangem.atlassian.net/browse/AND-4476
    operator fun invoke(): Flow<Boolean> {
        return flow {
            while (true) {
                emit(false)
                delay(3000)
            // var value = true
            // while (true) {
            //     emit(value)
            //     value = !value
            //     delay(3000)
            }
        }
    }

}
