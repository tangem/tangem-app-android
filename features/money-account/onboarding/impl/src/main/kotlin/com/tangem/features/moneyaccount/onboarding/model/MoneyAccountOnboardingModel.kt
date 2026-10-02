package com.tangem.features.moneyaccount.onboarding.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountFeeTableUseCase
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val onboardingRepository: OnboardingRepository,
    private val getMoneyAccountFeeTableUseCase: GetMoneyAccountFeeTableUseCase,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountOnboardingComponent.Params>()

    val uiState: StateFlow<MoneyAccountOnboardingUM>
        field = MutableStateFlow<MoneyAccountOnboardingUM>(MoneyAccountOnboardingUM.Loading(onBack = ::back))

    init {
        when (params) {
            is MoneyAccountOnboardingComponent.Params.Deeplink -> validateDeeplinkAndShow(params.deeplink)
            MoneyAccountOnboardingComponent.Params.FromMain,
            MoneyAccountOnboardingComponent.Params.FromDetailsScreen,
            -> showOnboarding()
        }
    }

    private fun validateDeeplinkAndShow(deeplink: String) {
        modelScope.launch {
            onboardingRepository.validateDeeplink(deeplink)
                .onRight { isValid -> if (isValid) showOnboarding() else back() }
                .onLeft { back() }
        }
    }

    private fun showOnboarding() {
        uiState.update {
            MoneyAccountOnboardingUM.Content(
                onBack = ::back,
                isLoading = false,
                fees = null,
                onGetStartedClick = ::onGetStartedClick,
                onTermsAndFeesClick = ::showFees,
                onTermsClick = ::onTermsClick,
                onFeesDismiss = ::hideFees,
            )
        }
    }

    private fun showFees() {
        updateContent { it.copy(fees = MoneyAccountOnboardingUM.FeesUM.Loading) }
        loadFees()
    }

    private fun loadFees() {
        modelScope.launch {
            val fees = getMoneyAccountFeeTableUseCase().fold(
                ifLeft = { MoneyAccountOnboardingUM.FeesUM.Error(onRetryClick = ::onRetryFeesClick) },
                ifRight = { paymentMethods ->
                    MoneyAccountOnboardingUM.FeesUM.Content(paymentMethods.map(::toFeeRow).toImmutableList())
                },
            )
            updateContent { content -> if (content.fees == null) content else content.copy(fees = fees) }
        }
    }

    private fun onRetryFeesClick() {
        updateContent { it.copy(fees = MoneyAccountOnboardingUM.FeesUM.Loading) }
        loadFees()
    }

    private fun toFeeRow(paymentMethod: MoneyAccountPaymentMethod) = MoneyAccountOnboardingUM.FeeRowUM(
        icon = paymentMethod.iconUrl?.let { url ->
            TangemIconUM.Url(
                url = url,
                fallbackRes = R.drawable.ic_empty_64,
            )
        } ?: TangemIconUM.Empty,
        provider = paymentMethod.name,
        receive = formatFee(paymentMethod.receiveFee),
        send = formatFee(paymentMethod.sendFee),
    )

    private fun formatFee(fee: MoneyAccountPaymentMethod.Fee?): String {
        if (fee == null) return DASH_SIGN
        val currency = getJavaCurrencyByCode(fee.currency)
        return fee.amount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
    }

    private fun hideFees() {
        updateContent { it.copy(fees = null) }
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TERMS_OF_USE_URL)
    }

    private fun onGetStartedClick() {
        router.push(
            AppRoute.TangemPayEligibility(
                product = TangemPayProduct.BANKING_ACCOUNT,
                userWalletId = null,
            ),
        )
    }

    private fun updateContent(block: (MoneyAccountOnboardingUM.Content) -> MoneyAccountOnboardingUM.Content) {
        uiState.update { state ->
            when (state) {
                is MoneyAccountOnboardingUM.Content -> block(state)
                is MoneyAccountOnboardingUM.Loading -> state
            }
        }
    }

    private fun back() {
        router.pop()
    }

    private companion object {
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
    }
}