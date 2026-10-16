package com.tangem.features.moneyaccount.details

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MoneyAccountDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val getExploreUrlUseCase: GetExploreUrlUseCase,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountDetailsComponent.Params>()
    val txHistorySlotNavigation = SlotNavigation<CryptoCurrency.Token>()
    private val stateFactory = MoneyAccountDetailsStateFactory(
        onTermsClick = ::onTermsClick,
        onSupportClick = ::onSupportClick,
        onBackClick = ::onBackClick,
    )
    val uiState: StateFlow<MoneyAccountDetailsUM>
        field = MutableStateFlow(stateFactory.createInitialState())

    private var currentState: MoneyAccountStatusValue.Account? = null

    init {
        moneyAccountStatusSupplier.invoke(params.userWalletId)
            .map { it.value }
            .onEach { value ->
                when (value) {
                    is MoneyAccountStatusValue.Account -> {
                        currentState = value
                        txHistorySlotNavigation.activate(value.cryptoCurrency)
                        uiState.update { stateFactory.createAccountState(value) }
                    }
                    else -> router.pop()
                }
            }
            .launchIn(modelScope)
    }

    private fun onBackClick() {
        router.pop()
    }

    fun onExploreClick() {
        val currency = currentState?.cryptoCurrency ?: return
        modelScope.launch {
            val url = getExploreUrlUseCase(userWalletId = params.userWalletId, currency = currency)
            if (url.isNotBlank()) urlOpener.openUrl(url)
        }
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TERMS_OF_USE_URL)
    }

    private fun onSupportClick() {
        val customerId = currentState?.customerId ?: return
        modelScope.launch {
            sendFeedbackEmailUseCase(
                type = FeedbackEmailType.Visa.DirectUserRequest(
                    walletMetaInfo = WalletMetaInfo(userWalletId = params.userWalletId),
                    customerId = customerId,
                ),
            )
        }
    }

    private companion object {
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
    }
}