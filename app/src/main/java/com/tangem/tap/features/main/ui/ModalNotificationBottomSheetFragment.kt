package com.tangem.tap.features.main.ui

import android.content.DialogInterface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeBottomSheetFragment
import com.tangem.tap.features.main.MainViewModel
import com.tangem.tap.features.main.model.ModalNotification
import com.tangem.tap.features.main.ui.components.ModalNotificationContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ModalNotificationBottomSheetFragment : ComposeBottomSheetFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: MainViewModel by activityViewModels()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val notification = state.modalNotification
        if (notification == null) {
            dismiss()
            return
        }

        BackHandler(onBack = notification.onDismissRequest)

        when (val content = notification.content) {
            is ModalNotification -> ModalNotificationContent(
                modifier = modifier
                    .background(
                        color = TangemTheme.colors.background.primary,
                        shape = TangemTheme.shapes.bottomSheet,
                    ),
                notification = content,
            )
            else -> Unit
        }

        LaunchedEffect(notification) {
            if (!notification.isShown) {
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.state.value.modalNotification?.onDismissRequest?.invoke()
        super.onDismiss(dialog)
    }
}