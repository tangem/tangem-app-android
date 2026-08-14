package com.tangem.features.tangempay.multichain.othernetworks

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class PaymentOtherNetworksModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PaymentOtherNetworksComponent.Params>()

    val uiState: PaymentOtherNetworksUM = PaymentOtherNetworksUM(
        title = resourceReference(R.string.tangempay_other_networks_title),
        subtitle = resourceReference(R.string.tangempay_other_networks_subtitle),
        onClose = params.onDismiss,
    )

    fun onDismiss() {
        params.onDismiss()
    }
}