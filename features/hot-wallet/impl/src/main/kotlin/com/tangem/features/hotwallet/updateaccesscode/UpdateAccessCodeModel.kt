package com.tangem.features.hotwallet.updateaccesscode

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.features.hotwallet.UpdateAccessCodeComponent
import com.tangem.features.hotwallet.accesscode.AccessCodeComponent
import com.tangem.features.hotwallet.setupfinished.MobileWalletSetupFinishedComponent
import com.tangem.features.hotwallet.updateaccesscode.routing.UpdateAccessCodeRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class UpdateAccessCodeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
    private val setAccessCodeSkippedUseCase: SetAccessCodeSkippedUseCase,
) : Model(), AccessCodeComponent.ModelCallbacks {

    private val params = paramsContainer.require<UpdateAccessCodeComponent.Params>()

    private var isAccessCodeUpdateStarted = false

    val stackNavigation = StackNavigation<UpdateAccessCodeRoute>()
    val startRoute: UpdateAccessCodeRoute = UpdateAccessCodeRoute.SetAccessCode(params.userWalletId)
    val currentRoute: MutableStateFlow<UpdateAccessCodeRoute> = MutableStateFlow(startRoute)
    val mobileWalletSetupFinishedComponentModelCallbacks = MobileWalletSetupFinishedComponentModelCallbacks()

    init {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.AccessCodeScreenOpened(source = params.source))
    }

    fun onChildBack() {
        when (currentRoute.value) {
            is UpdateAccessCodeRoute.SetAccessCode -> router.pop()
            is UpdateAccessCodeRoute.ConfirmAccessCode -> stackNavigation.pop()
            is UpdateAccessCodeRoute.SetupFinished -> Unit
        }
    }

    fun isBackButtonVisible(route: UpdateAccessCodeRoute): Boolean = when (route) {
        is UpdateAccessCodeRoute.SetAccessCode -> params.shouldShowBackButton
        is UpdateAccessCodeRoute.ConfirmAccessCode -> true
        is UpdateAccessCodeRoute.SetupFinished -> false
    }

    fun isSkipButtonVisible(route: UpdateAccessCodeRoute): Boolean = params.canSkip &&
        when (route) {
            is UpdateAccessCodeRoute.SetAccessCode,
            is UpdateAccessCodeRoute.ConfirmAccessCode,
            -> true
            is UpdateAccessCodeRoute.SetupFinished -> false
        }

    fun onSkipClick() {
        if (isAccessCodeUpdateStarted) return
        showSkipAccessCodeWarningDialog()
    }

    override fun onNewAccessCodeInput(userWalletId: UserWalletId, accessCode: String) {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ReEnterAccessCodeScreen(source = params.source))
        stackNavigation.push(UpdateAccessCodeRoute.ConfirmAccessCode(userWalletId, accessCode))
    }

    override fun onAccessCodeUpdateStarted(userWalletId: UserWalletId) {
        isAccessCodeUpdateStarted = true
    }

    override fun onAccessCodeUpdated(userWalletId: UserWalletId) {
        stackNavigation.push(UpdateAccessCodeRoute.SetupFinished)
    }

    private fun showSkipAccessCodeWarningDialog() {
        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(R.string.access_code_alert_skip_description),
                title = resourceReference(R.string.access_code_alert_skip_title),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_cancel),
                    onClick = {},
                ),
                secondAction = EventMessageAction(
                    title = resourceReference(R.string.access_code_alert_skip_ok),
                    onClick = {
                        modelScope.launch(NonCancellable) {
                            setAccessCodeSkippedUseCase(params.userWalletId, true)
                        }
                        stackNavigation.push(UpdateAccessCodeRoute.SetupFinished)
                    },
                ),
                shouldDismissOnFirstAction = true,
            ),
        )
    }

    inner class MobileWalletSetupFinishedComponentModelCallbacks : MobileWalletSetupFinishedComponent.ModelCallbacks {
        override fun onFinishClick() {
            val nextScreen = params.nextScreen
            if (nextScreen != null) {
                router.replaceCurrent(nextScreen)
            } else {
                router.pop()
            }
        }
    }
}