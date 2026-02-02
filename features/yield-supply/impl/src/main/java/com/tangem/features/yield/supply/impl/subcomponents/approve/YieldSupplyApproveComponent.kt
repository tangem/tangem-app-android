package com.tangem.features.yield.supply.impl.subcomponents.approve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.HoldToConfirmButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.R as CoreR
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyActionContent
import com.tangem.features.yield.supply.impl.subcomponents.approve.model.YieldSupplyApproveModel
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import kotlinx.coroutines.flow.StateFlow

@Suppress("MagicNumber")
internal class YieldSupplyApproveComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: YieldSupplyApproveModel = getOrCreateModel(params = params)

    private val yieldSupplyNotificationsComponent = YieldSupplyNotificationsComponent(
        appComponentContext = child("yieldSupplyApproveNotifications"),
        params = YieldSupplyNotificationsComponent.Params(
            userWalletId = params.userWallet.walletId,
            cryptoCurrencyStatusFlow = params.cryptoCurrencyStatusFlow,
            feeCryptoCurrencyStatusFlow = model.feeCryptoCurrencyStatusFlow,
            callback = model,
        ),
    )

    override fun dismiss() {
        params.callback.onDismissClick()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()

        TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = params.callback::onDismissClick,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    endIconRes = R.drawable.ic_close_24,
                    onEndClick = params.callback::onDismissClick,
                )
            },
            footer = {
                val modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)

                if (state.isHoldToConfirmEnabled) {
                    HoldToConfirmButton(
                        text = stringResourceSafe(
                            CoreR.string.common_hold_to,
                            stringResourceSafe(R.string.common_confirm),
                        ),
                        onConfirm = model::onClick,
                        enabled = state.isPrimaryButtonEnabled,
                        isLoading = state.isTransactionSending,
                        modifier = modifier,
                    )
                } else {
                    PrimaryButtonIconEnd(
                        text = stringResourceSafe(R.string.common_confirm),
                        onClick = model::onClick,
                        iconResId = walletInterationIcon(params.userWallet),
                        enabled = state.isPrimaryButtonEnabled,
                        modifier = modifier,
                    )
                }
            },
            content = {
                YieldSupplyActionContent(
                    yieldSupplyActionUM = state,
                    onFooterClick = model::onReadMoreClick,
                    yieldSupplyNotificationsComponent = yieldSupplyNotificationsComponent,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(TangemTheme.colors.icon.accent.copy(0.1f), CircleShape),
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_check_circle_24),
                            contentDescription = null,
                            tint = TangemTheme.colors.icon.accent,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(32.dp),
                        )
                    }
                }
            },
        )
    }

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onDismissClick()
        fun onTransactionProgress(inProgress: Boolean)
        fun onTransactionSent()
    }
}