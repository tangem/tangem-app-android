package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers

import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory.getErrorMessage
import com.tangem.utils.transformer.Transformer

internal class SwapChooseErrorStateTransformer(
    private val tokenName: String,
    private val onDismiss: () -> Unit,
) : Transformer<SwapChooseTokenNetworkUM> {
    override fun transform(prevState: SwapChooseTokenNetworkUM): SwapChooseTokenNetworkUM {
        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig.copy(
                content = SwapChooseTokenNetworkContentUM.Error(
                    messageContent = getErrorMessage(
                        tokenName = tokenName,
                        onDismiss = onDismiss,
                    ),
                ),
            ),
        )
    }
}