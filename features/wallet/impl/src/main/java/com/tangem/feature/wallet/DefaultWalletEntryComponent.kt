package com.tangem.feature.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.feature.wallet.child.organizetokens.OrganizeTokensComponent
import com.tangem.feature.wallet.child.wallet.WalletComponent
import com.tangem.feature.wallet.navigation.WalletRoute
import com.tangem.features.wallet.WalletEntryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletEntryComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    walletComponentFactory: WalletComponent.Factory,
) : WalletEntryComponent, AppComponentContext by appComponentContext {

    private val navigation = StackNavigation<WalletRoute>()

    private val stack = childStack<WalletRoute, ComposableContentComponent>(
        source = navigation,
        serializer = WalletRoute.serializer(),
        initialConfiguration = WalletRoute.Wallet,
        childFactory = { route, context ->
            when (route) {
                WalletRoute.Wallet -> walletComponentFactory.create(
                    appComponentContext = childByContext(context),
                    navigate = { navigation.pushNew(it) },
                )
                is WalletRoute.OrganizeTokens -> OrganizeTokensComponent(
                    appComponentContext = childByContext(context),
                    params = OrganizeTokensComponent.Params(route.userWalletId),
                    onBack = { navigation.pop() },
                )
            }
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Children(
            stack = stack,
            animation = stackAnimation(fade()),
        ) {
            it.instance.Content(modifier)
        }
    }

    @AssistedFactory
    interface Factory : WalletEntryComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWalletEntryComponent
    }
}