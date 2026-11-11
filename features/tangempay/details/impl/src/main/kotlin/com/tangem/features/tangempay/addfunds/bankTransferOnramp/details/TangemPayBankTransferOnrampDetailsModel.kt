package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.tangempay.usecase.GetOnrampFeesUseCase
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesLoader
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayRouteFeeUMConverter
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.FeeSource
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayBankTransferOnrampDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
) : Model() {

    private val params = paramsContainer.require<TangemPayBankTransferOnrampDetailsComponent.Params>()

    private val requisitesConverter = TangemPayBankTransferOnrampRequisitesUMConverter(onCopyClick = ::onCopyClick)
    private val dailyDepositLimitConverter = TangemPayDailyDepositLimitUMConverter()

    val uiState: StateFlow<TangemPayBankTransferOnrampDetailsScreenUM>
        field = MutableStateFlow(
            TangemPayBankTransferOnrampDetailsScreenUM(
                tabs = persistentListOf(),
                requisites = persistentListOf(),
                referenceWarning = null,
                fees = TangemPayOnrampFeesUM.Loading,
                dailyDepositLimit = null,
                onShareClick = ::onShareClick,
                onCloseClick = ::onCloseClick,
            ).withSelectedRoute(index = 0),
        )

    init {
        params.analytics.onDetailsShown()
        val feeSource = params.feeSource
        if (feeSource is FeeSource.Catalogue) loadCatalogueFees(feeSource.product)
    }

    private fun loadCatalogueFees(product: TangemPayProduct) {
        val feesLoader = TangemPayOnrampFeesLoader(
            getOnrampFeesUseCase = getOnrampFeesUseCase,
            userWalletId = params.userWalletId,
            product = product,
            scope = modelScope,
        )
        feesLoader.fees
            .onEach { fees -> uiState.update { state -> state.copy(fees = fees) } }
            .launchIn(modelScope)
        feesLoader.load()
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun onTabClick(index: Int) {
        uiState.update { state -> state.withSelectedRoute(index) }
    }

    private fun TangemPayBankTransferOnrampDetailsScreenUM.withSelectedRoute(
        index: Int,
    ): TangemPayBankTransferOnrampDetailsScreenUM {
        val credentials = params.bankCredentials[index]
        return copy(
            tabs = createTabs(selectedIndex = index),
            requisites = requisitesConverter.convert(credentials),
            referenceWarning = resourceReference(R.string.tangempay_bank_transfer_details_reference_warning)
                .takeIf { credentials.isSpei && !credentials.transferMessage.isNullOrBlank() },
            fees = when (params.feeSource) {
                is FeeSource.Catalogue -> fees
                FeeSource.Route -> TangemPayRouteFeeUMConverter.convert(credentials)
            },
            dailyDepositLimit = dailyDepositLimitConverter.convert(credentials),
        )
    }

    private fun createTabs(selectedIndex: Int): ImmutableList<TangemTabItemUM> {
        if (params.bankCredentials.size < 2) return persistentListOf()
        return params.bankCredentials.mapIndexed { index, credentials ->
            TangemTabItemUM.Content(
                id = index.toString(),
                label = stringReference(credentials.depositCurrencyCode),
                isSelected = index == selectedIndex,
                onClick = { onTabClick(index) },
            )
        }.toImmutableList()
    }

    private fun onCopyClick(field: RequisiteField, value: String) {
        params.analytics.onFieldCopied(field)
        clipboardManager.setText(text = value, isSensitive = true)
    }

    private fun onShareClick() {
        params.analytics.onShareClicked()
        val requisites = uiState.value.requisites
        shareManager.shareText(requisites.joinToString(separator = "\n") { "${it.key}: ${it.value}" })
    }
}