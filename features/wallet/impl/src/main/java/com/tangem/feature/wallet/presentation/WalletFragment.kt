package com.tangem.feature.wallet.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.features.wallet.navigation.WalletRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Wallet fragment
 *
 * @author Andrew Khokhlov on 29/05/2023
 */
@AndroidEntryPoint
internal class WalletFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    /** Feature router */
    @Inject
    internal lateinit var walletRouter: WalletRouter

    override val backgroundColor: Color
        @Composable
        @ReadOnlyComposable
        get() = TangemTheme.colors.background.secondary

    private val _walletRouter: InnerWalletRouter
        get() = requireNotNull(walletRouter as? InnerWalletRouter) {
            "_walletRouter should be instance of InnerWalletRouter"
        }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        _walletRouter.Initialize(fragmentManager = requireActivity().supportFragmentManager)
    }

    companion object {

        /** Create wallet fragment instance */
        fun create(): WalletFragment = WalletFragment()
    }
}
