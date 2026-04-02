package com.tangem.feature.wallet.child.tokenActions

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.getDefaultRowColors
import com.tangem.core.ui.components.getWarningRowColors
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.feature.wallet.child.tokenActions.TokenActionsComponent.Params
import com.tangem.feature.wallet.presentation.wallet.ui.components.fastForEach
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

internal class DefaultTokenActionsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
    val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
) : TokenActionsComponent, AppComponentContext by appComponentContext {

    val isBalanceHiddenFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                isBalanceHiddenFlow.value = it.isBalanceHidden
            }
            .launchIn(componentScope)
    }

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        if (!LocalRedesignEnabled.current) {
            TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
                containerColor = TangemTheme.colors.background.primary,
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::dismiss,
                    content = TangemBottomSheetConfigContent.Empty,
                ),
            ) {
                Column {
                    params.actions.fastForEach { action ->
                        if (action.isEnabled) {
                            val rowColors = if (action.isWarning) {
                                getWarningRowColors()
                            } else {
                                getDefaultRowColors()
                            }
                            SimpleSettingsRow(
                                title = action.text.resolveReference(),
                                icon = action.iconResId,
                                enabled = action.isEnabled,
                                rowColors = rowColors,
                                onItemsClick = action.onClick,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        if (params.tokenRowUM == null) {
            dismiss()
        } else {
            val isBalanceHidden by isBalanceHiddenFlow.collectAsStateWithLifecycle()
            if (LocalRedesignEnabled.current) {
                val offset = with(LocalDensity.current) {
                    DpOffset(params.offsetX.toDp(), params.offsetY.toDp())
                }
                TokenActionContent(
                    tokenRowUM = params.tokenRowUM,
                    isBalanceHidden = isBalanceHidden,
                    offset = offset,
                    actions = params.actions,
                    onDismiss = params.onDismiss,
                    modifier = modifier,
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : TokenActionsComponent.Factory {
        override fun create(context: AppComponentContext, params: Params): DefaultTokenActionsComponent
    }
}