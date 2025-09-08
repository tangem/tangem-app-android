
package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.hot.HotWalletAccessor

class ClearAllHotWalletContextualUnlockUseCase(
    private val hotWalletAccessor: HotWalletAccessor,
) {

    operator fun invoke(): Either<Throwable, Unit> {
        return Either.catch {
            hotWalletAccessor.clearAllContextualUnlock()
        }
    }
}