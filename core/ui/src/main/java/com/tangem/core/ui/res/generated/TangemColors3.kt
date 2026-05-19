@file:Suppress("all")

package com.tangem.core.ui.res.generated

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Auto-generated from design tokens. Do not edit manually.
 */
@Stable
class TangemColors3 internal constructor(
    val text: Text,
    val bg: Bg,
    val icon: Icon,
    val border: Border,
    val overlay: Overlay,
    val interaction: Interaction,
    val material: Material,
) {

    @Stable
    class Text internal constructor(
        primary: Color,
        secondary: Color,
        tertiary: Color,
        brand: Color,
        val staticLight: StaticLight,
        val staticDark: StaticDark,
        val inverse: Inverse,
        val status: Status,
        val accent: Accent,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var tertiary by mutableStateOf(tertiary)
            private set
        var brand by mutableStateOf(brand)
            private set

        @Stable
        class StaticLight internal constructor(
            primary: Color,
            secondary: Color,
            tertiary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set

            fun update(other: StaticLight) {
                primary = other.primary
                secondary = other.secondary
                tertiary = other.tertiary
            }
        }

        @Stable
        class StaticDark internal constructor(
            primary: Color,
            secondary: Color,
            tertiary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set

            fun update(other: StaticDark) {
                primary = other.primary
                secondary = other.secondary
                tertiary = other.tertiary
            }
        }

        @Stable
        class Inverse internal constructor(
            primary: Color,
            secondary: Color,
            tertiary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set

            fun update(other: Inverse) {
                primary = other.primary
                secondary = other.secondary
                tertiary = other.tertiary
            }
        }

        @Stable
        class Status internal constructor(
            success: Color,
            error: Color,
            warning: Color,
            info: Color,
        ) {
            var success by mutableStateOf(success)
                private set
            var error by mutableStateOf(error)
                private set
            var warning by mutableStateOf(warning)
                private set
            var info by mutableStateOf(info)
                private set

            fun update(other: Status) {
                success = other.success
                error = other.error
                warning = other.warning
                info = other.info
            }
        }

        @Stable
        class Accent internal constructor(
            blue: Color,
            violet: Color,
            red: Color,
            orange: Color,
            yellow: Color,
            green: Color,
        ) {
            var blue by mutableStateOf(blue)
                private set
            var violet by mutableStateOf(violet)
                private set
            var red by mutableStateOf(red)
                private set
            var orange by mutableStateOf(orange)
                private set
            var yellow by mutableStateOf(yellow)
                private set
            var green by mutableStateOf(green)
                private set

            fun update(other: Accent) {
                blue = other.blue
                violet = other.violet
                red = other.red
                orange = other.orange
                yellow = other.yellow
                green = other.green
            }
        }

        fun update(other: Text) {
            primary = other.primary
            secondary = other.secondary
            tertiary = other.tertiary
            brand = other.brand
            staticLight.update(other.staticLight)
            staticDark.update(other.staticDark)
            inverse.update(other.inverse)
            status.update(other.status)
            accent.update(other.accent)
        }
    }

    @Stable
    class Bg internal constructor(
        primary: Color,
        secondary: Color,
        tertiary: Color,
        brand: Color,
        inverse: Color,
        base: Color,
        disabled: Color,
        val opaque: Opaque,
        val status: Status,
        val accent: Accent,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var tertiary by mutableStateOf(tertiary)
            private set
        var brand by mutableStateOf(brand)
            private set
        var inverse by mutableStateOf(inverse)
            private set
        var base by mutableStateOf(base)
            private set
        var disabled by mutableStateOf(disabled)
            private set

        @Stable
        class Opaque internal constructor(
            primary: Color,
            secondary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set

            fun update(other: Opaque) {
                primary = other.primary
                secondary = other.secondary
            }
        }

        @Stable
        class Status internal constructor(
            success: Color,
            successSubtle: Color,
            error: Color,
            errorSubtle: Color,
            warning: Color,
            warningSubtle: Color,
            info: Color,
            infoSubtle: Color,
        ) {
            var success by mutableStateOf(success)
                private set
            var successSubtle by mutableStateOf(successSubtle)
                private set
            var error by mutableStateOf(error)
                private set
            var errorSubtle by mutableStateOf(errorSubtle)
                private set
            var warning by mutableStateOf(warning)
                private set
            var warningSubtle by mutableStateOf(warningSubtle)
                private set
            var info by mutableStateOf(info)
                private set
            var infoSubtle by mutableStateOf(infoSubtle)
                private set

            fun update(other: Status) {
                success = other.success
                successSubtle = other.successSubtle
                error = other.error
                errorSubtle = other.errorSubtle
                warning = other.warning
                warningSubtle = other.warningSubtle
                info = other.info
                infoSubtle = other.infoSubtle
            }
        }

        @Stable
        class Accent internal constructor(
            blue: Color,
            violet: Color,
            red: Color,
            orange: Color,
            yellow: Color,
            green: Color,
        ) {
            var blue by mutableStateOf(blue)
                private set
            var violet by mutableStateOf(violet)
                private set
            var red by mutableStateOf(red)
                private set
            var orange by mutableStateOf(orange)
                private set
            var yellow by mutableStateOf(yellow)
                private set
            var green by mutableStateOf(green)
                private set

            fun update(other: Accent) {
                blue = other.blue
                violet = other.violet
                red = other.red
                orange = other.orange
                yellow = other.yellow
                green = other.green
            }
        }

        fun update(other: Bg) {
            primary = other.primary
            secondary = other.secondary
            tertiary = other.tertiary
            brand = other.brand
            inverse = other.inverse
            base = other.base
            disabled = other.disabled
            opaque.update(other.opaque)
            status.update(other.status)
            accent.update(other.accent)
        }
    }

    @Stable
    class Icon internal constructor(
        primary: Color,
        secondary: Color,
        tertiary: Color,
        brand: Color,
        staticLight: Color,
        staticDark: Color,
        inverse: Color,
        val status: Status,
        val accent: Accent,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var tertiary by mutableStateOf(tertiary)
            private set
        var brand by mutableStateOf(brand)
            private set
        var staticLight by mutableStateOf(staticLight)
            private set
        var staticDark by mutableStateOf(staticDark)
            private set
        var inverse by mutableStateOf(inverse)
            private set

        @Stable
        class Status internal constructor(
            success: Color,
            error: Color,
            warning: Color,
            info: Color,
        ) {
            var success by mutableStateOf(success)
                private set
            var error by mutableStateOf(error)
                private set
            var warning by mutableStateOf(warning)
                private set
            var info by mutableStateOf(info)
                private set

            fun update(other: Status) {
                success = other.success
                error = other.error
                warning = other.warning
                info = other.info
            }
        }

        @Stable
        class Accent internal constructor(
            blue: Color,
            violet: Color,
            red: Color,
            orange: Color,
            yellow: Color,
            green: Color,
        ) {
            var blue by mutableStateOf(blue)
                private set
            var violet by mutableStateOf(violet)
                private set
            var red by mutableStateOf(red)
                private set
            var orange by mutableStateOf(orange)
                private set
            var yellow by mutableStateOf(yellow)
                private set
            var green by mutableStateOf(green)
                private set

            fun update(other: Accent) {
                blue = other.blue
                violet = other.violet
                red = other.red
                orange = other.orange
                yellow = other.yellow
                green = other.green
            }
        }

        fun update(other: Icon) {
            primary = other.primary
            secondary = other.secondary
            tertiary = other.tertiary
            brand = other.brand
            staticLight = other.staticLight
            staticDark = other.staticDark
            inverse = other.inverse
            status.update(other.status)
            accent.update(other.accent)
        }
    }

    @Stable
    class Border internal constructor(
        primary: Color,
        secondary: Color,
        tertiary: Color,
        brand: Color,
        val inverse: Inverse,
        val status: Status,
        val accent: Accent,
    ) {
        var primary by mutableStateOf(primary)
            private set
        var secondary by mutableStateOf(secondary)
            private set
        var tertiary by mutableStateOf(tertiary)
            private set
        var brand by mutableStateOf(brand)
            private set

        @Stable
        class Inverse internal constructor(
            primary: Color,
            secondary: Color,
            tertiary: Color,
        ) {
            var primary by mutableStateOf(primary)
                private set
            var secondary by mutableStateOf(secondary)
                private set
            var tertiary by mutableStateOf(tertiary)
                private set

            fun update(other: Inverse) {
                primary = other.primary
                secondary = other.secondary
                tertiary = other.tertiary
            }
        }

        @Stable
        class Status internal constructor(
            success: Color,
            successSubtle: Color,
            error: Color,
            errorSubtle: Color,
            warning: Color,
            warningSubtle: Color,
            info: Color,
            infoSubtle: Color,
        ) {
            var success by mutableStateOf(success)
                private set
            var successSubtle by mutableStateOf(successSubtle)
                private set
            var error by mutableStateOf(error)
                private set
            var errorSubtle by mutableStateOf(errorSubtle)
                private set
            var warning by mutableStateOf(warning)
                private set
            var warningSubtle by mutableStateOf(warningSubtle)
                private set
            var info by mutableStateOf(info)
                private set
            var infoSubtle by mutableStateOf(infoSubtle)
                private set

            fun update(other: Status) {
                success = other.success
                successSubtle = other.successSubtle
                error = other.error
                errorSubtle = other.errorSubtle
                warning = other.warning
                warningSubtle = other.warningSubtle
                info = other.info
                infoSubtle = other.infoSubtle
            }
        }

        @Stable
        class Accent internal constructor(
            blue: Color,
            violet: Color,
            red: Color,
            orange: Color,
            yellow: Color,
            green: Color,
        ) {
            var blue by mutableStateOf(blue)
                private set
            var violet by mutableStateOf(violet)
                private set
            var red by mutableStateOf(red)
                private set
            var orange by mutableStateOf(orange)
                private set
            var yellow by mutableStateOf(yellow)
                private set
            var green by mutableStateOf(green)
                private set

            fun update(other: Accent) {
                blue = other.blue
                violet = other.violet
                red = other.red
                orange = other.orange
                yellow = other.yellow
                green = other.green
            }
        }

        fun update(other: Border) {
            primary = other.primary
            secondary = other.secondary
            tertiary = other.tertiary
            brand = other.brand
            inverse.update(other.inverse)
            status.update(other.status)
            accent.update(other.accent)
        }
    }

    @Stable
    class Overlay internal constructor(
        modal: Color,
    ) {
        var modal by mutableStateOf(modal)
            private set

        fun update(other: Overlay) {
            modal = other.modal
        }
    }

    @Stable
    class Interaction internal constructor(
        pressStaticLight: Color,
        pressStaticDark: Color,
        val press: Press,
        val focusRing: FocusRing,
    ) {
        var pressStaticLight by mutableStateOf(pressStaticLight)
            private set
        var pressStaticDark by mutableStateOf(pressStaticDark)
            private set

        @Stable
        class Press internal constructor(
            default: Color,
            inverse: Color,
        ) {
            var default by mutableStateOf(default)
                private set
            var inverse by mutableStateOf(inverse)
                private set

            fun update(other: Press) {
                default = other.default
                inverse = other.inverse
            }
        }

        @Stable
        class FocusRing internal constructor(
            default: Color,
            brand: Color,
        ) {
            var default by mutableStateOf(default)
                private set
            var brand by mutableStateOf(brand)
                private set

            fun update(other: FocusRing) {
                default = other.default
                brand = other.brand
            }
        }

        fun update(other: Interaction) {
            pressStaticLight = other.pressStaticLight
            pressStaticDark = other.pressStaticDark
            press.update(other.press)
            focusRing.update(other.focusRing)
        }
    }

    @Stable
    class Material internal constructor(
        val tint: Tint,
        val fill: Fill,
        val lighten: Lighten,
        val softLight: SoftLight,
        val border: Border,
    ) {

        @Stable
        class Tint internal constructor(
            glass: Color,
            blur: Color,
            solid: Color,
        ) {
            var glass by mutableStateOf(glass)
                private set
            var blur by mutableStateOf(blur)
                private set
            var solid by mutableStateOf(solid)
                private set

            fun update(other: Tint) {
                glass = other.glass
                blur = other.blur
                solid = other.solid
            }
        }

        @Stable
        class Fill internal constructor(
            glass: Color,
            blur: Color,
            solid: Color,
        ) {
            var glass by mutableStateOf(glass)
                private set
            var blur by mutableStateOf(blur)
                private set
            var solid by mutableStateOf(solid)
                private set

            fun update(other: Fill) {
                glass = other.glass
                blur = other.blur
                solid = other.solid
            }
        }

        @Stable
        class Lighten internal constructor(
            glass: Color,
            blur: Color,
            solid: Color,
        ) {
            var glass by mutableStateOf(glass)
                private set
            var blur by mutableStateOf(blur)
                private set
            var solid by mutableStateOf(solid)
                private set

            fun update(other: Lighten) {
                glass = other.glass
                blur = other.blur
                solid = other.solid
            }
        }

        @Stable
        class SoftLight internal constructor(
            glass: Color,
            blur: Color,
            solid: Color,
        ) {
            var glass by mutableStateOf(glass)
                private set
            var blur by mutableStateOf(blur)
                private set
            var solid by mutableStateOf(solid)
                private set

            fun update(other: SoftLight) {
                glass = other.glass
                blur = other.blur
                solid = other.solid
            }
        }

        @Stable
        class Border internal constructor(
            start: Color,
            mid: Color,
            end: Color,
        ) {
            var start by mutableStateOf(start)
                private set
            var mid by mutableStateOf(mid)
                private set
            var end by mutableStateOf(end)
                private set

            fun update(other: Border) {
                start = other.start
                mid = other.mid
                end = other.end
            }
        }

        fun update(other: Material) {
            tint.update(other.tint)
            fill.update(other.fill)
            lighten.update(other.lighten)
            softLight.update(other.softLight)
            border.update(other.border)
        }
    }

    fun update(other: TangemColors3) {
        text.update(other.text)
        bg.update(other.bg)
        icon.update(other.icon)
        border.update(other.border)
        overlay.update(other.overlay)
        interaction.update(other.interaction)
        material.update(other.material)
    }
}