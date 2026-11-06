package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.features.moneyaccount.details.addfunds.options.MoneyAccountAddFundsOptionsUMConverter.Input
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountAddFundsOptionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountAddFundsOptionsComponent.Params>()

    private val converter = MoneyAccountAddFundsOptionsUMConverter(
        depositAddress = params.depositAddress,
        onSwapClick = ::onSwapClick,
        onReceiveClick = ::onReceiveClick,
        onBankTransferClick = ::onBankTransferClick,
    )

    private var input = Input()

    private val prefetchedBankCredentials: Deferred<Either<VisaApiError, List<BankCredentials>>>? =
        if (moneyAccountFeatureToggles.isMxnEnabled) modelScope.async { loadBankCredentials() } else null

    val uiState: StateFlow<MoneyAccountAddFundsOptionsUM>
        field = MutableStateFlow(converter.convert(input))

    init {
        analytics.send(MoneyAccountAnalyticsEvents.AddFundsScreenOpened())
        prefetchedBankCredentials?.let(::showRailsWhenLoaded)
    }

    fun onDismiss() {
        params.listener.onDismissAddFunds()
    }

    private fun onSwapClick() {
        sendMethodSelected(MoneyAccountAnalyticsEvents.AddFundsMethod.WALLET)
        params.listener.onSwapClick()
    }

    private fun onReceiveClick(depositAddress: String) {
        sendMethodSelected(MoneyAccountAnalyticsEvents.AddFundsMethod.EXTERNAL_WALLET)
        params.listener.onReceiveClick(depositAddress)
    }

    private fun onBankTransferClick() {
        if (input.isBankTransferLoading) return
        sendMethodSelected(MoneyAccountAnalyticsEvents.AddFundsMethod.BANK_TRANSFER)
        updateInput { current -> current.copy(isBankTransferLoading = true) }
        modelScope.launch {
            val prefetched = prefetchedBankCredentials?.await()?.takeIf { it.isRight() }
            (prefetched ?: loadBankCredentials())
                .onRight(params.listener::onBankDetailsLoaded)
                .onLeft {
                    updateInput { current -> current.copy(isBankTransferLoading = false) }
                    params.listener.onBankingDetailsLoadFailed()
                }
        }
    }

    private fun showRailsWhenLoaded(request: Deferred<Either<VisaApiError, List<BankCredentials>>>) {
        modelScope.launch {
            request.await().onRight { credentials ->
                updateInput { current -> current.copy(hasSpei = credentials.any(BankCredentials::isSpei)) }
            }
        }
    }

    private suspend fun loadBankCredentials(): Either<VisaApiError, List<BankCredentials>> {
        return getMoneyAccountBankCredentialsUseCase(
            userWalletId = params.userWalletId,
            includeAllRails = moneyAccountFeatureToggles.isMxnEnabled,
        )
    }

    private fun sendMethodSelected(method: MoneyAccountAnalyticsEvents.AddFundsMethod) {
        analytics.send(MoneyAccountAnalyticsEvents.AddFundsMethodSelected(method))
    }

    private fun updateInput(transform: (Input) -> Input) {
        input = transform(input)
        uiState.value = converter.convert(input)
    }
}