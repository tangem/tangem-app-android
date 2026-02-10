package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusFinder
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest

/**
 * Use case to retrieve the available actions for a specific cryptocurrency associated with an account.
 *
 * @property userWalletsListRepository repository to get the user wallets list.
 * @property singleAccountStatusListSupplier supplier to get the list of account statuses.
 * @property getCryptoCurrencyActionsUseCase use case to get the actions for a specific cryptocurrency status.
 *
[REDACTED_AUTHOR]
 */
class GetCryptoCurrencyActionsUseCaseV2(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        accountId: AccountId,
        currency: CryptoCurrency,
        yieldSupplyAvailability: YieldSupplyAvailability = YieldSupplyAvailability.Unavailable,
    ): Flow<TokenActionsState> {
        return singleAccountStatusListSupplier(
            params = SingleAccountStatusListProducer.Params(userWalletId = accountId.userWalletId),
        )
            .transformLatest { accountStatusList ->
                val accountCurrencyStatus = AccountCryptoCurrencyStatusFinder(
                    accountStatusList = accountStatusList,
                    currency = currency,
                )

                if (accountCurrencyStatus != null) {
                    val userWallet = userWalletsListRepository.getSyncStrict(id = accountId.userWalletId)
                    val actionsFlow = getCryptoCurrencyActionsUseCase(
                        userWallet = userWallet,
                        cryptoCurrencyStatus = accountCurrencyStatus.status,
                        yieldSupplyAvailability = yieldSupplyAvailability,
                    )

                    emitAll(actionsFlow)
                }
            }
    }
}