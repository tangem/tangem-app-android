package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.messageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.secondaryButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.entity.TangemPayViewPinUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayViewPinErrorStateTransformer : Transformer<TangemPayViewPinUM> {

    override fun transform(prevState: TangemPayViewPinUM): TangemPayViewPinUM {
        return TangemPayViewPinUM.Error(
            errorMessage = messageBottomSheetUM {
                infoBlock {
                    icon(R.drawable.img_attention_20) {
                        backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Attention
                    }
                    title = TextReference.Res(R.string.common_error)
                    body = TextReference.Res(R.string.common_unknown_error)
                }
                secondaryButton {
                    text = resourceReference(R.string.common_got_it)
                    onClick { prevState.onDismiss() }
                }
            },
            onDismiss = prevState.onDismiss,
        )
    }
}