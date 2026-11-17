package com.tangem.features.moneyaccount.main.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.moneyaccount.model.IssueMoneyAccountError
import com.tangem.domain.moneyaccount.usecase.IssueMoneyAccountUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.moneyaccount.main.component.MoneyAccountActivationComponent
import com.tangem.features.moneyaccount.main.ui.state.MoneyAccountActivationUM
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MoneyAccountActivationModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val issueMoneyAccountUseCase: IssueMoneyAccountUseCase,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountActivationComponent.Params>()
    private val activationJobHolder = JobHolder()

    val uiState: StateFlow<MoneyAccountActivationUM>
        field = MutableStateFlow(
            MoneyAccountActivationUM(
                isColdWallet = isColdWallet(),
                isActivationInProgress = false,
                onActivateClick = ::onActivateClick,
                onTermsClick = ::onTermsClick,
                onDismiss = ::onDismiss,
            ),
        )

    fun onDismiss() {
        params.onDismiss()
    }

    private fun onActivateClick() {
        if (uiState.value.isActivationInProgress) return

        modelScope.launch {
            uiState.update { it.copy(isActivationInProgress = true) }
            issueMoneyAccountUseCase(params.userWalletId)
                .onRight {
                    params.onDismiss()
                    router.push(
                        AppRoute.TangemPayOnboardingSuccess(
                            product = AppRoute.TangemPayOnboardingSuccess.Product.MONEY_ACCOUNT,
                        ),
                    )
                }
                .onLeft { error ->
                    uiState.update { it.copy(isActivationInProgress = false) }
                    if (error != IssueMoneyAccountError.UserCancelled) {
                        showErrorDialog()
                    }
                }
        }.saveIn(activationJobHolder)
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TangemPayConstants.tangemPayLink())
    }

    private fun showErrorDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
            ),
        )
    }

    private fun isColdWallet(): Boolean {
        val userWallet = getUserWalletUseCase(params.userWalletId).getOrElse { return false }
        return userWallet is UserWallet.Cold
    }
}