package com.tangem.features.yieldlending.impl.subcomponents.active.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.features.yieldlending.impl.subcomponents.active.YieldLendingActiveComponent
import com.tangem.features.yieldlending.impl.subcomponents.active.entity.YieldLendingActiveContentUM
import com.tangem.features.yieldlending.impl.subcomponents.active.entity.YieldLendingActiveUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class YieldLendingActiveModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: YieldLendingActiveComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow

    val uiState: StateFlow<YieldLendingActiveUM>
        field = MutableStateFlow(
            YieldLendingActiveUM(
                TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = params.onDismiss,
                    content = YieldLendingActiveContentUM.Main(
                        totalEarnings = stringReference("0"),
                        availableBalance = stringReference(
                            cryptoCurrencyStatusFlow.value.value.amount.format {
                                crypto(cryptoCurrency = cryptoCurrencyStatusFlow.value.currency)
                            },
                        )
                    )
                )
            )
        )

    fun onCloseClick() {

    }

    fun onBackClick() {

    }

    fun onClick() {

    }
}