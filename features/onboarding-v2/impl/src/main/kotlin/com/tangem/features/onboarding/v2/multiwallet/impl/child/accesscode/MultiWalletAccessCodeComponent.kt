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
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model.MultiWalletAccessCodeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.ui.MultiWalletAccessCodeBS
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
class MultiWalletAccessCodeComponent(
    context: AppComponentContext,
    private val params: MultiWalletChildParams,
    private val onDismiss: () -> Unit,
) : AppComponentContext by context, ComposableBottomSheetComponent {

    private val model = getOrCreateModel<MultiWalletAccessCodeModel>()

    init {
        componentScope.launch {
            model.onDismiss.collect {
                onDismiss()
            }
        }
    }

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bsConfig = remember(this) {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }

        MultiWalletAccessCodeBS(
            config = bsConfig,
            state = state,
            onBack = { model.onBack() },
        )
    }

    @JvmInline
    value class AccessCode(val value: String)
}