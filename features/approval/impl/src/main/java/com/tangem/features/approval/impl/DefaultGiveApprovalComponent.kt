package com.tangem.features.approval.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.impl.model.GiveApprovalModel
import com.tangem.features.approval.impl.ui.GiveApprovalContent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.tangem.common.ui.R as CommonUiR

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
            containerColor = TangemTheme.colors.background.secondary,
            titleText = resourceReference(CommonUiR.string.give_permission_title),
            titleAction = TopAppBarButtonUM.Icon(
                iconRes = CommonUiR.drawable.ic_information_24,
                onClicked = model::showPermissionInfoDialog,
            ),
        ) {
            GiveApprovalContent(
                currency = currency,
                subtitle = params.subtitle,
                approveType = uiState.approveType,
                approveItems = uiState.approveItems,
                onChangeApproveType = model::onChangeApproveType,
                walletInteractionIcon = uiState.walletInteractionIcon,
                isApproveEnabled = uiState.isApproveButtonEnabled,
                isApproveLoading = uiState.isApproveLoading,
                onApproveClick = model::onApproveClick,
                onCancelClick = model::onCancelClick,
                onOpenLearnMoreAboutApproveClick = model::onOpenLearnMoreAboutApproveClick,
                feeSelectorBlockComponent = feeSelectorBlockComponent,
            )
        }
    }

    @AssistedFactory
    interface Factory : GiveApprovalComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: GiveApprovalComponent.Params,
        ): DefaultGiveApprovalComponent
    }
}