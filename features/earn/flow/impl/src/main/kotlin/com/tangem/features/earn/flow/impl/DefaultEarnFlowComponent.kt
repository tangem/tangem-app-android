package com.tangem.features.earn.flow.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.earn.api.ui.EarnHowItWorksModal
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.impl.model.EarnFlowModel
import com.tangem.features.earn.flow.impl.model.EarnFlowStep
import com.tangem.features.earn.flow.impl.ui.EarnAmountContent
import com.tangem.features.earn.flow.impl.ui.EarnConfirmContent
import com.tangem.features.earn.flow.impl.ui.EarnFlowLoadingContent
import com.tangem.features.earn.flow.impl.ui.EarnResultContent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class DefaultEarnFlowComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: EarnFlowComponent.Params,
    private val feeSelectorBlockFactory: FeeSelectorBlockComponent.Factory,
) : EarnFlowComponent, AppComponentContext by appComponentContext {

    private val model: EarnFlowModel = getOrCreateModel(params)

    private val stack = childStack(
        key = "earnFlowStack",
        source = model.navigation,
        serializer = null,
        initialStack = model::restoredStack,
        handleBackButton = false,
        childFactory = { step, _ -> step },
    )

    /** One block for both screens: the reload trigger is global, so a second block would reload together with it. */
    private val feeSelectorBlock: StateFlow<FeeSelectorBlockComponent?>
        field = MutableStateFlow(null)

    init {
        backHandler.register(BackCallback(onBack = model::onBack))

        componentScope.launch {
            val (status, feeStatus) = model.state
                .map { state ->
                    val status = state.status?.takeIf { it.value !is CryptoCurrencyStatus.Loading }
                    val feeStatus = state.feeStatus
                    if (status != null && feeStatus != null) status to feeStatus else null
                }
                .filterNotNull()
                .first()
            feeSelectorBlock.value = createFeeSelectorBlock(status, feeStatus)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stackState by stack.subscribeAsState()
        val feeBlock by feeSelectorBlock.collectAsStateWithLifecycle()

        when (stackState.active.configuration) {
            EarnFlowStep.Amount -> {
                val amountUM by model.amount.collectAsStateWithLifecycle()
                amountUM?.let { um ->
                    EarnAmountContent(
                        state = um,
                        feeBlock = feeBlock,
                        onBack = model::onBack,
                        onClose = model::onClose,
                        modifier = modifier,
                    )
                } ?: EarnFlowLoadingContent(modifier)
            }
            EarnFlowStep.Confirm -> {
                val confirmUM by model.confirm.collectAsStateWithLifecycle()
                confirmUM?.let { um ->
                    EarnConfirmContent(
                        state = um,
                        title = model.action.confirmTitle(params.flowProduct.title),
                        feeBlock = feeBlock,
                        onBack = model::onBack,
                        onClose = model::onClose,
                        modifier = modifier,
                    )
                } ?: EarnFlowLoadingContent(modifier)
            }
            EarnFlowStep.Result -> {
                val resultUM by model.result.collectAsStateWithLifecycle()
                resultUM?.let { um -> EarnResultContent(state = um, modifier = modifier) }
                    ?: EarnFlowLoadingContent(modifier)
            }
        }

        val howItWorks by model.howItWorks.collectAsStateWithLifecycle()
        howItWorks?.let { um -> EarnHowItWorksModal(state = um, onDismiss = model::onHowItWorksDismiss) }
    }

    private fun createFeeSelectorBlock(
        status: CryptoCurrencyStatus,
        feeStatus: CryptoCurrencyStatus,
    ): FeeSelectorBlockComponent = feeSelectorBlockFactory.create(
        context = child(key = "earnFeeSelector"),
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = FeeSelectorUM.Loading,
            userWalletId = params.product.userWalletId,
            onLoadFee = model::loadFee,
            onLoadFeeExtended = if (model.action.isGaslessAllowed) model::loadFeeExtended else null,
            cryptoCurrencyStatus = status,
            feeCryptoCurrencyStatus = feeStatus,
            feeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
            blockAppearance = FeeSelectorParams.BlockAppearance.Row,
            analyticsCategoryName = params.flowProduct.analytics.category,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Earning,
        ),
        onResult = model::onFeeResult,
    )

    @AssistedFactory
    interface Factory : EarnFlowComponent.Factory {
        override fun create(context: AppComponentContext, params: EarnFlowComponent.Params): DefaultEarnFlowComponent
    }
}