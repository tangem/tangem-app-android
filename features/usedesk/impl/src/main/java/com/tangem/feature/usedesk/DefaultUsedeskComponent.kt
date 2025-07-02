package com.tangem.feature.usedesk

import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.feature.usedesk.api.UsedeskComponent
import com.tangem.feature.usedesk.model.UsedeskModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import ru.usedesk.chat_gui.chat.UsedeskChatScreen

internal class DefaultUsedeskComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: UsedeskComponent.Params,
) : UsedeskComponent, AppComponentContext by appComponentContext {

    private val model: UsedeskModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = View.generateViewId()

                        val fragment = UsedeskChatScreen.newInstance(
                            state.usedeskChatConfiguration,
                        )
                        (context as FragmentActivity).supportFragmentManager.beginTransaction()
                            .replace(id, fragment)
                            .commit()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : UsedeskComponent.Factory {
        override fun create(context: AppComponentContext, params: UsedeskComponent.Params): DefaultUsedeskComponent
    }
}