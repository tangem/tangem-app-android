package com.tangem.core.ui.extensions

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.buildAnnotatedString
import org.intellij.markdown.MarkdownElementTypes
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
     * Annotated text
     *
     * @property value annotated string
     */
    data class Annotated(val value: AnnotatedString) : TextReference

    /**
     * Combined reference. It concatenates all [refs].
     *
     * @see [TextReference.plus] method
     */
    data class Combined(val refs: WrappedList<TextReference>) : TextReference

    companion object {

        /** Empty string as [TextReference] */
        val EMPTY: TextReference by lazy(mode = LazyThreadSafetyMode.NONE) { Str(value = "") }
    }
}

/**
 * Creates a [TextReference] using a string resource ID with optional format arguments.
 *
 * @param id The resource ID of the string.
 * @param formatArgs A list of format arguments to be applied to the string resource.
 * @return A [TextReference] representing the string resource with format arguments.
 */
fun resourceReference(@StringRes id: Int, formatArgs: WrappedList<Any> = WrappedList(emptyList())): TextReference {
    return TextReference.Res(id, formatArgs)
}

/**
 * Creates a [TextReference] using a plain string value.
 *
 * @param value The plain string value.
 * @return A [TextReference] representing the provided string value.
 */
fun stringReference(value: String): TextReference {
    return TextReference.Str(value)
}

/**
 * Creates a [TextReference] using an annotated string value.
 *
 * @param value The annotated string value.
 * @return A [TextReference] representing the provided annotated string value.
 */
fun annotatedReference(value: AnnotatedString): TextReference {
    return TextReference.Annotated(value)
}

/**
 * Creates a [TextReference] using an annotated string value.
 *
 * @param onAnnotated The annotated string builder.
 * @return A [TextReference] representing the provided annotated string value.
 */
@Composable
inline fun annotatedReference(onAnnotated: Builder.() -> Unit): TextReference {
    return TextReference.Annotated(buildAnnotatedString { onAnnotated() })
}

/**
 * Creates a [TextReference] using a plural string resource ID with count and optional format arguments.
 *
 * @param id The resource ID of the plural string.
 * @param count The count value to determine the plural form.
 * @param formatArgs A list of format arguments to be applied to the plural string resource.
 * @return A [TextReference] representing the plural string resource with count and format arguments.
 */
fun pluralReference(
    @PluralsRes id: Int,
    count: Int,
    formatArgs: WrappedList<Any> = WrappedList(emptyList()),
): TextReference {
    return TextReference.PluralRes(id, count, formatArgs)
}

/**
 * Combines multiple [TextReference] instances into a single [TextReference].
 *
 * @param refs A list of [TextReference] instances to be combined.
 * @return A [TextReference] representing the combined text references.
 */
fun combinedReference(refs: WrappedList<TextReference>): TextReference {
    return TextReference.Combined(refs)
}

/**
 * Combines multiple [TextReference] instances into a single [TextReference].
 *
 * @param refs Vararg of [TextReference] instances to be combined.
 * @return A [TextReference] representing the combined text references.
 */
fun combinedReference(vararg refs: TextReference): TextReference {
    return TextReference.Combined(WrappedList(listOf(*refs)))
}

/** Resolve [TextReference] as [String] */
@Composable
@ReadOnlyComposable
fun TextReference.resolveReference(): String {
    return when (this) {
        is TextReference.Res -> {
            val args = formatArgs
                .map { if (it is TextReference) it.resolveReference() else it }
                .toTypedArray()

            stringResource(id = id, *args)
        }
        is TextReference.PluralRes -> pluralStringResource(id, count, *formatArgs.toTypedArray())
        is TextReference.Str -> value
        is TextReference.Annotated -> value.text
        is TextReference.Combined -> {
            buildString {
                refs.forEach {
                    append(it.resolveReference())
                }
            }
        }
    }
}

/** Resolve [TextReference] as [String] using [resources] (non-composable context) */
fun TextReference.resolveReference(resources: Resources): String {
    return when (this) {
        is TextReference.Res -> {
            val args = formatArgs
                .map { if (it is TextReference) it.resolveReference(resources) else it }
                .toTypedArray()

            resources.getString(id, *args)
        }
        is TextReference.PluralRes -> resources.getQuantityString(id, count, *formatArgs.toTypedArray())
        is TextReference.Str -> value
        is TextReference.Annotated -> value.text
        is TextReference.Combined -> {
            buildString {
                refs.forEach {
                    append(it.resolveReference(resources))
                }
            }
        }
    }
}

/** Resolve [TextReference] as [AnnotatedString] */
@Composable
fun TextReference.resolveAnnotatedReference(): AnnotatedString {
    return when (this) {
        is TextReference.Res -> {
            val args = formatArgs
                .map { if (it is TextReference) it.resolveReference() else it }
                .toTypedArray()

            formatAnnotated(stringResource(id = id, *args))
        }
        is TextReference.PluralRes -> formatAnnotated(
            pluralStringResource(id, count, *formatArgs.toTypedArray()),
        )
        is TextReference.Str -> formatAnnotated(value)
        is TextReference.Annotated -> value
        is TextReference.Combined -> buildAnnotatedString {
            refs.forEach {
                append(it.resolveAnnotatedReference())
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
        is TextReference.Annotated,
        -> TextReference.Combined(refs = wrappedList(this, ref))
    }
}

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun TextReference?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this == TextReference.EMPTY
}

@Composable
private fun formatAnnotated(rawString: String): AnnotatedString {
    val markdownDescriptor = rememberMarkdownParser()
    val parsedTree = markdownDescriptor.parse(MarkdownElementTypes.MARKDOWN_FILE, rawString, true)

    return buildAnnotatedString {
        appendMarkdown(markdownText = rawString, node = parsedTree)
    }
}
