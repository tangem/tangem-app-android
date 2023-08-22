package com.tangem.core.ui.extensions

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Utility class for creating text as [String] or [StringRes].
 * It necessary to use [Immutable] annotation because all sealed interface has runtime stability.
 * All subclasses are stable.
 */
@Immutable
sealed interface TextReference {

    /**
     * Text resource id
     *
     * @property id         resource id
     * @property formatArgs arguments. Impossible to use [kotlinx.collections.immutable.ImmutableList] because [Any] is
     *                      unstable.
     */
    data class Res(@StringRes val id: Int, val formatArgs: WrappedList<Any> = WrappedList(emptyList())) : TextReference

    /**
     * Plural resource id
     *
     * @property id         resource id
     * @property count      count
     * @property formatArgs arguments. Impossible to use [kotlinx.collections.immutable.ImmutableList] because [Any] is
     *                      unstable.
     */
    data class PluralRes(@PluralsRes val id: Int, val count: Int, val formatArgs: WrappedList<Any>) : TextReference

    /**
     * Text string
     *
     * @property value value
     */
    data class Str(val value: String) : TextReference

    /**
     * Combined reference. It concatenates all [refs].
     *
     * @see [TextReference.plus] method
     */
    data class Combined(val refs: WrappedList<TextReference>) : TextReference
}

/** Resolve [TextReference] as [String] */
@Composable
@ReadOnlyComposable
fun TextReference.resolveReference(): String {
    return when (this) {
        is TextReference.Res -> stringResource(id, *formatArgs.toTypedArray())
        is TextReference.PluralRes -> pluralStringResource(id, count, *formatArgs.toTypedArray())
        is TextReference.Str -> value
        is TextReference.Combined -> {
            buildString {
                refs.forEach {
                    append(it.resolveReference())
                }
            }
        }
    }
}

/** Concatenate [this] reference with [ref] */
operator fun TextReference.plus(ref: TextReference): TextReference {
    return when (this) {
        is TextReference.Combined -> copy(refs = (refs.data + ref).toWrappedList())
        is TextReference.PluralRes,
        is TextReference.Res,
        is TextReference.Str,
        -> TextReference.Combined(refs = wrappedList(this, ref))
    }
}