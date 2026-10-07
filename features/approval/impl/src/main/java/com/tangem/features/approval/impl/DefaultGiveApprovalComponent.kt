package com.tangem.features.approval.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.impl.model.GiveApprovalModel
import com.tangem.features.approval.impl.model.GiveApprovalUM
import com.tangem.features.approval.impl.ui.ApprovalAmountRow
import com.tangem.features.approval.impl.ui.ApprovalConfirmButton
import com.tangem.features.approval.impl.ui.ApprovalRows
import com.tangem.features.approval.impl.ui.ApprovalSubtitle
import com.tangem.features.approval.impl.ui.GiveApprovalContent
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultGiveApprovalComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: GiveApprovalComponent.Params,
    feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : GiveApprovalComponent, AppComponentContext by appComponentContext {

    private val model: GiveApprovalModel = getOrCreateModel(params = params)

    private val feeSelectorBlockComponent = feeSelectorBlockComponentFactory.create(
        context = child("giveApprovalFeeSelector"),
        params = FeeSelectorParams.FeeSelectorBlockParams(
            state = FeeSelectorUM.Loading,
            onLoadFee = { model.loadFee() },
            onDisableCustomFee = { model.shouldDisableCustomFee() },
            onLoadFeeExtended = { selectedFeeToken -> model.loadFeeExtended(selectedFeeToken) },
            feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            feeStateConfiguration = FeeSelectorParams.FeeStateConfiguration.None,
            feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
            analyticsCategoryName = CommonSendAnalyticEvents.APPROVE_CATEGORY,
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Approve,
            userWalletId = params.userWalletId,
        ),
        onResult = model::onFeeResult,
    )

    private val currency: String = params.cryptoCurrencyStatus.currency.symbol

    override fun dismiss() {
        params.callback.onCancelClick()
    }

    @Composable
    override fun BottomSheet() {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        val config = remember {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = config,
            containerColor = TangemTheme.colors.background.tertiary,
            titleText = resourceReference(uiState.titleRes()),
            titleAction = TopAppBarButtonUM.Icon(
                iconRes = R.drawable.ic_close_new_20,
                onClicked = model::onCancelClick,
            ),
        ) {
            GiveApprovalContent(
                currency = currency,
                amountFooter = params.amountFooter,
                feeFooter = params.feeFooter,
                uiState = uiState,
                onChangeApproveType = model::onChangeApproveType,
                onApproveClick = model::onApproveClick,
                onOpenLearnMoreAboutApproveClick = model::onOpenLearnMoreAboutApproveClick,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
            )
        }
    }

    @Composable
    override fun Title() {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        TangemModal.TopNavigation(
            title = resourceReference(uiState.titleRes()),
            onClose = model::onCancelClick,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
            ApprovalSubtitle(
                text = params.amountFooter,
                onLearnMoreClick = model::onOpenLearnMoreAboutApproveClick,
            )
            ApprovalRows(
                amountRow = {
                    ApprovalAmountRow(
                        currency = currency,
                        approveType = uiState.approveType,
                        approveItems = uiState.approveItems,
                        onChangeApproveType = model::onChangeApproveType,
                    )
                },
                feeRow = { rowModifier -> feeSelectorBlockComponent.Content(rowModifier) },
                feeNote = resourceReference(R.string.update_approval_permission_fee_note)
                    .takeIf { uiState.isResetApproval },
            )
        }
    }

    @Composable
    override fun Footer() {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        ApprovalConfirmButton(
            text = if (uiState.isResetApproval) {
                resourceReference(R.string.update_approval_permission_title)
            } else {
                resourceReference(R.string.common_confirm)
            },
            isEnabled = uiState.isApproveButtonEnabled,
            isLoading = uiState.isApproveLoading,
            isHoldToConfirm = uiState.isHoldToConfirm,
            walletInteractionIcon = uiState.walletInteractionIcon,
            onClick = model::onApproveClick,
        )
    }

    private fun GiveApprovalUM.titleRes(): Int = if (isResetApproval) {
        R.string.update_approval_permission_title
    } else {
        R.string.give_permission_title
    }

    @AssistedFactory
    interface Factory : GiveApprovalComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: GiveApprovalComponent.Params,
        ): DefaultGiveApprovalComponent
    }
}