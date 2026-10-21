package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.pay.usecase.GetOnrampFeesUseCase
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesLoader
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    private val feesLoader = TangemPayOnrampFeesLoader(
        getOnrampFeesUseCase = getOnrampFeesUseCase,
        userWalletId = params.userWalletId,
        scope = modelScope,
    )

    private val requisites = requisitesConverter.convert(params.bankCredentials)

    val uiState: StateFlow<TangemPayBankTransferOnrampDetailsScreenUM>
        field = MutableStateFlow(
            TangemPayBankTransferOnrampDetailsScreenUM(
                requisites = requisites,
                fees = TangemPayOnrampFeesUM.Loading,
                dailyDepositLimit = params.bankCredentials.depositLimit?.let(dailyDepositLimitConverter::convert),
                onShareClick = ::onShareClick,
                onCloseClick = ::onCloseClick,
            ),
        )

    init {
        params.analytics.onDetailsShown()
        feesLoader.fees
            .onEach { fees -> uiState.update { state -> state.copy(fees = fees) } }
            .launchIn(modelScope)
        feesLoader.load()
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun onCopyClick(field: RequisiteField, value: String) {
        params.analytics.onFieldCopied(field)
        clipboardManager.setText(text = value, isSensitive = true)
    }

    private fun onShareClick() {
        params.analytics.onShareClicked()
        shareManager.shareText(requisites.joinToString(separator = "\n") { "${it.key}: ${it.value}" })
    }
}