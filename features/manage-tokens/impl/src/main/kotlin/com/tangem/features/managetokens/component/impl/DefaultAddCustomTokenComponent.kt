package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.AddCustomTokenBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAddCustomTokenComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AddCustomTokenComponent.Params,
    private val selectorComponentFactory: CustomTokenSelectorComponent.Factory,
    private val formComponentFactory: CustomTokenFormComponent.Factory,
) : AddCustomTokenComponent, AppComponentContext by context {

    private val navigation = StackNavigation<AddCustomTokenConfig>()
    private val contentStack = childStack(
        key = "add_custom_token_content_stack",
        source = navigation,
        initialConfiguration = AddCustomTokenConfig(
            userWalletId = params.userWalletId,
            step = AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
            popBack = ::dismiss,
        ),
        handleBackButton = true,
        serializer = AddCustomTokenConfig.serializer(),
        childFactory = ::contentChild,
    )

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val config = remember {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = ::dismiss,
                content = contentStack.active.configuration,
            )
        }
        val childStack by contentStack.subscribeAsState()

        AddCustomTokenBottomSheet(
            config = config.copy(
                content = childStack.active.configuration,
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
                    onNetworkSelected = { network ->
                        showForm(network = network)
                    },
                ),
            )
        }
        AddCustomTokenConfig.Step.NETWORK_SELECTOR -> {
            selectorComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenSelectorComponent.Params.NetworkSelector(
                    userWalletId = config.userWalletId,
                    selectedNetwork = config.selectedNetwork,
                    onNetworkSelected = { network ->
                        showForm(network = network)
                    },
                ),
            )
        }
        AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> {
            selectorComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenSelectorComponent.Params.DerivationPathSelector(
                    userWalletId = config.userWalletId,
                    selectedDerivationPath = config.selectedDerivationPath,
                    onDerivationPathSelected = { derivationPath ->
                        showForm(derivationPath = derivationPath)
                    },
                ),
            )
        }
        AddCustomTokenConfig.Step.FORM -> {
            formComponentFactory.create(
                context = childByContext(componentContext),
                params = CustomTokenFormComponent.Params(
                    userWalletId = config.userWalletId,
                    network = config.selectedNetwork ?: error("Network is not selected"),
                    derivationPath = config.selectedDerivationPath ?: SelectedDerivationPath(
                        value = config.selectedNetwork.derivationPath,
                        name = resourceReference(R.string.custom_token_derivation_path_default),
                    ),
                    formValues = config.formValues,
                    onSelectNetworkClick = ::showNetworkSelector,
                    onSelectDerivationPathClick = ::showDerivationPathSelector,
                ),
            )
        }
    }

    private fun showDerivationPathSelector(formValues: CustomTokenFormValues) {
        val currentConfig = contentStack.value.active.configuration

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR,
            formValues = formValues,
            popBack = navigation::pop,
        )
        navigation.push(config)
    }

    private fun showNetworkSelector(formValues: CustomTokenFormValues) {
        val currentConfig = contentStack.value.active.configuration

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.NETWORK_SELECTOR,
            formValues = formValues,
            popBack = navigation::pop,
        )
        navigation.push(config)
    }

    private fun showForm(network: SelectedNetwork? = null, derivationPath: SelectedDerivationPath? = null) {
        val currentConfig = contentStack.value.active.configuration

        val config = currentConfig.copy(
            step = AddCustomTokenConfig.Step.FORM,
            selectedNetwork = network ?: currentConfig.selectedNetwork,
            selectedDerivationPath = derivationPath ?: currentConfig.selectedDerivationPath,
            popBack = ::dismiss,
        )
        navigation.replaceAll(config)
    }

    @AssistedFactory
    interface Factory : AddCustomTokenComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: AddCustomTokenComponent.Params,
        ): DefaultAddCustomTokenComponent
    }
}
