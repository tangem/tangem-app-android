package com.tangem.features.yield.supply.impl.incentives.claim

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.HoldToConfirmButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimModel
import com.tangem.features.yield.supply.impl.incentives.claim.ui.YieldSupplyClaimContent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import kotlinx.coroutines.flow.StateFlow

internal class YieldSupplyClaimComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyClaimModel = getOrCreateModel(params = params)

    private val notificationsComponent = YieldSupplyNotificationsComponent(
        appComponentContext = child("yieldSupplyClaimNotifications"),
        params = YieldSupplyNotificationsComponent.Params(
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
            feeCryptoCurrencyStatusFlow = model.feeCryptoCurrencyStatusFlow,
            callback = model,
        ),
    )

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val config = remember {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = params.onDismiss,
                // Vetoes only the drag: scrim and back hide the sheet before asking, so refusing them leaves an invisible
                // dialog eating touches
                dismissOnClickOutside = { !model.uiState.value.isClaimInProgress },
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
            config = config,
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.yield_module_claim_sheet_title),
                    subtitle = resourceReference(R.string.yield_module_claim_sheet_subtitle),
                    endIconRes = R.drawable.ic_close_24,
                    onEndClick = model::onCloseClick,
                )
            },
            footer = {
                val text = stringResourceSafe(
                    id = R.string.yield_module_claim_button_title,
                    state.total.resolveReference(),
                )
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)

                if (state.isHoldToConfirmEnabled) {
                    HoldToConfirmButton(
                        text = text,
                        onConfirm = model::onClaimClick,
                        enabled = state.isClaimEnabled,
                        isLoading = state.isClaimInProgress,
                        modifier = modifier,
                    )
                } else {
                    PrimaryButtonIconEnd(
                        text = text,
                        onClick = model::onClaimClick,
                        iconResId = walletInterationIcon(params.userWallet),
                        enabled = state.isClaimEnabled,
                        showProgress = state.isClaimInProgress,
                        modifier = modifier,
                    )
                }
            },
            content = {
                YieldSupplyClaimContent(
                    state = state,
                    notificationsComponent = notificationsComponent,
                )
            },
        )
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val chainId: Int,
        val userAddress: String,
        val onDismiss: () -> Unit,
    )
}