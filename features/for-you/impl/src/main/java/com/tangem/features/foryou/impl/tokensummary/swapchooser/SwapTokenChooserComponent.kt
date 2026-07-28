package com.tangem.features.foryou.impl.tokensummary.swapchooser

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEmbeddedContent
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.tokensummary.model.SwapHolding
import com.tangem.features.foryou.impl.tokensummary.swapchooser.model.SwapTokenChooserModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

internal class SwapTokenChooserComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by context {

    private val model: SwapTokenChooserModel = getOrCreateModel(params)

    override fun dismiss() = model.onDismiss()

    @Composable
    override fun BottomSheet() {
        val content by model.content.collectAsStateWithLifecycle()
        val contentNonNull = content ?: return

        TangemBottomSheet<TokenSelectorContentUM>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = contentNonNull,
            ),
            type = TangemBottomSheetType.Modal,
            content = { model ->
                TokenSelectorEmbeddedContent(
                    content = model,
                    scrollBottomInset = LocalTangemBottomSheetContentBottomInset.current,
                )
            },
            title = {
                TangemTopBar(
                    title = resourceReference(R.string.common_swap),
                    subtitle = resourceReference(R.string.common_choose_token),
                    type = TangemTopBarType.BottomSheet,
                    endContent = {
                        TangemButton(
                            modifier = Modifier.testTag(BaseBottomSheetTestTags.CLOSE_BUTTON),
                            iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                            onClick = { model.onDismiss() },
                            size = TangemButton.Size.X11,
                            variant = TangemButton.Variant.Secondary,
                        )
                    },
                )
            },
            footer = {
                SecondaryButton(
                    text = stringResourceSafe(R.string.common_cancel),
                    onClick = { dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            },
        )
    }

    data class Params(
        val holdings: StateFlow<List<SwapHolding>>,
        val callbacks: ModelCallbacks,
    )

    interface ModelCallbacks {
        fun onHoldingSelected(holding: SwapHolding)
        fun onDismiss()
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): SwapTokenChooserComponent
    }
}