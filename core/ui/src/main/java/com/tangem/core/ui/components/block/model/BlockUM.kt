package com.tangem.core.ui.components.block.model

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import javax.annotation.concurrent.Immutable

data class BlockUM(
    val text: TextReference,
    @DrawableRes val iconRes: Int?,
    val onClick: () -> Unit,
    val accentType: AccentType = AccentType.NONE,
    val endContent: EndContent = EndContent.None,
) {

    @Immutable
    sealed interface EndContent {
        data object None : EndContent
        data class Label(
            val label: LabelUM,
        ) : EndContent

        data class Icon(
            val resId: Int,
            val accentType: AccentType = AccentType.NONE,
        ) : EndContent
    }

    enum class AccentType {
        NONE, ACCENT, WARNING,
    }
}