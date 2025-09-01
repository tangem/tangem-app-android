
package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.HotWalletId

class ClearHotWalletContextualUnlockUseCase(
    private val hotWalletAccessor: HotWalletAccessor,
) {

    operator fun invoke(hotWalletId: HotWalletId): Either<Throwable, Unit> {
        return Either.catch {
            hotWalletAccessor.clearContextualUnlock(hotWalletId)
        }
    }
}