package com.tangem.features.jointaccount.creation.component

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
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.jointaccount.common.displayname.JointAccountDisplayNameComponent
import com.tangem.features.jointaccount.creation.composition.JointAccountCompositionComponent
import com.tangem.features.jointaccount.creation.config.JointAccountConfigComponent
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.model.JointAccountCreationModel
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.features.jointaccount.creation.impl.R
import com.tangem.features.jointaccount.creation.promo.JointAccountPromoComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountCreationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAccountCreationComponent.Params,
    private val displayNameComponentFactory: JointAccountDisplayNameComponent.Factory,
) : JointAccountCreationComponent, AppComponentContext by appComponentContext {

    /** Retained across configuration changes; owns the draft the steps accumulate */
    private val model: JointAccountCreationModel = getOrCreateModel()

    private val childParams = JointAccountCreationChildParams(
        userWalletId = params.userWalletId,
        draftHolder = model.draftHolder,
    )

    private val stackNavigation = StackNavigation<JointAccountCreationRoute>()

    private val innerRouter = InnerRouter<JointAccountCreationRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack = childStack(
        key = "jointAccountCreationStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = JointAccountCreationRoute.Promo,
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
        route: JointAccountCreationRoute,
        childContext: AppComponentContext,
    ): ComposableContentComponent = when (route) {
        is JointAccountCreationRoute.Promo -> createPromoComponent(childContext = childContext)
        is JointAccountCreationRoute.Config -> createConfigComponent(childContext = childContext)
        is JointAccountCreationRoute.Composition -> createCompositionComponent(childContext = childContext)
        is JointAccountCreationRoute.DisplayName -> createDisplayNameComponent(childContext = childContext)
    }

    private fun createPromoComponent(childContext: AppComponentContext): JointAccountPromoComponent {
        return JointAccountPromoComponent(
            appComponentContext = childContext,
            params = params,
        )
    }

    private fun createConfigComponent(childContext: AppComponentContext): JointAccountConfigComponent {
        return JointAccountConfigComponent(
            appComponentContext = childContext,
            params = childParams,
            onCloseClick = { router.pop() },
        )
    }

    private fun createCompositionComponent(childContext: AppComponentContext): JointAccountCompositionComponent {
        return JointAccountCompositionComponent(
            appComponentContext = childContext,
            params = childParams,
            onCloseClick = { router.pop() },
        )
    }

    private fun createDisplayNameComponent(childContext: AppComponentContext): JointAccountDisplayNameComponent {
        val draft = model.draftHolder.draft.value

        return displayNameComponentFactory.create(
            context = childContext,
            params = JointAccountDisplayNameComponent.Params(
                userWalletId = draft.config?.walletId ?: params.userWalletId,
                buttonText = resourceReference(R.string.common_create_account),
                initialName = draft.displayName,
                onContinueClick = ::onDisplayNameContinue,
                onCloseClick = { router.pop() },
            ),
        )
    }

    private fun onDisplayNameContinue(name: String) {
        model.draftHolder.setDisplayName(name)

        // TODO: start the card-signature (NFC) session — a separate task once the backend contract settles
    }

    private fun onChildBack() {
        if (stack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : JointAccountCreationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountCreationComponent.Params,
        ): DefaultJointAccountCreationComponent
    }
}