package com.tangem.features.pushnotifications.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.features.pushnotifications.impl.presentation.ui.PushNotificationsScreen
import com.tangem.features.pushnotifications.impl.presentation.viewmodel.PushNotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class PushNotificationsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel by viewModels<PushNotificationViewModel>()

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        BackHandler(onBack = requireActivity()::finish)
        NavigationBar3ButtonsScrim()
        PushNotificationsScreen(
            onRequest = viewModel::onRequest,
            onNeverRequest = viewModel::onNeverRequest,
            onAllowPermission = viewModel::onAllowPermission,
            onDenyPermission = viewModel::onDenyPermission,
        )
    }

    companion object {
        /** Create push notifications fragment instance */
        fun create(): PushNotificationsFragment = PushNotificationsFragment()
    }
}