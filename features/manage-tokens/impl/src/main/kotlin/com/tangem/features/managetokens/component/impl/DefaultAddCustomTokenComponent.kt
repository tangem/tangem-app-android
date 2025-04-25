package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.analytics.CustomTokenAnalyticsEvent
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.ui.AddCustomTokenBottomSheet
import com.tangem.features.managetokens.utils.ui.toContentModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddCustomTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddCustomTokenComponent.Params,
    private val selectorComponentFactory: CustomTokenSelectorComponent.Factory,
    private val formComponentFactory: CustomTokenFormComponent.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : AddCustomTokenComponent, AppComponentContext by context {

    private val initialConfiguration = AddCustomTokenConfig(
        userWalletId = params.userWalletId,
        step = AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
    )

    private val navigation = StackNavigation<AddCustomTokenConfig>()
    private val contentStack = childStack(
        key = "add_custom_token_content_stack",
        source = navigation,
        initialConfiguration = initialConfiguration,
        handleBackButton = true,
        serializer = AddCustomTokenConfig.serializer(),
        childFactory = ::contentChild,
    )

    init {
        analyticsEventHandler.send(CustomTokenAnalyticsEvent.ScreenOpened(params.source))
    }

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val config = remember {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = initialConfiguration.step.toContentModel(::popBack),
            )
        }
        val childStack by contentStack.subscribeAsState()

        AddCustomTokenBottomSheet(
            config = config.copy(
                content = childStack.active.configuration.step.toContentModel(::popBack),
            ),
            content = { modifier ->
                Children(
                    stack = childStack,
                    animation = stackAnimation(),
                ) { child ->
                    child.instance.Content(modifier = modifier)
                }
            },
        )
    }

    private fun popBack() {
        when (contentStack.value.active.configuration.step) {
            AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
            AddCustomTokenConfig.Step.FORM,
            -> dismiss()
            AddCustomTokenConfig.Step.NETWORK_SELECTOR,
            AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR,
            -> navigation.pop()
        }
    }

    private fun contentChild(
        config: AddCustomTokenConfig,
        componentContext: ComponentContext,
    ): ComposableContentComponent = when (config.step) {
        AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR -> {
            selectorComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenSelectorComponent.Params.NetworkSelector(
                    userWalletId = config.userWalletId,
                    selectedNetwork = null,
                    onNetworkSelected = ::changeSelectedNetwork,
                ),
            )
        }
        AddCustomTokenConfig.Step.NETWORK_SELECTOR -> {
            selectorComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenSelectorComponent.Params.NetworkSelector(
                    userWalletId = config.userWalletId,
                    selectedNetwork = config.selectedNetwork,
                    onNetworkSelected = ::changeSelectedNetwork,
                ),
            )
        }
        AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> {
            selectorComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenSelectorComponent.Params.DerivationPathSelector(
                    userWalletId = config.userWalletId,
                    selectedNetwork = requireNotNull(config.selectedNetwork) {
                        "Network is not selected"
                    },
                    selectedDerivationPath = config.selectedDerivationPath,
                    onDerivationPathSelected = ::changeDerivationPath,
                ),
            )
        }
        AddCustomTokenConfig.Step.FORM -> {
            formComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenFormComponent.Params(
                    userWalletId = config.userWalletId,
                    network = requireNotNull(config.selectedNetwork) {
                        "Network is not selected"
                    },
                    derivationPath = config.selectedDerivationPath,
                    formValues = config.formValues,
                    source = params.source,
                    onSelectNetworkClick = ::showNetworkSelector,
                    onSelectDerivationPathClick = ::showDerivationPathSelector,
                    onCurrencyAdded = ::dismissAndNotify,
                ),
            )
        }
    }

    private fun changeSelectedNetwork(network: SelectedNetwork) {
        val event = CustomTokenAnalyticsEvent.NetworkSelected(
            networkName = network.name,
            source = params.source,
        )
        analyticsEventHandler.send(event)

        showForm(network = network)
    }

    private fun changeDerivationPath(derivationPath: SelectedDerivationPath) {
        val event = CustomTokenAnalyticsEvent.DerivationSelected(
            derivationName = derivationPath.name,
            source = params.source,
        )
        analyticsEventHandler.send(event)

        showForm(derivationPath = derivationPath)
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun showDerivationPathSelector(formValues: CustomTokenFormValues) {
        val currentConfig = contentStack.value.active.configuration

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR,
            formValues = formValues,
        )
        navigation.push(config)
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun showNetworkSelector(formValues: CustomTokenFormValues) {
        val currentConfig = contentStack.value.active.configuration

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.NETWORK_SELECTOR,
            formValues = formValues,
        )
        navigation.push(config)
    }

    private fun showForm(network: SelectedNetwork? = null, derivationPath: SelectedDerivationPath? = null) {
        val currentConfig = contentStack.value.active.configuration

        val selectedNetwork = network ?: currentConfig.selectedNetwork
        val selectedDerivationPath = derivationPath ?: currentConfig.selectedDerivationPath

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.FORM,
            selectedNetwork = selectedNetwork,
            selectedDerivationPath = defineSelectedDerivationPath(selectedNetwork, selectedDerivationPath),
        )

        navigation.replaceAll(config)
    }

    private fun defineSelectedDerivationPath(
        selectedNetwork: SelectedNetwork?,
        selectedDerivationPath: SelectedDerivationPath?,
    ): SelectedDerivationPath? {
        // if default derivation path
        if (selectedDerivationPath?.value == selectedNetwork?.derivationPath) return null

        // map to custom derivation path
        return if (selectedDerivationPath != null && selectedDerivationPath.value.value != null) {
            selectedDerivationPath.copy(
                value = Network.DerivationPath.Custom(value = requireNotNull(selectedDerivationPath.value.value)),
            )
        } else {
            null
        }
    }

    private fun dismissAndNotify() {
        dismiss()
        params.onCurrencyAdded()
    }

    @AssistedFactory
    interface Factory : AddCustomTokenComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddCustomTokenComponent.Params,
        ): DefaultAddCustomTokenComponent
    }
}