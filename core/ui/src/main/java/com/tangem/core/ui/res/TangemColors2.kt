@file:Suppress("LongParameterList")

package com.tangem.core.ui.res

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Stable
class TangemColors2 internal constructor(
    val text: Text,
    val graphic: Graphic,
    val button: Button,
    val surface: Surface,
    val controls: Controls,
    val field: Field,
    val overlay: Overlay,
    val border: Border,
    val fill: Fill,
    val skeleton: Skeleton,
    val markers: Markers,
    val tabs: Tabs,
) {

    @Stable
    class Text internal constructor(
        val neutral: Neutral,
        val status: Status,
    ) {
        @Stable
        class Neutral internal constructor(
            primary: Color,
            primaryInverted: Color,
            secondary: Color,
            tertiary: Color,
            primaryInvertedConstant: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var primaryInverted by mutableStateOf(primaryInverted)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set
            var primaryInvertedConstant by mutableStateOf(primaryInvertedConstant)
                private set

            fun update(other: Neutral) {
                primary = other.primary
                primaryInverted = other.primaryInverted
                secondary = other.secondary
                tertiary = other.tertiary
                primaryInvertedConstant = other.primaryInvertedConstant
            }
        }

        @Stable
        class Status internal constructor(
            disabled: Color,
            accent: Color,
            warning: Color,
            attention: Color,
            positive: Color,
        ) {
            var disabled by mutableStateOf(disabled)
                private set
            var accent by mutableStateOf(accent)
                private set
            var warning by mutableStateOf(warning)
                private set
            var attention by mutableStateOf(attention)
                private set
            var positive by mutableStateOf(positive)
                private set

            fun update(other: Status) {
                disabled = other.disabled
                accent = other.accent
                warning = other.warning
                attention = other.attention
                positive = other.positive
            }
        }

        fun update(other: Text) {
            neutral.update(other.neutral)
            status.update(other.status)
        }
    }

    @Stable
    class Graphic internal constructor(
        val neutral: Neutral,
        val status: Status,
    ) {
        @Stable
        class Neutral internal constructor(
            primary: Color,
            primaryInverted: Color,
            secondary: Color,
            tertiary: Color,
            quaternary: Color,
            primaryInvertedConstant: Color,
            tertiaryConstant: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var primaryInverted by mutableStateOf(primaryInverted)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set
            var quaternary by mutableStateOf(quaternary)
                private set
            var primaryInvertedConstant by mutableStateOf(primaryInvertedConstant)
                private set
            var tertiaryConstant by mutableStateOf(tertiaryConstant)
                private set

            fun update(other: Neutral) {
                primary = other.primary
                primaryInverted = other.primaryInverted
                secondary = other.secondary
                tertiary = other.tertiary
                quaternary = other.quaternary
                primaryInvertedConstant = other.primaryInvertedConstant
                tertiaryConstant = other.tertiaryConstant
            }
        }

        @Stable
        class Status internal constructor(
            accent: Color,
            warning: Color,
            attention: Color,
        ) {
            var accent by mutableStateOf(accent)
                private set
            var warning by mutableStateOf(warning)
                private set
            var attention by mutableStateOf(attention)
                private set

            fun update(other: Status) {
                accent = other.accent
                warning = other.warning
                attention = other.attention
            }
        }

        fun update(other: Graphic) {
            neutral.update(other.neutral)
            status.update(other.status)
        }
    }

    @Stable
    class Button internal constructor(
        backgroundPrimary: Color,
        backgroundSecondary: Color,
        backgroundDisabled: Color,
        backgroundPositive: Color,
        backgroundPrimaryInverse: Color,
        textPrimary: Color,
        textSecondary: Color,
        textDisabled: Color,
        iconPrimary: Color,
        iconSecondary: Color,
        iconDisabled: Color,
        borderPrimary: Color,
    ) {
        var backgroundPrimary by mutableStateOf(backgroundPrimary)
            private set
        var backgroundSecondary by mutableStateOf(backgroundSecondary)
            private set
        var backgroundDisabled by mutableStateOf(backgroundDisabled)
            private set
        var backgroundPositive by mutableStateOf(backgroundPositive)
            private set
        var backgroundPrimaryInverse by mutableStateOf(backgroundPrimaryInverse)
            private set
        var textPrimary by mutableStateOf(textPrimary)
            private set
        var textSecondary by mutableStateOf(textSecondary)
            private set
        var textDisabled by mutableStateOf(textDisabled)
            private set
        var iconPrimary by mutableStateOf(iconPrimary)
            private set
        var iconSecondary by mutableStateOf(iconSecondary)
            private set
        var iconDisabled by mutableStateOf(iconDisabled)
            private set
        var borderPrimary by mutableStateOf(borderPrimary)
            private set

        fun update(other: Button) {
            backgroundPrimary = other.backgroundPrimary
            backgroundSecondary = other.backgroundSecondary
            backgroundDisabled = other.backgroundDisabled
            backgroundPositive = other.backgroundPositive
            backgroundPrimaryInverse = other.backgroundPrimaryInverse
            textPrimary = other.textPrimary
            textSecondary = other.textSecondary
            textDisabled = other.textDisabled
            iconPrimary = other.iconPrimary
            iconSecondary = other.iconSecondary
            iconDisabled = other.iconDisabled
            borderPrimary = other.borderPrimary
        }
    }

    @Stable
    class Surface internal constructor(
        level1: Color,
        level2: Color,
        level3: Color,
        level4: Color,
    ) {
        var level1 by mutableStateOf(level1)
            private set
        var level2 by mutableStateOf(level2)
            private set
        var level3 by mutableStateOf(level3)
            private set
        var level4 by mutableStateOf(level4)
            private set

        fun update(other: Surface) {
            level1 = other.level1
            level2 = other.level2
            level3 = other.level3
            level4 = other.level4
        }
    }

    @Stable
    class Controls internal constructor(
        backgroundDefault: Color,
        backgroundChecked: Color,
        iconDefault: Color,
        iconDisabled: Color,
    ) {
        var backgroundDefault by mutableStateOf(backgroundDefault)
            private set
        var backgroundChecked by mutableStateOf(backgroundChecked)
            private set
        var iconDefault by mutableStateOf(iconDefault)
            private set
        var iconDisabled by mutableStateOf(iconDisabled)
            private set

        fun update(other: Controls) {
            backgroundDefault = other.backgroundDefault
            backgroundChecked = other.backgroundChecked
            iconDefault = other.iconDefault
            iconDisabled = other.iconDisabled
        }
    }

    @Stable
    class Field internal constructor(
        backgroundDefault: Color,
        backgroundFocused: Color,
        textPlaceholder: Color,
        textDefault: Color,
        textDisabled: Color,
        iconDefault: Color,
        iconDisabled: Color,
        textInvalid: Color,
        borderInvalid: Color,
    ) {
        var backgroundDefault by mutableStateOf(backgroundDefault)
            private set
        var backgroundFocused by mutableStateOf(backgroundFocused)
            private set
        var textPlaceholder by mutableStateOf(textPlaceholder)
            private set
        var textDefault by mutableStateOf(textDefault)
            private set
        var textDisabled by mutableStateOf(textDisabled)
            private set
        var iconDefault by mutableStateOf(iconDefault)
            private set
        var iconDisabled by mutableStateOf(iconDisabled)
            private set
        var textInvalid by mutableStateOf(textInvalid)
            private set
        var borderInvalid by mutableStateOf(borderInvalid)
            private set

        fun update(other: Field) {
            backgroundDefault = other.backgroundDefault
            backgroundFocused = other.backgroundFocused
            textPlaceholder = other.textPlaceholder
            textDefault = other.textDefault
            textDisabled = other.textDisabled
            iconDefault = other.iconDefault
            iconDisabled = other.iconDisabled
            textInvalid = other.textInvalid
            borderInvalid = other.borderInvalid
        }
    }

    @Stable
    class Overlay internal constructor(
        overlayPrimary: Color,
        overlaySecondary: Color,
    ) {
        var overlayPrimary by mutableStateOf(overlayPrimary)
            private set
        var overlaySecondary by mutableStateOf(overlaySecondary)
            private set

        fun update(other: Overlay) {
            overlayPrimary = other.overlayPrimary
            overlaySecondary = other.overlaySecondary
        }
    }

    @Stable
    class Border internal constructor(
        val neutral: Neutral,
        val status: Status,
    ) {

        @Stable
        class Neutral internal constructor(
            primary: Color,
            secondary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set

            fun update(other: Neutral) {
                primary = other.primary
                secondary = other.secondary
            }
        }

        @Stable
        class Status internal constructor(
            accent: Color,
            warning: Color,
            attention: Color,
        ) {
            var accent by mutableStateOf(accent)
                private set
            var warning by mutableStateOf(warning)
                private set
            var attention by mutableStateOf(attention)
                private set

            fun update(other: Status) {
                accent = other.accent
                warning = other.warning
                attention = other.attention
            }
        }

        fun update(other: Border) {
            neutral.update(other.neutral)
            status.update(other.status)
        }
    }

    @Stable
    class Fill internal constructor(
        val neutral: Neutral,
        val status: Status,
    ) {

        @Stable
        class Neutral internal constructor(
            primary: Color,
            primaryInverted: Color,
            primaryInvertedConstant: Color,
            secondary: Color,
            tertiaryConstant: Color,
            quaternary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var primaryInverted by mutableStateOf(primaryInverted)
                private set
            var primaryInvertedConstant by mutableStateOf(primaryInvertedConstant)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiaryConstant by mutableStateOf(tertiaryConstant)
                private set
            var quaternary by mutableStateOf(quaternary)
                private set

            fun update(other: Neutral) {
                primary = other.primary
                primaryInverted = other.primaryInverted
                primaryInvertedConstant = other.primaryInvertedConstant
                secondary = other.secondary
                tertiaryConstant = other.tertiaryConstant
                quaternary = other.quaternary
            }
        }

        @Stable
        class Status internal constructor(
            accent: Color,
            warning: Color,
            attention: Color,
        ) {
            var accent by mutableStateOf(accent)
                private set
            var warning by mutableStateOf(warning)
                private set
            var attention by mutableStateOf(attention)
                private set

            fun update(other: Status) {
                accent = other.accent
                warning = other.warning
                attention = other.attention
            }
        }

        fun update(other: Fill) {
            neutral.update(other.neutral)
            status.update(other.status)
        }
    }

    @Stable
    class Skeleton internal constructor(
        backgroundPrimary: Color,
    ) {
        var backgroundPrimary by mutableStateOf(backgroundPrimary)
            private set

        fun update(other: Skeleton) {
            backgroundPrimary = other.backgroundPrimary
        }
    }

    @Stable
    class Markers internal constructor(
        backgroundSolidGray: Color,
        backgroundDisabled: Color,
        backgroundSolidBlue: Color,
        textGray: Color,
        textDisabled: Color,
        iconGray: Color,
        iconDisabled: Color,
        borderGray: Color,
        backgroundTintedBlue: Color,
        textBlue: Color,
        backgroundSolidRed: Color,
        backgroundTintedRed: Color,
        iconBlue: Color,
        iconRed: Color,
        textRed: Color,
        backgroundTintedGray: Color,
        borderTintedBlue: Color,
        borderTintedRed: Color,
    ) {
        var backgroundSolidGray by mutableStateOf(backgroundSolidGray)
            private set
        var backgroundDisabled by mutableStateOf(backgroundDisabled)
            private set
        var backgroundSolidBlue by mutableStateOf(backgroundSolidBlue)
            private set
        var textGray by mutableStateOf(textGray)
            private set
        var textDisabled by mutableStateOf(textDisabled)
            private set
        var iconGray by mutableStateOf(iconGray)
            private set
        var iconDisabled by mutableStateOf(iconDisabled)
            private set
        var borderGray by mutableStateOf(borderGray)
            private set
        var backgroundTintedBlue by mutableStateOf(backgroundTintedBlue)
            private set
        var textBlue by mutableStateOf(textBlue)
            private set
        var backgroundSolidRed by mutableStateOf(backgroundSolidRed)
            private set
        var backgroundTintedRed by mutableStateOf(backgroundTintedRed)
            private set
        var iconBlue by mutableStateOf(iconBlue)
            private set
        var iconRed by mutableStateOf(iconRed)
            private set
        var textRed by mutableStateOf(textRed)
            private set
        var backgroundTintedGray by mutableStateOf(backgroundTintedGray)
            private set
        var borderTintedBlue by mutableStateOf(borderTintedBlue)
            private set
        var borderTintedRed by mutableStateOf(borderTintedRed)
            private set

        fun update(other: Markers) {
            backgroundSolidGray = other.backgroundSolidGray
            backgroundDisabled = other.backgroundDisabled
            backgroundSolidBlue = other.backgroundSolidBlue
            textGray = other.textGray
            textDisabled = other.textDisabled
            iconGray = other.iconGray
            iconDisabled = other.iconDisabled
            borderGray = other.borderGray
            backgroundTintedBlue = other.backgroundTintedBlue
            textBlue = other.textBlue
            backgroundSolidRed = other.backgroundSolidRed
            backgroundTintedRed = other.backgroundTintedRed
            iconBlue = other.iconBlue
            iconRed = other.iconRed
            textRed = other.textRed
            backgroundTintedGray = other.backgroundTintedGray
            borderTintedBlue = other.borderTintedBlue
            borderTintedRed = other.borderTintedRed
        }
    }

    @Stable
    class Tabs internal constructor(
        textPrimary: Color,
        textSecondary: Color,
        textTertiary: Color,
        backgroundPrimary: Color,
        backgroundSecondary: Color,
        backgroundTertiary: Color,
        backgroundQuaternary: Color,
    ) {
        var textPrimary by mutableStateOf(textPrimary)
            private set
        var textSecondary by mutableStateOf(textSecondary)
            private set
        var textTertiary by mutableStateOf(textTertiary)
            private set
        var backgroundPrimary by mutableStateOf(backgroundPrimary)
            private set
        var backgroundSecondary by mutableStateOf(backgroundSecondary)
            private set
        var backgroundTertiary by mutableStateOf(backgroundTertiary)
            private set
        var backgroundQuaternary by mutableStateOf(backgroundQuaternary)
            private set

        fun update(other: Tabs) {
            textPrimary = other.textPrimary
            textSecondary = other.textSecondary
            textTertiary = other.textTertiary
            backgroundPrimary = other.backgroundPrimary
            backgroundSecondary = other.backgroundSecondary
            backgroundTertiary = other.backgroundTertiary
            backgroundQuaternary = other.backgroundQuaternary
        }
    }

    fun update(other: TangemColors2) {
        text.update(other.text)
        graphic.update(other.graphic)
        button.update(other.button)
        surface.update(other.surface)
        controls.update(other.controls)
        field.update(other.field)
        overlay.update(other.overlay)
        border.update(other.border)
        fill.update(other.fill)
        skeleton.update(other.skeleton)
        markers.update(other.markers)
        tabs.update(other.tabs)
    }
}