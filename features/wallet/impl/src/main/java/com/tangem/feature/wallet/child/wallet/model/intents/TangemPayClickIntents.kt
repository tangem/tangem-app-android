package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.TangemPayIssueOrderUseCase
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayInitialStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayIssueAvailableStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayIssueProgressStateTransformer
import com.tangem.features.tangempay.TangemPayFeatureToggles
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface TangemPayIntents {

    suspend fun onPullToRefresh()
}

@ModelScoped
internal class TangemPayClickIntentsImplementor @Inject constructor(
    private val stateHolder: WalletStateController,
    private val mainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
    private val issueOrderUseCase: TangemPayIssueOrderUseCase,
    private val featureToggles: TangemPayFeatureToggles,
    private val onboardingRepository: OnboardingRepository,
) : BaseWalletClickIntents(), TangemPayIntents {

    override suspend fun onPullToRefresh() {
        if (!featureToggles.isTangemPayEnabled || !onboardingRepository.isTangemPayInitialDataProduced()) return
        val info = mainScreenCustomerInfoUseCase()
        val userWalletId = stateHolder.getSelectedWalletId()
        if (info != null) {
            stateHolder.update(
                transformer = TangemPayInitialStateTransformer(
                    value = info,
                    onClickIssue = ::issueOrder,
                    onClickKyc = router::openTangemPayOnboarding,
                    openDetails = { config -> router.openTangemPayDetails(userWalletId, config) },
                ),
            )
        }
    }

    private fun issueOrder() {
        modelScope.launch {
            stateHolder.update(TangemPayIssueProgressStateTransformer())
            issueOrderUseCase().onLeft {
                stateHolder.update(TangemPayIssueAvailableStateTransformer(onClickIssue = ::issueOrder))
            }
        }
    }
}