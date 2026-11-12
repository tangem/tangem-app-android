package com.tangem.features.earn.impl.sheet.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.HostEarnNavigator
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.sheet.EarnYieldSheetComponent
import com.tangem.features.earn.impl.sheet.ui.state.EarnYieldSheetUM
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class EarnYieldSheetModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val registry: EarnProductsRegistry,
    summarySupplier: EarnProductSummarySupplier,
    singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    getSelectedAppCurrency: GetSelectedAppCurrencyUseCase,
    private val converter: EarnYieldSheetConverter,
    router: Router,
) : Model(), EarnYieldSheetClickIntents {

    private val params: EarnYieldSheetComponent.Params = paramsContainer.require()

    private val productParams = EarnProduct.Params(params.userWalletId, params.cryptoCurrency)

    private val navigator = HostEarnNavigator(router, params.userWalletId, params.cryptoCurrency)

    /** One handler per product for the lifetime of the sheet: a handler may keep state between clicks. */
    private val handlers = mutableMapOf<EarnProductId, EarnActionHandler>()

    /** `null` until the status is known, and when the wallet has no status for the currency. */
    private val fiatRate: Flow<BigDecimal?> = singleAccountStatusListSupplier(params.userWalletId)
        .map { it.getCryptoCurrencyStatus(params.cryptoCurrency).getOrNull()?.value?.fiatRate }
        .onStart { emit(null) }
        .distinctUntilChanged()

    val uiState: StateFlow<EarnYieldSheetUM>
        field = MutableStateFlow(converter.empty(intents = this))

    init {
        combine(
            flow = summarySupplier(params.userWalletId).map { it[params.cryptoCurrency.id] },
            flow2 = fiatRate,
            flow3 = getSelectedAppCurrency.invokeOrDefault(),
            transform = ::createUiState,
        )
            .flowOn(dispatchers.default)
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    override fun onActionClick(productId: EarnProductId, action: EarnAction) {
        val product = registry.find(productId) ?: return

        handlers
            .getOrPut(productId) { product.actionHandler(productParams, navigator) }
            .onActionClick(action)
        params.callback.onDismiss()
    }

    override fun onDismissClick() {
        params.callback.onDismiss()
    }

    private fun createUiState(
        summaries: List<EarnProductSummary>,
        fiatRate: BigDecimal?,
        appCurrency: AppCurrency,
    ): EarnYieldSheetUM = converter.convert(
        summaries = summaries,
        currency = params.cryptoCurrency,
        fiatRate = fiatRate,
        appCurrency = appCurrency,
        intents = this,
    )
}