package com.tangem.feature.wallet.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.asRouter
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wallet fragment
 *
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
internal class WalletFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    /** Feature router */
    @Inject
    internal lateinit var walletRouter: WalletRouter

    @Inject
    internal lateinit var coroutineDispatcherProvider: CoroutineDispatcherProvider

    @Inject
    internal lateinit var componentBuilder: DecomposeComponent.Builder

    @Inject
    internal lateinit var appRouter: AppRouter

    @Inject
    @GlobalUiMessageSender
    internal lateinit var messageSender: UiMessageSender

    private val _walletRouter: InnerWalletRouter
        get() = requireNotNull(walletRouter as? InnerWalletRouter) {
            "_walletRouter should be instance of InnerWalletRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _walletRouter.initializeResources(
            appComponentContext = DefaultAppComponentContext(
                componentContext = defaultComponentContext(requireActivity().onBackPressedDispatcher),
                messageSender = messageSender,
                dispatchers = coroutineDispatcherProvider,
                hiltComponentBuilder = componentBuilder,
                replaceRouter = appRouter.asRouter(),
            ),
        )
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        _walletRouter.Initialize(
            onFinish = remember(requireActivity()) {
                {
                    requireActivity().finish()
                }
            },
        )
    }

    companion object {

        /** Create wallet fragment instance */
        fun create(): WalletFragment = WalletFragment()
    }
}