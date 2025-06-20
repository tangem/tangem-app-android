package com.tangem.features.walletconnect.transaction.components.common

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

internal abstract class WcCommonTransactionComponentDelegate(
    private val appComponentContext: AppComponentContext,
    private val feeSelectorBlockComponentFactory: FeeSelectorBlockComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private var stackNavigation: StackNavigation<WcTransactionRoutes>? = null
    private var contentStack: Value<ChildStack<WcTransactionRoutes, ComposableBottomSheetComponent>>? = null

    protected val innerRouter by lazy {
        stackNavigation?.let {
            InnerRouter<WcTransactionRoutes>(stackNavigation = it, popCallback = { onChildBack() })
        }
    }
    protected val feeSelectorBlockComponent = feeSelectorBlockComponentFactory.create(
        context = appComponentContext,
        params = Unit,
    )

    protected fun init(
        navigation: StackNavigation<WcTransactionRoutes>,
        screenChild: (WcTransactionRoutes, ComponentContext) -> ComposableBottomSheetComponent,
        screenKey: String,
    ) {
        stackNavigation = navigation
        contentStack = childStack(
            key = screenKey,
            source = navigation,
            serializer = WcTransactionRoutes.serializer(),
            initialConfiguration = WcTransactionRoutes.Transaction,
            handleBackButton = true,
            childFactory = screenChild,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        if (contentStack != null) {
            val content by contentStack!!.subscribeAsState()
            BackHandler(onBack = ::onChildBack)
            content.active.instance.BottomSheet()
        }
    }

    private fun onChildBack() {
        when (contentStack?.value?.active?.configuration) {
            is WcTransactionRoutes.Transaction,
            is WcTransactionRoutes.TransactionRequestInfo,
            is WcTransactionRoutes.Alert,
            is WcTransactionRoutes.CustomAllowance,
            -> stackNavigation?.pop()
            else -> Unit
        }
    }
}