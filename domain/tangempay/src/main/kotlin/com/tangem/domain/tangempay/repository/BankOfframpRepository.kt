package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.tangempay.model.bankofframp.BankRail

/**
 * Bank off-ramp catalog: the recipients the customer has saved and the routes a withdrawal can take.
 *
 * Every function is total: failures come back as [BankOfframpError], never thrown. Placing an order is
 * [BankOfframpOrderRepository].
 */
interface BankOfframpRepository {

    suspend fun getBankAccounts(
        userWalletId: UserWalletId,
        rail: BankRail? = null,
    ): Either<BankOfframpError, List<BankAccount>>

    suspend fun getOptions(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<BankOfframpError, BankOfframpOptions>
}