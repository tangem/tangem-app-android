package com.tangem.features.jointaccount.main.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.confirmaddress.JointAccountConfirmAddressComponent
import com.tangem.features.jointaccount.main.navigation.JointAccountMembersRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountMembersComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAccountMembersComponent.Params,
    private val childFactory: JointAccountMembersChildFactory,
) : JointAccountMembersComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<JointAccountMembersRoute>()

    private val innerRouter = InnerRouter<JointAccountMembersRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack = childStack(
        key = "jointAccountMembersStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = JointAccountMembersRoute.Members,
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(
                route = route,
                childContext = childByContext(componentContext = factoryContext, router = innerRouter),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val childStack by stack.subscribeAsState()

        Children(
            stack = childStack,
            modifier = modifier,
            animation = stackAnimation { slide() },
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun createChild(
        route: JointAccountMembersRoute,
        childContext: AppComponentContext,
    ): ComposableContentComponent = when (route) {
        is JointAccountMembersRoute.Members -> JointAccountMembersListComponent(
            appComponentContext = childContext,
            params = params,
            childFactory = childFactory,
        )
        is JointAccountMembersRoute.ConfirmAddress -> JointAccountConfirmAddressComponent(
            appComponentContext = childContext,
            params = JointAccountConfirmAddressComponent.Params(
                userWalletId = params.userWalletId,
                cryptoAccountId = route.cryptoAccountId,
            ),
        )
    }

    private fun onChildBack() {
        if (stack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : JointAccountMembersComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountMembersComponent.Params,
        ): DefaultJointAccountMembersComponent
    }
}