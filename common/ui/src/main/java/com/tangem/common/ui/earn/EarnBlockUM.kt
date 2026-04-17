package com.tangem.common.ui.earn

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface EarnBlockUM {

    data object Loading : EarnBlockUM

    data class Content(
        val backgroundUM: BackgroundUM,
        val iconUM: IconUM,
        val titleUM: TitleUM,
        val subtitleUM: SubtitleUM?,
        val trailingUM: TrailingUM?,
    ) : EarnBlockUM

    @Immutable
    sealed interface BackgroundUM {
        data object Surface : BackgroundUM
        data class Tinted(val color: ColorReference2) : BackgroundUM
    }

    @Immutable
    sealed interface IconUM {
        data class Glowing(
            @DrawableRes val iconRes: Int,
            val glowColor: ColorReference2,
        ) : IconUM

        data class Plain(
            @DrawableRes val iconRes: Int,
        ) : IconUM
    }

    @Immutable
    data class TitleUM(
        val text: TextReference,
        val style: Style,
        val color: ColorReference2,
    ) {
        enum class Style { Large, Small }
    }

    @Immutable
    sealed interface SubtitleUM {
        data class Text(
            val text: TextReference,
            val style: Style,
            val color: ColorReference2,
        ) : SubtitleUM

        enum class Style { Large, Small }
    }

    @Immutable
    sealed interface TrailingUM {
        data class Button(val buttonUM: TangemButtonUM) : TrailingUM

        data class Balance(
            val fiatValue: TextReference,
            val cryptoValue: TextReference,
            val isBalanceHidden: Boolean,
            val onClick: () -> Unit,
        ) : TrailingUM
    }
}