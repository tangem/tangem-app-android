package com.tangem.core.ui.components.bottomsheets.message

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
data class MessageBottomSheetUMV2(
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
annotation class MessageBottomSheetV2Dsl

// region: DSL

fun messageBottomSheetUM(init: @MessageBottomSheetV2Dsl MessageBottomSheetUMV2.() -> Unit) =
    MessageBottomSheetUMV2().apply(init)

fun MessageBottomSheetUMV2.onDismiss(block: () -> Unit) = MessageBottomSheetUMV2().apply { onDismissRequest = block }

fun MessageBottomSheetUMV2.infoBlock(init: @MessageBottomSheetV2Dsl MessageBottomSheetUMV2.InfoBlock.() -> Unit) =
    apply {
        val element = MessageBottomSheetUMV2.InfoBlock().apply(init)
        elements = (elements + element).toPersistentList()
    }

fun MessageBottomSheetUMV2.InfoBlock.icon(@DrawableRes res: Int, init: MessageBottomSheetUMV2.Icon.() -> Unit = {}) =
    apply {
        icon = MessageBottomSheetUMV2.Icon(res).apply(init)
    }

fun MessageBottomSheetUMV2.InfoBlock.chip(text: TextReference, init: MessageBottomSheetUMV2.Chip.() -> Unit = {}) =
    apply {
        chip = MessageBottomSheetUMV2.Chip(text).apply(init)
    }

internal fun MessageBottomSheetUMV2.button(init: @MessageBottomSheetV2Dsl MessageBottomSheetUMV2.Button.() -> Unit) =
    apply {
        val element = MessageBottomSheetUMV2.Button().apply(init)
        elements = (elements + element).toPersistentList()
    }

fun MessageBottomSheetUMV2.primaryButton(init: @MessageBottomSheetV2Dsl MessageBottomSheetUMV2.Button.() -> Unit) =
    apply {
        button { isPrimary = true; apply(init) }
    }

fun MessageBottomSheetUMV2.secondaryButton(init: @MessageBottomSheetV2Dsl MessageBottomSheetUMV2.Button.() -> Unit) =
    apply {
        button { isPrimary = false; apply(init) }
    }

fun MessageBottomSheetUMV2.Button.onClick(block: MessageBottomSheetUMV2.CloseScope.() -> Unit) = apply {
    onClick = block
}

// endregion