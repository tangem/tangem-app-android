package com.tangem.features.send.sendnft.confirm.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class NFTSendConfirmInitialStateTransformer(
    private val isShowTapHelp: Boolean,
    private val walletName: TextReference,
) : Transformer<ConfirmUM> {
    override fun transform(prevState: ConfirmUM): ConfirmUM {
        return ConfirmUM.Content(
            isSending = false,
            isShowTapHelp = isShowTapHelp,
            sendingFooter = TextReference.EMPTY,
            walletName = walletName,
            notifications = persistentListOf(),
        )
    }
}