package com.tangem.feature.usedesk

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
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
import com.tangem.usedesk.chat_gui.chat.UsedeskChatScreen

internal class DefaultUsedeskComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: UsedeskComponent.Params,
) : UsedeskComponent, AppComponentContext by appComponentContext {

    private val model: UsedeskModel = getOrCreateModel(params)

    @Suppress("NestedScopeFunctions")
    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val configuration = state.usedeskChatConfiguration

        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            // Show the chat only once the configuration is ready (clientId is loaded asynchronously).
            if (configuration != null) {
                val activity = LocalContext.current as? FragmentActivity ?: return@Box
                val containerId = rememberSaveable { View.generateViewId() }

                AndroidView(
                    factory = { context ->
                        FragmentContainerView(context).apply {
                            id = containerId

                            val fragment = UsedeskChatScreen.newInstance(
                                usedeskChatConfiguration = configuration,
                                allowedFileExtensions = ALLOWED_FILE_EXTENSIONS,
                                cameraEnabled = false,
                            ).apply {
                                onChatLoaded = { model.onChatLoaded() }
                                onChatLoadError = { model.onChatLoadError() }
                                onAttachLogs = { onReady ->
                                    model.provideLogsFile { file ->
                                        val uri = file?.let { logsFile ->
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                logsFile,
                                            )
                                        }
                                        onReady(uri)
                                    }
                                }
                            }
                            activity.supportFragmentManager.beginTransaction()
                                .replace(containerId, fragment)
                                .commit()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                DisposableEffect(Unit) {
                    onDispose {
                        // When leaving the chat, hide the keyboard via the (still alive) activity
                        // window and remove the hosted fragment. Otherwise the input connection to
                        // the destroyed EditText leaks: the keyboard stays open and crashes the app
                        // when it is dismissed later.
                        activity.getSystemService(InputMethodManager::class.java)
                            ?.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)

                        val fragmentManager = activity.supportFragmentManager
                        if (!fragmentManager.isStateSaved) {
                            fragmentManager.executePendingTransactions()
                            fragmentManager.findFragmentById(containerId)?.let { fragment ->
                                fragmentManager.beginTransaction()
                                    .remove(fragment)
                                    .commit()
                            }
                        }
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : UsedeskComponent.Factory {
        override fun create(context: AppComponentContext, params: UsedeskComponent.Params): DefaultUsedeskComponent
    }

    private companion object {
        // Allowed attachment types: zip, images (jpg/jpeg/png/gif/webp/heic),
        // videos (mp4/mov/webm). jpeg is added as a synonym for jpg.
        val ALLOWED_FILE_EXTENSIONS = listOf(
            "zip",
            "jpg", "jpeg", "png", "gif", "webp", "heic",
            "mp4", "mov", "webm",
        )
    }
}