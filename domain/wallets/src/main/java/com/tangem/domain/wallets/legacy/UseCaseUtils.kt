package com.tangem.domain.wallets.legacy

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull

internal inline fun <Error> Raise<Error>.ensureUserWalletListManagerNotNull(
    walletsStateHolder: WalletsStateHolder,
    raise: (Throwable) -> Error,
): UserWalletsListManager {
    return ensureNotNull(
        value = walletsStateHolder.userWalletsListManager,
        raise = {
            raise(IllegalStateException("User wallets list manager not initialized"))
        },
    )
}