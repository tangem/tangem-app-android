package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.PaymentMethodConverter
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class DefaultMoneyAccountRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestPerformer: TangemPayRequestPerformer,
    private val appPreferencesStore: AppPreferencesStore,
    private val spendPreferencesKeys: SpendPreferencesKeys,
) : MoneyAccountRepository {

    override suspend fun getPaymentMethods(): Either<VisaApiError, List<MoneyAccountPaymentMethod>> {
        return requestPerformer.performWithStaticToken {
            tangemPayApi.getPaymentMethods()
        }.map { response ->
            response.result.orEmpty().mapNotNull(PaymentMethodConverter::convert)
        }
    }

    override fun isMainBlockHidden(userWalletId: UserWalletId): Flow<Boolean> {
        return appPreferencesStore.get(
            key = spendPreferencesKeys.getMoneyAccountMainBlockHiddenKey(userWalletId),
            default = false,
        )
    }

    override suspend fun hideMainBlock(userWalletId: UserWalletId) {
        appPreferencesStore.store(spendPreferencesKeys.getMoneyAccountMainBlockHiddenKey(userWalletId), true)
    }
}