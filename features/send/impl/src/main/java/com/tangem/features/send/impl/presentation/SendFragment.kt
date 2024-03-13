package com.tangem.features.send.impl.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.ui.SendScreen
import com.tangem.features.send.impl.presentation.viewmodel.SendViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Send fragment
 */
@AndroidEntryPoint
internal class SendFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var router: SendRouter

    @Inject
    lateinit var listenToQrScanningUseCase: ListenToQrScanningUseCase

    @Inject
    lateinit var analyticsEventsHandler: AnalyticsEventHandler

    private val viewModel by viewModels<SendViewModel>()
    private val innerSendRouter: InnerSendRouter
        get() = requireNotNull(router as? InnerSendRouter) {
            "innerSendRouter should be instance of InnerSendRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)

        val isEditingDisabled = arguments?.getString(SendRouter.TRANSACTION_ID_KEY) != null
        viewModel.setRouter(
            innerSendRouter,
            StateRouter(
                fragmentManager = WeakReference(parentFragmentManager),
                isEditingDisabled = isEditingDisabled,
                analyticsEventsHandler = analyticsEventsHandler,
            ),
        )
        listenToQrCode()
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val systemBarsColor = TangemTheme.colors.background.tertiary
        SystemBarsEffect {
            setSystemBarsColor(systemBarsColor)
        }
        SendScreen(viewModel.uiState, viewModel.stateRouter.currentState)
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    private fun listenToQrCode() {
        lifecycleScope.launch {
            listenToQrScanningUseCase(SourceType.SEND)
                .getOrElse { emptyFlow() }
                .flowWithLifecycle(this@SendFragment.lifecycle, minActiveState = Lifecycle.State.CREATED)
                .collect {
                    delay(QR_SCAN_DELAY)

                    // Delayed launch is needed in order for the UI to be drawn and to process the sent events.
                    // If do not use the delay, then etAmount error field is not displayed when
                    // inserting an incorrect amount by shareUri
                    viewModel.onRecipientAddressScanned(it)
                }
        }
    }

    companion object {
        private const val QR_SCAN_DELAY = 200L

        /** Create send fragment instance */
        fun create(): SendFragment = SendFragment()
    }
}