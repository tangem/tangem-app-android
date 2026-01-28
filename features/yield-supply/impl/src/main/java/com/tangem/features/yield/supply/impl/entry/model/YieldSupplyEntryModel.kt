package com.tangem.features.yield.supply.impl.entry.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.yield.supply.usecase.YieldSupplyEnterStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyEntryComponent
import com.tangem.features.yield.supply.api.entry.YieldSupplyEntryRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val yieldSupplyEnterStatusUseCase: YieldSupplyEnterStatusUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
) : Model() {

    private val params = paramsContainer.require<YieldSupplyEntryComponent.Params>()

    val initialRoute: YieldSupplyEntryRoute = YieldSupplyEntryRoute.Empty

    init {
        navigateToInitialRoute()
    }

    private fun navigateToInitialRoute() {
        val userWalletId = params.userWalletId
        val cryptoCurrency = params.cryptoCurrency
        modelScope.launch(dispatchers.default) {
            getSingleCryptoCurrencyStatusUseCase.invokeMultiWalletSync(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrency.id,
            ).onLeft { error ->
                Timber.e("Failed to get CryptoCurrencyStatus: $error")
                withContext(dispatchers.mainImmediate) {
                    router.pop()
                }
            }.onRight { cryptoCurrencyStatus ->
                withContext(dispatchers.mainImmediate) {
                    val route = getInitialRoute(cryptoCurrencyStatus)
                    if (route != null) {
                        router.replaceCurrent(route)
                    }
                }
            }
        }
    }

    private suspend fun getInitialRoute(cryptoCurrencyStatus: CryptoCurrencyStatus): YieldSupplyEntryRoute? {
        val userWalletId = params.userWalletId
        val cryptoCurrency = params.cryptoCurrency
        val token = cryptoCurrency as? CryptoCurrency.Token
        if (token == null) {
            router.pop()
            return null
        }

        val tokenEnterStatus = yieldSupplyEnterStatusUseCase(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        ).getOrNull()
        val isActiveYield = cryptoCurrencyStatus.value.yieldSupplyStatus?.isActive == true

        if (tokenEnterStatus != null) {
            router.replaceCurrent(
                AppRoute.CurrencyDetails(
                    userWalletId = userWalletId,
                    currency = token,
                    navigationAction = NavigationAction.YieldSupply(
                        isActive = isActiveYield,
                    ),
                ),
            )
            return null
        }

        return if (isActiveYield) {
            YieldSupplyEntryRoute.Active(cryptoCurrency = token)
        } else {
            YieldSupplyEntryRoute.Promo(cryptoCurrency = token, apy = params.apy)
        }
    }
}