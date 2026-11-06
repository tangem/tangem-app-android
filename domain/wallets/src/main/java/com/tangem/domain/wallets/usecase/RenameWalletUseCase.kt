package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.models.UpdateWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.runSuspendCatching

class RenameWalletUseCase(
    private val walletsRepository: WalletsRepository,
    private val userWalletsSyncDelegate: UserWalletsSyncDelegate,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, name: String): Either<UpdateWalletError, UserWallet> =
        either {
            runSuspendCatching {
                walletsRepository.setWalletName(userWalletId, name)
            }

            userWalletsSyncDelegate.syncWallet(userWalletId, name).bind()
        }
}