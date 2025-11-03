package com.tangem.domain.account.tokens

import arrow.core.Either
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Interface for migrating tokens associated with a main account,
 * but belonging to another account according to the derivation path.
 *
[REDACTED_AUTHOR]
 */
interface MainAccountTokensMigration {

    /**
     * Migration of non-native tokens from the main account to the account with the provided [derivationIndex].
     * If there are no such tokens, the function will complete successfully.
     *
     * @param userWalletId The unique identifier of the user's wallet.
     * @param derivationIndex The derivation index associated with the account.
     */
    suspend fun migrate(userWalletId: UserWalletId, derivationIndex: DerivationIndex): Either<Throwable, Unit>
}