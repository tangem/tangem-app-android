package com.tangem.features.send.v2.send.confirm.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SendConfirmInitialStateTransformer(
    private val isShowTapHelp: Boolean,
    private val walletName: TextReference,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        return ConfirmUM.Content(
            walletName = walletName,
            isSending = false,
            showTapHelp = isShowTapHelp,
            sendingFooter = TextReference.EMPTY,
            notifications = persistentListOf(),
        )
    }
}