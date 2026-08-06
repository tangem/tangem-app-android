package com.tangem.features.tangempay.orderCard.impl

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.card.issue.TangemPayIssueAdditionalCardComponent
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardComponent
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardModel
import com.tangem.features.tangempay.orderCard.impl.model.TangemPayOrderCardNavigation
import com.tangem.features.tangempay.orderCard.impl.navigation.TangemPayOrderCardInnerRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayOrderCardComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: TangemPayOrderCardComponent.Params,
) : TangemPayOrderCardComponent, AppComponentContext by appComponentContext {

    private val model: TangemPayOrderCardModel = getOrCreateModel(params = params)

    private val stackNavigation = StackNavigation<TangemPayOrderCardInnerRoute>()

    private val innerRouter = InnerRouter<TangemPayOrderCardInnerRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val childStack = childStack(
        key = "tangemPayOrderCardInnerStack",
        source = stackNavigation,
        serializer = TangemPayOrderCardInnerRoute.serializer(),
        initialConfiguration = TangemPayOrderCardInnerRoute.Type,
        childFactory = ::screenChild,
    )

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TangemPayOrderCardNavigation.serializer(),
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by childStack.subscribeAsState()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        BackHandler(onBack = ::onChildBack)
        Children(modifier = modifier, stack = stack) { child ->
            child.instance.Content(modifier = Modifier.fillMaxSize())
        }
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun screenChild(
        config: TangemPayOrderCardInnerRoute,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config) {
        TangemPayOrderCardInnerRoute.Type -> TangemPayOrderCardTypeComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayOrderCardTypeComponent.Params(
                userWalletId = params.userWalletId,
                onSelectVirtual = model::onSelectVirtual,
                onSelectPlastic = { stackNavigation.pushNew(TangemPayOrderCardInnerRoute.Data) },
            ),
        )
        TangemPayOrderCardInnerRoute.Data -> TangemPayOrderCardDataComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayOrderCardDataComponent.Params(
                userWalletId = params.userWalletId,
                onOrderSubmitted = { stackNavigation.pushNew(TangemPayOrderCardInnerRoute.Success) },
                onClose = { router.pop() },
            ),
        )
        TangemPayOrderCardInnerRoute.Success -> TangemPayOrderCardSuccessComponent(
            appComponentContext = childByContext(componentContext = componentContext, router = innerRouter),
            params = TangemPayOrderCardSuccessComponent.Params(
                onDone = { router.pop() },
            ),
        )
    }

    private fun bottomSheetChild(
        navigation: TangemPayOrderCardNavigation,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (navigation) {
        is TangemPayOrderCardNavigation.IssueVirtual -> TangemPayIssueAdditionalCardComponent(
            appComponentContext = childByContext(componentContext),
            params = TangemPayIssueAdditionalCardComponent.Params(
                userWalletId = navigation.walletId,
                feeAmount = navigation.feeAmount,
                feeCurrency = navigation.feeCurrency,
                fiatBalance = navigation.fiatBalance,
                listener = model,
            ),
        )
    }

    private fun onChildBack() {
        if (childStack.value.backStack.isEmpty()) {
            router.pop()
        } else {
            stackNavigation.pop()
        }
    }

    @AssistedFactory
    interface Factory : TangemPayOrderCardComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayOrderCardComponent.Params,
        ): DefaultTangemPayOrderCardComponent
    }
}