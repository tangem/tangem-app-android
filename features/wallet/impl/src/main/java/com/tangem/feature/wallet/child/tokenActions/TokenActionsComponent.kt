package com.tangem.feature.wallet.child.tokenActions

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.getDefaultRowColors
import com.tangem.core.ui.components.getWarningRowColors
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.fastForEach
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class TokenActionsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
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

    data class Params(
        val actions: List<TokenActionButtonUM>,
        val onDismiss: () -> Unit,
    )
}