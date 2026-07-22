package com.tangem.feature.wallet.child.tokenActions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.feature.wallet.child.tokenActions.TokenActionsComponent.Params
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
        // No-op: token actions are rendered via Content() in the redesigned UI
    }

    @Composable
    override fun Content(modifier: Modifier) {
        if (params.tokenRowUM == null) {
            dismiss()
        } else {
            val isBalanceHidden by isBalanceHiddenFlow.collectAsStateWithLifecycle()
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

    @AssistedFactory
    interface Factory : TokenActionsComponent.Factory {
        override fun create(context: AppComponentContext, params: Params): DefaultTokenActionsComponent
    }
}