package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SendWithSwapConfirmInitialStateTransformer(
    private val isShowTapHelp: Boolean,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        return ConfirmUM.Content(
            isPrimaryButtonEnabled = false,
            isTransactionInProcess = false,
            showTapHelp = isShowTapHelp,
            sendingFooter = TextReference.EMPTY,
            notifications = persistentListOf(),
        )
    }
}