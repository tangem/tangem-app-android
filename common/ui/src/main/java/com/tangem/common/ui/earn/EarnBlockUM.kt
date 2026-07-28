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
        data class Glowing(@DrawableRes val iconRes: Int, val tone: Tone = Tone.Accent) : IconUM
        data class Plain(@DrawableRes val iconRes: Int) : IconUM

        /** Accent — glow/tint follow the block [Type]; Warning — yellow icon tint and glow. */
        enum class Tone { Accent, Warning }
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
        ) : SubtitleUM

        data class AccentedText(
            val text: TextReference,
            val accent: TextReference,
            val style: Style,
        ) : SubtitleUM

        enum class Style { Large, Small }
        enum class Tone { Primary, Disabled, Accent }
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

        /** Chevron indicating the whole row is clickable. */
        data object Chevron : TrailingUM

        /** Status icon (warning / info) placed in the trailing slot. */
        data class StatusIcon(val tone: Tone) : TrailingUM {
            enum class Tone { Warning, Info }
        }

        /** Circular progress indicator placed in the trailing slot (enabling / disabling states). */
        data class Loader(val tone: LoaderTone) : TrailingUM {
            enum class LoaderTone { Positive, Muted }
        }
    }
}