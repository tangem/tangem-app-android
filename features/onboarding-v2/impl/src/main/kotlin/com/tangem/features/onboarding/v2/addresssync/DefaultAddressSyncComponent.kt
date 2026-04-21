package com.tangem.features.onboarding.v2.addresssync

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.tangem.features.onboarding.v2.addresssync.navigation.AddressSyncStep
import com.tangem.features.onboarding.v2.addresssync.ui.AddressSyncContent
import com.tangem.features.pushnotifications.api.PushNotificationsComponent
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.PushNotificationsParams

@OptIn(DelicateDecomposeApi::class)
internal class DefaultAddressSyncComponent(
    appComponentContext: AppComponentContext,
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val pushNotificationsComponentFactory: PushNotificationsComponent.Factory,
) : AppComponentContext by appComponentContext, AddressSyncComponent {

    private val model: AddressSyncModel = getOrCreateModel()

    private val childStack: Value<ChildStack<AddressSyncStep, ComposableContentComponent>> =
        childStack(
            key = "innerStack",
            source = model.stackNavigation,
            serializer = null,
            initialConfiguration = AddressSyncStep.ASK_BIOMETRY,
            handleBackButton = true,
            childFactory = { configuration, factoryContext ->
                createChild(
                    step = configuration,
                    childContext = childByContext(factoryContext),
                )
            },
        )

    @Composable
    override fun Content(modifier: Modifier) {
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

        BackHandler {
            model.onIntent(AddressSyncIntent.Back)
        }
    }

    private fun createChild(step: AddressSyncStep, childContext: AppComponentContext): ComposableContentComponent {
        return when (step) {
            AddressSyncStep.ASK_BIOMETRY -> createAskBiometryComponent(childContext)
            AddressSyncStep.ASK_NOTIFICATIONS -> createPushNotificationComponent(childContext)
            AddressSyncStep.ADDRESS_SYNC -> TODO("Will be implemented during [REDACTED_TASK_KEY]")
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
                                shouldReplace = true,
                            ),
                        )
                    }

                    override fun onDenied() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ASK_NOTIFICATIONS,
                                shouldReplace = false,
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
                                shouldReplace = true,
                            ),
                        )
                    }

                    override fun onDenySystemPermission() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ADDRESS_SYNC,
                                shouldReplace = false,
                            ),
                        )
                    }

                    override fun onDismiss() {
                        model.onIntent(
                            AddressSyncIntent.Next(
                                step = AddressSyncStep.ADDRESS_SYNC,
                                shouldReplace = false,
                            ),
                        )
                    }
                },
                source = AppRoute.PushNotification.Source.Onboarding,
            ),
        )
    }
}