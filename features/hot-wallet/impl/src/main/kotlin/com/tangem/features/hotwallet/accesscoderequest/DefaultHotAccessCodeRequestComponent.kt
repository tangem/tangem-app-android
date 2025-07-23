package com.tangem.features.hotwallet.accesscoderequest

import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.FullScreen
import com.tangem.features.hotwallet.HotAccessCodeRequestComponent
import com.tangem.features.hotwallet.HotWalletPasswordRequester
import com.tangem.features.hotwallet.accesscoderequest.ui.HotAccessCodeRequestFullScreenContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

internal class DefaultHotAccessCodeRequestComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AppComponentContext by appComponentContext, HotAccessCodeRequestComponent {

    private val model: HotAccessCodeRequestModel = getOrCreateModel(params)

    override suspend fun wrongPassword() {
        model.wrongAccessCode()
    }

    override suspend fun requestPassword(hasBiometry: Boolean): HotWalletPasswordRequester.Result {
        model.show(hasBiometry)
        return model.waitResult()
    }

    override suspend fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        var isShownProxy by remember { mutableStateOf(state.isShown) }
        var isShownIfProxy by remember { mutableStateOf(state.isShown) }

        if (isShownIfProxy) {
            FullScreen(focusable = true, onBackClick = state.onDismiss) {
                HotAccessCodeRequestFullScreenContent(
                    state = state.copy(isShown = isShownProxy),
                    modifier = modifier,
                )
            }
        }

        LaunchedEffect(state.isShown) {
            if (state.isShown) {
                isShownIfProxy = true
                delay(timeMillis = 100)
                isShownProxy = true
            } else {
                isShownProxy = false
                delay(timeMillis = 250)
                isShownIfProxy = false
            }
        }
    }

    @AssistedFactory
    interface Factory : HotAccessCodeRequestComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultHotAccessCodeRequestComponent
    }
}