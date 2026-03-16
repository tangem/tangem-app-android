package com.tangem.core.ui.components.bottomsheets.message

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
data class MessageBottomSheetUM(
    var elements: ImmutableList<Element> = persistentListOf(),
    var onDismissRequest: () -> Unit = {},
) : TangemBottomSheetConfigContent {

    @Immutable
    inner class CloseScope {
        fun closeBs() {
            onDismissRequest()
        }
    }

    val closeScope = CloseScope()

    @Immutable
    sealed interface Element

    @Immutable
    data class Icon(
        @DrawableRes internal var res: Int,
        var type: Type = Type.Unspecified,
        var backgroundType: BackgroundType = BackgroundType.Unspecified,
    ) : Element {
        enum class Type {
            Unspecified, Accent, Informative, Attention, Warning,
        }

        enum class BackgroundType {
            Unspecified, SameAsTint, Accent, Informative, Attention, Warning,
        }
    }

    @Immutable
    data class IconImage(@DrawableRes internal var res: Int) : Element

    @Immutable
    data class Chip(
        internal var text: TextReference,
        var type: Type = Type.Unspecified,
    ) : Element {
        enum class Type {
            Unspecified, Warning
        }
    }

    @Immutable
    data class InfoBlock(
        internal var icon: Icon? = null,
        internal var iconImage: IconImage? = null,
        internal var chip: Chip? = null,
        var title: TextReference? = null,
        var body: TextReference? = null,
    ) : Element

    @Immutable
    data class Button(
        internal var isPrimary: Boolean = false,
        var text: TextReference? = null,
        @DrawableRes internal var iconInternal: Int? = null,
        internal var iconOrder: IconOrder = IconOrder.Start,
        var onClick: (CloseScope.() -> Unit)? = null,
    ) : Element {

        var icon: Int? = iconInternal
            set(value) {
                iconOrder = if (text == null) {
                    IconOrder.Start
                } else {
                    IconOrder.End
                }
                field = value
            }

        enum class IconOrder {
            Start, End
        }
    }
}

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class MessageBottomSheetDsl

// region: DSL

fun messageBottomSheetUM(init: @MessageBottomSheetDsl MessageBottomSheetUM.() -> Unit) =
    MessageBottomSheetUM().apply(init)

fun MessageBottomSheetUM.onDismiss(block: () -> Unit) = apply { onDismissRequest = block }

@Suppress("NestedScopeFunctions")
fun MessageBottomSheetUM.infoBlock(init: @MessageBottomSheetDsl MessageBottomSheetUM.InfoBlock.() -> Unit) = apply {
    val element = MessageBottomSheetUM.InfoBlock().apply(init)
    elements = (elements + element).toPersistentList()
}

@Suppress("NestedScopeFunctions")
fun MessageBottomSheetUM.InfoBlock.icon(@DrawableRes res: Int, init: MessageBottomSheetUM.Icon.() -> Unit = {}) =
    apply {
        icon = MessageBottomSheetUM.Icon(res).apply(init)
    }

fun MessageBottomSheetUM.InfoBlock.iconImage(@DrawableRes res: Int) = apply {
    iconImage = MessageBottomSheetUM.IconImage(res)
}

@Suppress("NestedScopeFunctions")
fun MessageBottomSheetUM.InfoBlock.chip(text: TextReference, init: MessageBottomSheetUM.Chip.() -> Unit = {}) = apply {
    chip = MessageBottomSheetUM.Chip(text).apply(init)
}

@Suppress("NestedScopeFunctions")
internal fun MessageBottomSheetUM.button(init: @MessageBottomSheetDsl MessageBottomSheetUM.Button.() -> Unit) = apply {
    val element = MessageBottomSheetUM.Button().apply(init)
    elements = (elements + element).toPersistentList()
}

@Suppress("NestedScopeFunctions")
fun MessageBottomSheetUM.primaryButton(init: @MessageBottomSheetDsl MessageBottomSheetUM.Button.() -> Unit) = apply {
    button { isPrimary = true; apply(init) }
}

@Suppress("NestedScopeFunctions")
fun MessageBottomSheetUM.secondaryButton(init: @MessageBottomSheetDsl MessageBottomSheetUM.Button.() -> Unit) = apply {
    button { isPrimary = false; apply(init) }
}

fun MessageBottomSheetUM.Button.onClick(block: MessageBottomSheetUM.CloseScope.() -> Unit) = apply {
    onClick = block
}

// endregion