package com.tangem.features.virtualaccount.main

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.virtualaccount.details.component.VirtualAccountMainComponent
import com.tangem.features.virtualaccount.details.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class VirtualAccountMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    @Suppress("UnusedPrivateProperty")
    private val params = paramsContainer.require<VirtualAccountMainComponent.Params>()

    val uiState: StateFlow<VirtualAccountMainUM>
        field = MutableStateFlow(
            createInitialState(),
        )

    private fun createInitialState(): VirtualAccountMainUM = VirtualAccountMainUM(
        title = resourceReference(R.string.virtual_account_title),
        subtitle = resourceReference(R.string.tangempay_usdc_on_polygon_network),
        balance = VirtualAccountBalanceBlockState.Content(
            fiatBalance = stringReference("$0.00"),
            isBalanceFlickering = false,
        ),
        isBalanceHidden = false,
        onBackClick = { router.pop() },
        onMenuClick = {},
        onAddFundsClick = {},
        onSendClick = {},
    )
}