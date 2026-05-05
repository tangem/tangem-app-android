package com.tangem.common.ui.earn

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface EarnBlockUM {

    data object Loading : EarnBlockUM

    data class Content(
        val type: Type,
        val backgroundUM: BackgroundUM,
        val iconUM: IconUM,
        val titleUM: TitleUM,
        val subtitleUM: SubtitleUM?,
        val trailingUM: TrailingUM?,
        val onClick: (() -> Unit)? = null,
    ) : EarnBlockUM

    enum class Type { Staking, YieldSupply }

    @Immutable
    sealed interface BackgroundUM {
        data object Surface : BackgroundUM
        data object AccentSoft : BackgroundUM
        data object AccentStrong : BackgroundUM
    }

    @Immutable
    sealed interface IconUM {
        data class Glowing(@DrawableRes val iconRes: Int) : IconUM
        data class Plain(@DrawableRes val iconRes: Int) : IconUM
    }

    @Immutable
    data class TitleUM(
        val text: TextReference,
        val style: Style,
        val tone: Tone,
    ) {
        enum class Style { Large, Small }
        enum class Tone { Primary, Secondary, Disabled, Accent }
    }

    @Immutable
    sealed interface SubtitleUM {
        data class Text(
            val text: TextReference,
            val style: Style,
            val tone: Tone,
            val loader: Loader? = null,
        ) : SubtitleUM

        data class Loader(val tone: LoaderTone)

        enum class Style { Large, Small }
        enum class Tone { Primary, Disabled, Accent }
        enum class LoaderTone { Positive, Muted }
    }

    @Immutable
    sealed interface TrailingUM {
        data class Button(
            val text: TextReference,
            val isEnabled: Boolean = true,
        ) : TrailingUM

        data class Balance(
            val fiatValue: TextReference,
            val cryptoValue: TextReference,
            val isBalanceHidden: Boolean,
        ) : TrailingUM

        data class Icon(
            val tone: IconTone,
        ) : TrailingUM

        enum class IconTone { Warning, Info }
    }
}