package com.tangem.features.onboarding.v2.addresssync

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncIntent
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncModel
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncState
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.onboarding.v2.addresssync.ui.AddressSyncButtonScreen
import com.tangem.features.onboarding.v2.addresssync.ui.AddressSyncContent
import com.tangem.features.onboarding.v2.addresssync.ui.loading.AddressSyncLoading
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.PushNotificationsParams

@OptIn(DelicateDecomposeApi::class)
@Suppress("LongParameterList")
internal class DefaultAddressSyncComponent(
    appComponentContext: AppComponentContext,
    params: MultiWalletChildParams,
    private val onBack: () -> Unit,
    private val addressSyncParams: AddressSyncComponent.Params,
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val pushNotificationsComponentFactory: PushNotificationsComponent.Factory,
) : AppComponentContext by appComponentContext, AddressSyncComponent {

    private val model: AddressSyncModel = getOrCreateModel(params)

    private val childStack: Value<ChildStack<AddressSyncStep, ComposableContentComponent>> =
        childStack(
            key = "innerStack",
            source = model.stackNavigation,
            serializer = null,
            initialConfiguration = AddressSyncStep.LOADING,
            handleBackButton = true,
            childFactory = { configuration, factoryContext ->
                createChild(
                    step = configuration,
                    childContext = childByContext(factoryContext),
                )
            },
        )

    init {
        model.initConfiguration()
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(enabled = true) {
            onBack()
        }

        val state by model.state.collectAsStateWithLifecycle()

        LaunchedEffect(state) {
            when (state) {
                AddressSyncState.Exit -> handleExit()
                is AddressSyncState.Success -> {
                    val shouldExit = (state as AddressSyncState.Success).shouldExit
                    if (shouldExit) handleExit()
                }
                AddressSyncState.Loading -> Unit
            }
        }

        AddressSyncContent(
            modifier = modifier,
            childContent = {
                Children(
                    stack = childStack,
                ) { child ->
                    child.instance.Content(Modifier)
                }
            },
        )
    }

    private fun handleExit() {
        if (addressSyncParams.isWalletStarted) {
            router.popTo(AppRoute.Wallet)
        } else {
            router.replaceAll(AppRoute.Wallet)
        }
    }

    private fun createChild(step: AddressSyncStep, childContext: AppComponentContext): ComposableContentComponent {
        return when (step) {
            AddressSyncStep.LOADING -> ComposableContentComponent { AddressSyncLoading() }
            AddressSyncStep.ASK_BIOMETRY -> createAskBiometryComponent(childContext)
            AddressSyncStep.ASK_NOTIFICATIONS -> createPushNotificationComponent(childContext)
            AddressSyncStep.ADDRESS_SYNC -> ComposableContentComponent {
                val state by model.state.collectAsStateWithLifecycle()
                when (state) {
                    AddressSyncState.Loading -> AddressSyncLoading()
                    is AddressSyncState.Success -> AddressSyncButtonScreen(
                        state = state as AddressSyncState.Success,
                        modifier = Modifier.fillMaxSize(),
                        onSyncClick = {
                            model.onIntent(AddressSyncIntent.Sync)
                        },
                    )
                    AddressSyncState.Exit -> AddressSyncLoading()
                }
            }
        }
    }

    private fun createAskBiometryComponent(childContext: AppComponentContext): AskBiometryComponent {
        return askBiometryComponentFactory.create(
            context = childContext,
            params = AskBiometryComponent.Params(
                isBottomSheetVariant = false,
                modelCallbacks = object : AskBiometryComponent.ModelCallbacks {
                    override fun onAllowed() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ASK_NOTIFICATIONS,
                            ),
                        )
                    }

                    override fun onDenied() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ASK_NOTIFICATIONS,
                            ),
                        )
                    }
                },
            ),
        )
    }

    private fun createPushNotificationComponent(childContext: AppComponentContext): PushNotificationsComponent {
        return pushNotificationsComponentFactory.create(
            context = childContext,
            params = PushNotificationsParams(
                modelCallbacks = object : PushNotificationsModelCallbacks {
                    override fun onAllowSystemPermission() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ADDRESS_SYNC,
                            ),
                        )
                    }

                    override fun onDenySystemPermission() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ADDRESS_SYNC,
                            ),
                        )
                    }

                    override fun onDismiss() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ADDRESS_SYNC,
                            ),
                        )
                    }
                },
                source = AppRoute.PushNotification.Source.Onboarding,
            ),
        )
    }
}