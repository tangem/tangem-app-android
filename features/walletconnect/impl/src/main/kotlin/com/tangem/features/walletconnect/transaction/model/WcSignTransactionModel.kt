package com.tangem.features.walletconnect.transaction.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep
import com.tangem.domain.walletconnect.usecase.method.WcSignUseCase
import com.tangem.features.walletconnect.transaction.components.WcSignTransactionContainerComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.sign.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.utils.toUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcSignTransactionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val useCaseFactory: WcRequestUseCaseFactory,
) : Model() {

    private val _uiState = MutableStateFlow<WcSignTransactionUM?>(null)
    val uiState: StateFlow<WcSignTransactionUM?> = _uiState

    private val params = paramsContainer.require<WcSignTransactionContainerComponent.Params>()

    init {
        modelScope.launch {
            val useCase: WcSignUseCase<*> = useCaseFactory.createUseCase(params.rawRequest)
            useCase.invoke()
                .onEach { signState ->
                    if (signingIsDone(signState)) return@onEach
                    val signTransactionUM = useCase.toUM(
                        signState = signState,
                        actions = WcTransactionActionsUM(
                            onShowVerifiedAlert = ::showVerifiedAlert,
                            onDismiss = { cancel(useCase) },
                            onSign = useCase::sign,
                            onCopy = { copyData(useCase.rawSdkRequest.request.params) },
                        ),
                    )
                    _uiState.emit(signTransactionUM)
                }
                .launchIn(this)
        }
    }

    fun dismiss() {
        _uiState.value?.transaction?.onDismiss?.invoke() ?: router.pop()
    }

    private fun showVerifiedAlert(appName: String) {
        val message = messageBottomSheetUM {
            infoBlock {
                icon(R.drawable.img_approvale2_20) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Unspecified
                }
                title = resourceReference(R.string.wc_alert_verified_domain_title)
                body = resourceReference(R.string.wc_alert_verified_domain_description, wrappedList(appName))
            }
            secondaryButton {
                text = resourceReference(R.string.common_done)
                onClick { closeBs() }
            }
        }
        // TODO(wc): Nastya [REDACTED_JIRA]
        // router.push(WcAppInfoRoutes.Alert(elements = message.elements))
    }

    private fun signingIsDone(signState: WcSignState<*>): Boolean {
        (signState.domainStep as? WcSignStep.Result)?.result?.let {
            router.pop()
            return true
        }
        return false
    }

    private fun cancel(useCase: WcSignUseCase<*>) {
        useCase.cancel()
        router.pop()
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text, isSensitive = true)
    }
}