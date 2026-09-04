package com.tangem.features.tangempay.campaign

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class TangemPayCampaignModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val params = paramsContainer.require<TangemPayCampaignComponent.Params>()

    val uiState: StateFlow<TangemPayCampaignUM>
        field = MutableStateFlow<TangemPayCampaignUM>(TangemPayCampaignUM.Loading)

    init {
        TangemLogger.i("[TangemPay][Campaign] Opened campaign container: ${params.campaignKey}")
    }

    fun onClose() {
        modelScope.launch {
            if (userWalletsListRepository.userWalletsSync().isEmpty()) {
                router.replaceAll(AppRoute.Home())
            } else {
                router.replaceAll(AppRoute.Wallet)
            }
        }
    }
}