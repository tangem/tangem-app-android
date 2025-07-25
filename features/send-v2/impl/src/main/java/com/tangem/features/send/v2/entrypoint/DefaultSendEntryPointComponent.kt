package com.tangem.features.send.v2.entrypoint

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.SendEntryPointComponent
import com.tangem.features.send.v2.entrypoint.model.SendEntryPoint
import com.tangem.features.send.v2.entrypoint.model.SendEntryPointModel
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSendEntryPointComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SendEntryPointComponent.Params,
    sendWithSwapComponentFactory: SendWithSwapComponent.Factory,
    sendComponentFactory: SendComponent.Factory,
) : SendEntryPointComponent, AppComponentContext by appComponentContext {

    private val model: SendEntryPointModel = getOrCreateModel(params = params)

    private val sendWithSwapComponent = sendWithSwapComponentFactory.create(
        context = child("sendEntrySendWithSwap"),
        params = SendWithSwapComponent.Params(
            userWalletId = params.userWalletId,
            currency = params.cryptoCurrency,
            callback = model,
        ),
    )

    private val sendComponent = sendComponentFactory.create(
        context = child("sendEntryVanillaSend"),
        params = SendComponent.Params(
            userWalletId = params.userWalletId,
            currency = params.cryptoCurrency,
            callback = model,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val sendEntryState by model.sendEntryPointState.collectAsStateWithLifecycle()

        sendComponent.Content(modifier)

        AnimatedVisibility(
            visible = sendEntryState == SendEntryPoint.SendWithSwap,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            sendWithSwapComponent.Content(modifier)
        }
    }

    @AssistedFactory
    interface Factory : SendEntryPointComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: SendEntryPointComponent.Params,
        ): DefaultSendEntryPointComponent
    }
}