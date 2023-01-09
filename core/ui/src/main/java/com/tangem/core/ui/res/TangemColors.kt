package com.tangem.core.ui.res

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Suppress("unused", "MemberVisibilityCanBePrivate")
@Stable
class TangemColors internal constructor(
    text: Text,
    icon: Icon,
    button: Button,
    background: Background,
    control: Control,
    stroke: Stroke,
    field: Field,
) {
    var text by mutableStateOf(text)
        private set
    var icon by mutableStateOf(icon)
        private set
    var button by mutableStateOf(button)
        private set
    var background by mutableStateOf(background)
        private set
    var control by mutableStateOf(control)
        private set
    var stroke by mutableStateOf(stroke)
        private set
    var field by mutableStateOf(field)
        private set

    @Stable
    class Text internal constructor(
        primary1: Color,
        primary2: Color,
        secondary: Color,
        tertiary: Color,
        disabled: Color,
        accent: Color = TangemColorPalette.Meadow,
        warning: Color = TangemColorPalette.Amaranth,
        attention: Color = TangemColorPalette.Tangerine,
        constantWhite: Color = TangemColorPalette.White,
    ) {
        var primary1 by mutableStateOf(primary1)
            private set
        var primary2 by mutableStateOf(primary2)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var tertiary by mutableStateOf(tertiary)
            private set
        var disabled by mutableStateOf(disabled)
            private set
        var accent by mutableStateOf(accent)
            private set
        var warning by mutableStateOf(warning)
            private set
        var attention by mutableStateOf(attention)
            private set
        var constantWhite by mutableStateOf(constantWhite)
            private set

        fun update(other: Text) {
            primary1 = other.primary1
            primary2 = other.primary2
            secondary = other.secondary
            tertiary = other.tertiary
            disabled = other.disabled
            accent = other.accent
        }
    }

    @Stable
    class Icon internal constructor(
        primary1: Color,
        primary2: Color,
        secondary: Color,
        informative: Color,
        inactive: Color,
        accent: Color = TangemColorPalette.Meadow,
        warning: Color = TangemColorPalette.Amaranth,
        attention: Color = TangemColorPalette.Tangerine,
    ) {
        var primary1 by mutableStateOf(primary1)
            private set
        var primary2 by mutableStateOf(primary2)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var informative by mutableStateOf(informative)
            private set
        var inactive by mutableStateOf(inactive)
            private set
        var accent by mutableStateOf(accent)
            private set
        var warning by mutableStateOf(warning)
            private set
        var attention by mutableStateOf(attention)
            private set

        fun update(other: Icon) {
            primary1 = other.primary1
            primary2 = other.primary2
            secondary = other.secondary
            informative = other.informative
            inactive = other.inactive
            accent = other.accent
            warning = other.warning
            attention = other.attention
        }
    }

    @Stable
    class Button internal constructor(
        primary: Color,
        secondary: Color,
        disabled: Color,
        positive: Color = TangemColorPalette.Meadow,
        positiveDisabled: Color,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var disabled by mutableStateOf(disabled)
            private set
        var positive by mutableStateOf(positive)
            private set
        var positiveDisabled by mutableStateOf(positiveDisabled)
            private set

        fun update(other: Button) {
            primary = other.primary
            secondary = other.secondary
            disabled = other.disabled
            positive = other.positive
            positiveDisabled = other.positiveDisabled
        }
    }

    @Stable
    class Background internal constructor(
        primary: Color,
        secondary: Color,
        plain: Color,
        action: Color,
        fade: Color,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var plain by mutableStateOf(plain)
            private set
        var action by mutableStateOf(action)
            private set
        var fade by mutableStateOf(fade)
            private set

        fun update(other: Background) {
            primary = other.primary
            secondary = other.secondary
            plain = other.plain
            action = other.action
            fade = other.fade
        }
    }

    @Stable
    class Control internal constructor(
        checked: Color,
        unchecked: Color,
        key: Color,
    ) {
        var checked by mutableStateOf(checked)
            private set
        var unchecked by mutableStateOf(unchecked)
            private set
        var key by mutableStateOf(key)
            private set

        fun update(other: Control) {
            checked = other.checked
            unchecked = other.unchecked
            key = other.key
        }
    }

    @Stable
    class Stroke internal constructor(
        primary: Color,
        secondary: Color,
        transparency: Color,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var transparency by mutableStateOf(transparency)
            private set

        fun update(other: Stroke) {
            primary = other.primary
            secondary = other.secondary
            transparency = other.transparency
        }
    }

    @Stable
    class Field internal constructor(
        primary: Color,
        focused: Color,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var focused by mutableStateOf(focused)
            private set

        fun update(other: Field) {
            primary = other.primary
            focused = other.focused
        }
    }

    fun update(other: TangemColors) {
        text.update(other.text)
        icon.update(other.icon)
        button.update(other.button)
        background.update(other.background)
        control.update(other.control)
        stroke.update(other.stroke)
        field.update(other.field)
    }
}
