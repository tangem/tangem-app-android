package com.tangem.features.swap.v2.impl.sendviaswap.success

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.navigationButtons.DoneButtons
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.features.swap.v2.impl.sendviaswap.success.model.SendWithSwapSuccessModel
import com.tangem.features.swap.v2.impl.sendviaswap.success.ui.SendWithSwapSuccessContent
import kotlinx.coroutines.flow.StateFlow

internal class SendWithSwapSuccessComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: SendWithSwapSuccessModel = getOrCreateModel(params = params)

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            onBackClick = router::pop,
            backIconRes = R.drawable.ic_close_24,
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        SendWithSwapSuccessContent(sendWithSwapUM = state)
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        ) {
            DoneButtons(
                (NavigationButton(
                    textReference = resourceReference(R.string.common_explore),
                    iconRes = R.drawable.ic_web_24,
                    onClick = model::onExploreClick,
                ) to NavigationButton(
                    textReference = resourceReference(R.string.common_share),
                    iconRes = R.drawable.ic_share_24,
                    onClick = model::onShareClick,
                )).takeUnless { (state.confirmUM as? ConfirmUM.Success)?.txUrl.isNullOrBlank() },
            )
            PrimaryButton(
                text = stringResourceSafe(R.string.common_close),
                onClick = router::pop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    data class Params(
        val sendWithSwapUMFlow: StateFlow<SendWithSwapUM>,
        val analyticsCategoryName: String,
        val callback: NavigationModelCallback,
    )
}