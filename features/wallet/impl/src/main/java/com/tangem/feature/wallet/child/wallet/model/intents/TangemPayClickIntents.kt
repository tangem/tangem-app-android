package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.feature.wallet.child.wallet.model.TangemPayMainInfoManager
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.features.tangempay.TangemPayFeatureToggles
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TangemPayIntents {

    suspend fun onPullToRefresh()

    fun onRefreshPayToken(userWalletId: UserWalletId)
}

@ModelScoped
internal class TangemPayClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val featureToggles: TangemPayFeatureToggles,
    private val onboardingRepository: OnboardingRepository,
    private val produceInitialDataTangemPay: ProduceTangemPayInitialDataUseCase,
    private val tangemPayInfoManager: TangemPayMainInfoManager,
) : BaseWalletClickIntents(), TangemPayIntents {

    override suspend fun onPullToRefresh() {
        val userWalletId = stateHolder.getSelectedWalletId()
        if (!featureToggles.isTangemPayEnabled ||
            !onboardingRepository.isTangemPayInitialDataProduced(userWalletId)
        ) {
            return
        }
        tangemPayInfoManager.refreshTangemPayInfo(userWalletId)
    }

    override fun onRefreshPayToken(userWalletId: UserWalletId) {
        modelScope.launch {
            produceInitialDataTangemPay.invoke(userWalletId)
            tangemPayInfoManager.refreshTangemPayInfo(userWalletId)
        }
    }
}