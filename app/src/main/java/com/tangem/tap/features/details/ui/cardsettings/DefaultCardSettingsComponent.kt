package com.tangem.tap.features.details.ui.cardsettings

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.androidPredictiveBackAnimatableV2
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.decompose.ui.stack.ChildStack
import com.tangem.core.decompose.ui.stack.animation.PredictiveBackParams
import com.tangem.core.decompose.ui.stack.animation.slideAndFade
import com.tangem.core.decompose.ui.stack.animation.stackAnimation
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.coderecovery.api.AccessCodeRecoveryComponent
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.tap.features.details.ui.securitymode.api.SecurityModeComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCardSettingsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: CardSettingsComponent.Params,
    private val mainComponentFactory: CardSettingsMainComponent.Factory,
    private val securityModeComponentFactory: SecurityModeComponent.Factory,
    private val resetCardComponentFactory: ResetCardComponent.Factory,
    private val accessCodeRecoveryComponentFactory: AccessCodeRecoveryComponent.Factory,
) : CardSettingsComponent, AppComponentContext by appComponentContext {

    private val stackNavigation = StackNavigation<CardSettingsRoute>()

    private val innerRouter = InnerRouter<CardSettingsRoute>(
        stackNavigation = stackNavigation,
        popCallback = { onComplete -> onChildBack(onComplete) },
    )

    private val stack = childStack(
        key = "cardSettingsStack",
        source = stackNavigation,
        serializer = CardSettingsRoute.serializer(),
        initialConfiguration = CardSettingsRoute.Main,
        handleBackButton = true,
        childFactory = { route, factoryContext ->
            createChild(
                route = route,
                childContext = childByContext(componentContext = factoryContext, router = innerRouter),
            )
        },
    )

    @OptIn(ExperimentalDecomposeApi::class)
    @Composable
    override fun Content(modifier: Modifier) {
        ChildStack(
            stack = stack,
            modifier = modifier,
            animation = stackAnimation(
                animator = slideAndFade(),
                predictiveBackParams = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        PredictiveBackParams(
                            backHandler = backHandler,
                            onBack = { stackNavigation.pop() },
                            animatable = { backEvent -> androidPredictiveBackAnimatableV2(backEvent) },
                        )
                    } else {
                        null
                    }
                },
            ),
        ) { child ->
            child.instance.Content(Modifier.fillMaxSize())
        }
    }

    private fun createChild(route: CardSettingsRoute, childContext: AppComponentContext): ComposableContentComponent {
        return when (route) {
            CardSettingsRoute.Main -> mainComponentFactory.create(context = childContext, params = params)
            is CardSettingsRoute.SecurityMode -> securityModeComponentFactory.create(
                context = childContext,
                params = SecurityModeComponent.Params(userWalletId = route.userWalletId),
            )
            is CardSettingsRoute.ResetCard -> resetCardComponentFactory.create(
                context = childContext,
                params = ResetCardComponent.Params(
                    userWalletId = route.userWalletId,
                    cardId = route.cardId,
                    isActiveBackupStatus = route.isActiveBackupStatus,
                    backupCardsCount = route.backupCardsCount,
                    hasTangemPay = route.hasTangemPay,
                ),
            )
            CardSettingsRoute.AccessCodeRecovery -> accessCodeRecoveryComponentFactory.create(
                context = childContext,
                params = Unit,
            )
        }
    }

    private fun onChildBack(onComplete: (Boolean) -> Unit) {
        if (stack.value.backStack.isEmpty()) router.pop(onComplete) else stackNavigation.pop(onComplete)
    }

    @AssistedFactory
    interface Factory : CardSettingsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CardSettingsComponent.Params,
        ): DefaultCardSettingsComponent
    }
}