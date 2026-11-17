package com.tangem.features.earn.impl.widgets.calculator

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.earn.impl.widgets.calculator.ui.LocalEarnCalculatorOnGlass
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class EarnCalculatorSheetComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    override fun dismiss() {
        params.callback.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        TangemModal<EarnCalculatorSheetConfig>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = EarnCalculatorSheetConfig,
            ),
            onBack = ::dismiss,
            title = {
                TangemModal.TopNavigation(
                    title = resourceReference(R.string.earn_calculator_title),
                    onClose = ::dismiss,
                )
            },
            content = {
                CompositionLocalProvider(LocalEarnCalculatorOnGlass provides true) {
                    params.calculator.Content(modifier = Modifier.hazeSourceTangem().padding(bottom = 16.dp))
                }
            },
        )
    }

    data class Params(
        val calculator: ComposableContentComponent,
        val callback: ModelCallback,
    )

    interface ModelCallback {
        fun onDismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnCalculatorSheetComponent
    }

    companion object {
        const val KEY = "calculatorSheet"
    }
}

internal data object EarnCalculatorSheetConfig : TangemBottomSheetConfigContent