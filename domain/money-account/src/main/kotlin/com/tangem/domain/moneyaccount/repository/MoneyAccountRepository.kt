package com.tangem.domain.moneyaccount.repository

import arrow.core.Either
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.flow.Flow

interface MoneyAccountRepository {

    suspend fun getPaymentMethods(): Either<VisaApiError, List<MoneyAccountPaymentMethod>>

    fun isMainBlockHidden(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun hideMainBlock(userWalletId: UserWalletId)
}