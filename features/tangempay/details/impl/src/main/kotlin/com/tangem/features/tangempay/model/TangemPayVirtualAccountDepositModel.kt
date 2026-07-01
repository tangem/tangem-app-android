package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.features.tangempay.components.TangemPayVirtualAccountDepositComponent
import com.tangem.features.tangempay.entity.TangemPayVirtualAccountDepositUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayVirtualAccountDepositModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<TangemPayVirtualAccountDepositComponent.Params>()

    val uiState: TangemPayVirtualAccountDepositUM = TangemPayVirtualAccountDepositUM(
        fees = persistentListOf(
            TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("ACH"), value = "$1"),
            TangemPayVirtualAccountDepositUM.FeeRow(title = stringReference("FedWire"), value = "$11"),
        ),
        shouldShowTermsAndConditions = params.virtualAccountOnramp is VirtualAccountOnramp.Eligible,
        onShowDetailsClick = ::onShowDetailsClick,
        onDismiss = ::onDismiss,
        onTermsClick = { urlOpener.openUrl(TERMS_OF_USE_URL) },
        onPrivacyClick = { urlOpener.openUrl(PRIVACY_POLICY_URL) },
    )

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onShowDetailsClick() {
        when (params.virtualAccountOnramp) {
            is VirtualAccountOnramp.Available -> params.onShowDetails(params.virtualAccountOnramp)
            VirtualAccountOnramp.Eligible -> TODO()
        }
    }

    private companion object {
        const val TERMS_OF_USE_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"
        const val PRIVACY_POLICY_URL = "https://tangem.com/docs/en/pay-privacy-policy.pdf"
    }
}