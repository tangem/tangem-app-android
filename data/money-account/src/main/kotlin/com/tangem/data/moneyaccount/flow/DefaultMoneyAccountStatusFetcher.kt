package com.tangem.data.moneyaccount.flow

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.pay.usecase.ResolveMoneyAccountStatusFromSummaryUseCase
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultMoneyAccountStatusFetcher @Inject constructor(
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
    private val onboardingRepository: OnboardingRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val getMoneyAccountBalanceUseCase: GetMoneyAccountBalanceUseCase,
    private val deviceSecurity: DeviceSecurityInfoProvider,
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory,
    private val resolveMoneyAccountStatusFromSummaryUseCase: ResolveMoneyAccountStatusFromSummaryUseCase,
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase,
) : MoneyAccountStatusFetcher {

    override suspend fun invoke(params: MoneyAccountStatusFetcher.Params): Either<Throwable, Unit> {
        val account = Account.Money(userWalletId = params.userWalletId)
        return Either.catchOn(dispatchers.default) {
            moneyAccountStatusesStore.store(
                userWalletId = params.userWalletId,
                status = AccountStatus.Money(
                    account = account,
                    value = fetch(params.userWalletId),
                ),
            )
        }.onLeft {
            runSuspendCatching {
                moneyAccountStatusesStore.store(
                    userWalletId = params.userWalletId,
                    status = AccountStatus.Money(
                        account = account,
                        value = MoneyAccountStatusValue.Empty,
                    ),
                )
            }
        }
    }

    private suspend fun fetch(userWalletId: UserWalletId): MoneyAccountStatusValue {
        if (deviceSecurity.isSecurityExposed()) return MoneyAccountStatusValue.Error.ExposedDevice

        val summary = getCustomerSummaryUseCase.invoke(userWalletId, CachePolicy.REFRESH).getOrNull()
            ?: return MoneyAccountStatusValue.Empty

        resolveMoneyAccountStatusFromSummaryUseCase(summary)?.let { return it }

        return fetchAccount(userWalletId)
    }

    private suspend fun fetchAccount(userWalletId: UserWalletId): MoneyAccountStatusValue = coroutineScope {
        val cryptoCurrency = moneyAccountCurrencyFactory.create(userWalletId)
        val balanceDeferred = async { getMoneyAccountBalanceUseCase.invoke(userWalletId, cryptoCurrency) }
        val channelsDeferred = async { onboardingRepository.fetchCustomerEligibility(userWalletId) }
        val customerInfo = onboardingRepository.getCustomerInfo(userWalletId).getOrElse {
            balanceDeferred.cancel()
            channelsDeferred.cancel()
            return@coroutineScope MoneyAccountStatusValue.Empty
        }
        val balance = balanceDeferred.await().getOrNull()
        val channels = channelsDeferred.await().getOrNull()
        val instance = customerInfo.moneyAccountProductInstance
        val isFormer = customerInfo.state == CustomerInfo.State.FORMER

        when {
            instance == null && isFormer -> MoneyAccountStatusValue.Empty
            isFormer || instance?.status == CustomerInfo.ProductInstance.Status.DEACTIVATED -> {
                MoneyAccountStatusValue.Account.Deactivated(
                    source = StatusSource.ACTUAL,
                    cryptoCurrency = cryptoCurrency,
                    balance = balance,
                    customerId = customerInfo.customerId,
                )
            }
            instance?.status == CustomerInfo.ProductInstance.Status.ACTIVE -> {
                MoneyAccountStatusValue.Account.Active(
                    source = StatusSource.ACTUAL,
                    cryptoCurrency = cryptoCurrency,
                    balance = balance,
                    customerId = customerInfo.customerId,
                    productInstanceId = instance.id,
                    isBankOfframpAvailable = channels?.contains(TangemPayEligibilityType.BANK_OFFRAMP),
                )
            }
            else -> MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                customerId = customerInfo.customerId,
            )
        }
    }
}