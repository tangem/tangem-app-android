package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions

/**
 * Bank off-ramp catalog: the recipients the customer has saved and the routes a withdrawal can take.
 *
 * Every function is total: failures come back as [BankOfframpError], never thrown. Placing an order is
 * [BankOfframpOrderRepository].
 */
interface BankOfframpRepository {

    suspend fun getBankAccounts(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>>

    suspend fun getOptions(userWalletId: UserWalletId): Either<BankOfframpError, BankOfframpOptions>
}