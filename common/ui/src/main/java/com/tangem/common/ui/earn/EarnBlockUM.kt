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

    /**
     * Promo variant — a column with the [iconUM] + annotated multiline [title] + [subtitle] row on top
     * and a pair of full-width buttons below. Button labels are fixed and hardcoded in the composable,
     * so only their click handlers ([onSecondaryClick], [onPrimaryClick]) are exposed here.
     */
    data class Promo(
        val type: Type,
        val backgroundUM: BackgroundUM,
        val iconUM: IconUM,
        val title: TextReference,
        val subtitle: TextReference,
        val onPrimaryClick: () -> Unit,
        val onSecondaryClick: () -> Unit,
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
        val iconUM: IconUM? = null,
    ) {
        enum class Style { Large, Small }
        enum class Tone { Primary, Secondary, Disabled, Accent }

        data class IconUM(val tone: IconTone)
        enum class IconTone { Warning, Info }
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
            val style: Style = Style.Default,
        ) : TrailingUM {
            enum class Style { Default, Secondary }
        }

        data class Balance(
            val fiatValue: TextReference,
            val cryptoValue: TextReference,
            val isBalanceHidden: Boolean,
        ) : TrailingUM
    }
}