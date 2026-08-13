package com.tangem.features.jointaccount.join.component

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
import com.tangem.common.R
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.jointaccount.common.displayname.JointAccountDisplayNameComponent
import com.tangem.features.jointaccount.join.invitepreview.JointAccountInvitePreviewComponent
import com.tangem.features.jointaccount.join.model.JointAccountJoinChildParams
import com.tangem.features.jointaccount.join.model.JointAccountJoinModel
import com.tangem.features.jointaccount.join.navigation.JointAccountJoinRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountJoinComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointAccountJoinComponent.Params,
    private val displayNameComponentFactory: JointAccountDisplayNameComponent.Factory,
) : JointAccountJoinComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountJoinModel = getOrCreateModel()

    private val childParams = JointAccountJoinChildParams(
        inviteId = params.inviteId,
        draftHolder = model.draftHolder,
    )

    private val stackNavigation = StackNavigation<JointAccountJoinRoute>()

    private val innerRouter = InnerRouter<JointAccountJoinRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack = childStack(
        key = "jointAccountJoinStack",
        source = stackNavigation,
        serializer = null,
        initialConfiguration = JointAccountJoinRoute.InvitePreview,
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
        route: JointAccountJoinRoute,
        childContext: AppComponentContext,
    ): ComposableContentComponent = when (route) {
        is JointAccountJoinRoute.InvitePreview -> createInvitePreviewComponent(childContext = childContext)
        is JointAccountJoinRoute.DisplayName -> createDisplayNameComponent(childContext = childContext)
    }

    private fun createInvitePreviewComponent(childContext: AppComponentContext): JointAccountInvitePreviewComponent {
        return JointAccountInvitePreviewComponent(
            appComponentContext = childContext,
            params = childParams,
        )
    }

    private fun createDisplayNameComponent(childContext: AppComponentContext): JointAccountDisplayNameComponent {
        val draft = model.draftHolder.draft.value

        return displayNameComponentFactory.create(
            context = childContext,
            params = JointAccountDisplayNameComponent.Params(
                userWalletId = requireNotNull(draft.selectedWalletId) {
                    "The invite preview step must select a wallet before the display name step is entered"
                },
                buttonText = resourceReference(R.string.joint_account_join_to_account_btn_text),
                initialName = draft.displayName,
                onContinueClick = ::onDisplayNameContinue,
                onCloseClick = { router.pop() },
            ),
        )
    }

    private fun onDisplayNameContinue(name: String) {
        model.draftHolder.setDisplayName(name)
        // TODO: start the join signing (NFC) session — a separate task once the domain integration lands
    }

    private fun onChildBack() {
        if (stack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : JointAccountJoinComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountJoinComponent.Params,
        ): DefaultJointAccountJoinComponent
    }
}