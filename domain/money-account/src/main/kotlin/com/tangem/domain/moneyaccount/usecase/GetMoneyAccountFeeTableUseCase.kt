package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountFeeTableRow
import com.tangem.domain.moneyaccount.repository.MoneyAccountRepository
import com.tangem.domain.pay.model.AccountFeeGroupType
import com.tangem.domain.pay.usecase.GetAccountFeesUseCase
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GetMoneyAccountFeeTableUseCase(
    private val moneyAccountRepository: MoneyAccountRepository,
    private val getAccountFeesUseCase: GetAccountFeesUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, List<MoneyAccountFeeTableRow>> =
        coroutineScope {
            val paymentMethodsDeferred = async { moneyAccountRepository.getPaymentMethods() }
            val feesDeferred = async {
                getAccountFeesUseCase(
                    userWalletId = userWalletId,
                    types = setOf(AccountFeeGroupType.ONRAMP, AccountFeeGroupType.OFFRAMP),
                )
            }
            either {
                val paymentMethods = paymentMethodsDeferred.await().bind()
                val fees = feesDeferred.await().bind()
                paymentMethods.map { paymentMethod ->
                    val methodFees = fees.filter { it.paymentMethodId == paymentMethod.id }
                    MoneyAccountFeeTableRow(
                        paymentMethod = paymentMethod,
                        receiveFee = methodFees.firstOrNull { it.group == AccountFeeGroupType.ONRAMP },
                        sendFee = methodFees.firstOrNull { it.group == AccountFeeGroupType.OFFRAMP },
                    )
                }
            }
        }
}