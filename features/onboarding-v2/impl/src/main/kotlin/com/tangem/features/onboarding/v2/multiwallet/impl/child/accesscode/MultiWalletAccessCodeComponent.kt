package com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model.MultiWalletAccessCodeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.MultiWalletAccessCodeBS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MultiWalletAccessCodeComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    private val onDismiss: () -> Unit,
) : AppComponentContext by context, ComposableBottomSheetComponent {

    private val model: MultiWalletAccessCodeModel = getOrCreateModel(params)
    private val showBs = MutableStateFlow(true)

    init {
        componentScope.launch {
            model.onDismiss.collect {
                showBs.value = false
                delay(timeMillis = 500)
                onDismiss()
            }
        }
    }

    override fun dismiss() {
        componentScope.launch {
            showBs.value = false
            delay(timeMillis = 500)
            onDismiss()
        }
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val showBs by showBs.collectAsStateWithLifecycle()

        val bsConfig = remember(this, showBs) {
            TangemBottomSheetConfig(
                isShown = showBs,
                onDismissRequest = { dismiss() },
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        MultiWalletAccessCodeBS(
            config = bsConfig,
            state = state,
            onBack = { model.onBack() },
        )

        DisableScreenshotsDisposableEffect()
    }

    @JvmInline
    value class AccessCode(val value: String)
}