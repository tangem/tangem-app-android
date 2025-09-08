package com.tangem.features.yieldlending.impl.promo.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.yieldlending.api.YieldLendingPromoComponent
import com.tangem.features.yieldlending.impl.promo.entity.StartEarningBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class YieldLendingPromoModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val bottomSheetNavigation: SlotNavigation<StartEarningBottomSheetConfig> = SlotNavigation()

    private val params = paramsContainer.require<YieldLendingPromoComponent.Params>()

    fun onClick() {
        bottomSheetNavigation.activate(
            configuration = StartEarningBottomSheetConfig(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.currency,
            ),
        )
    }
}