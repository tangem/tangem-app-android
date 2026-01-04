package com.tangem.core.ui.extensions

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.res.getPluralStringSafe
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns.THREE_STARS
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
     * @property id                  resource id
     * @property formatArgs          arguments. Impossible to use [kotlinx.collections.immutable.ImmutableList] because
     *                               [Any] is unstable.
     * @property shouldDecapitalize  whether resolved reference should be decapitalized
     */
    data class Res(
        @StringRes val id: Int,
        val formatArgs: WrappedList<Any> = WrappedList(emptyList()),
        val shouldDecapitalize: Boolean = false,
    ) : TextReference

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

    /**
     * Styled string value
     *
     * @property value                string value
     * @property spanStyleReference   text style reference
     * @property onClick              optional click action
     */
    data class StyledStr(
        val value: String,
        val spanStyleReference: SpanStyleReference,
        val onClick: (() -> Unit)? = null,
    ) : TextReference

    /**
     * Styled string resource
     *
     * @property id                  resource id
     * @property formatArgs          arguments. Impossible to use [kotlinx.collections.immutable.ImmutableList] because
     *                               [Any] is unstable.
     * @property spanStyleReference  text style reference
     * @property onClick             optional click action
     */
    data class StyledRes(
        @StringRes val id: Int,
        val formatArgs: WrappedList<Any> = WrappedList(emptyList()),
        val spanStyleReference: SpanStyleReference,
        val onClick: (() -> Unit)? = null,
    ) : TextReference

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
 * @param decapitalize Whether resolved reference should be decapitalized
 * @return [TextReference] representing the string resource with format arguments.
 */
fun resourceReference(
    @StringRes id: Int,
    formatArgs: WrappedList<Any> = WrappedList(emptyList()),
    decapitalize: Boolean = false,
): TextReference {
    return TextReference.Res(id, formatArgs, decapitalize)
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
 * Creates a [TextReference] using a plain string value with optional span style and click action.
 *
 * @param value The plain string value.
 * @param spanStyleReference A [SpanStyleReference] representing the text style to be applied.
 * @param onClick An optional lambda function to be invoked when the text is clicked.
 * @return A [TextReference] representing the styled string with click action.
 */
fun styledStringReference(value: String, spanStyleReference: SpanStyleReference, onClick: (() -> Unit)? = null) =
    TextReference.StyledStr(
        value = value,
        onClick = onClick,
        spanStyleReference = spanStyleReference,
    )

/**
 * Creates a [TextReference] using a string resource ID with optional format arguments, span style, and click action.
 *
 * @param id The resource ID of the string.
 * @param formatArgs A list of format arguments to be applied to the string resource.
 * @param spanStyleReference A [SpanStyleReference] representing the text style to be applied.
 * @param onClick An optional lambda function to be invoked when the text is clicked.
 * @return A [TextReference] representing the styled string with click action.
 */
fun styledResourceReference(
    @StringRes id: Int,
    formatArgs: WrappedList<Any> = WrappedList(emptyList()),
    spanStyleReference: SpanStyleReference,
    onClick: (() -> Unit)? = null,
) = TextReference.StyledRes(
    id = id,
    formatArgs = formatArgs,
    spanStyleReference = spanStyleReference,
    onClick = onClick,
)

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
            val args = formatArgs.map { if (it is TextReference) it.resolveReference() else it }.toTypedArray()

            val resolvedReference = stringResourceSafe(id = id, *args)

            if (shouldDecapitalize) {
                resolvedReference.replaceFirstChar { char -> char.lowercase() }
            } else {
                resolvedReference
            }
        }
        is TextReference.PluralRes -> pluralStringResourceSafe(id, count, *formatArgs.toTypedArray())
        is TextReference.Str -> value
        is TextReference.Annotated -> value.text
        is TextReference.Combined -> {
            buildString {
                refs.forEach {
                    append(it.resolveReference())
                }
            }
        }
        is TextReference.StyledRes -> {
            val args = formatArgs.map { if (it is TextReference) it.resolveReference() else it }.toTypedArray()

            stringResourceSafe(id = id, *args)
        }
        is TextReference.StyledStr -> value
    }
}

/** Resolve [TextReference] as [String] using [resources] (non-composable context) */
fun TextReference.resolveReference(resources: Resources): String {
    return when (this) {
        is TextReference.Res -> {
            val args = formatArgs.map { if (it is TextReference) it.resolveReference(resources) else it }.toTypedArray()

            resources.getStringSafe(id, *args)
        }
        is TextReference.PluralRes -> resources.getPluralStringSafe(id, count, *formatArgs.toTypedArray())
        is TextReference.Str -> value
        is TextReference.Annotated -> value.text
        is TextReference.Combined -> {
            buildString {
                refs.forEach {
                    append(it.resolveReference(resources))
                }
            }
        }
        is TextReference.StyledRes -> {
            val args = formatArgs.map { if (it is TextReference) it.resolveReference(resources) else it }.toTypedArray()

            resources.getStringSafe(id, *args)
        }
        is TextReference.StyledStr -> value
    }
}

/** Resolve [TextReference] as [AnnotatedString] */
@Composable
fun TextReference.resolveAnnotatedReference(): AnnotatedString {
    return when (this) {
        is TextReference.Res -> {
            val args = formatArgs.map { if (it is TextReference) it.resolveReference() else it }.toTypedArray()

            formatAnnotated(stringResourceSafe(id = id, *args))
        }
        is TextReference.PluralRes -> formatAnnotated(
            pluralStringResourceSafe(id, count, *formatArgs.toTypedArray()),
        )
        is TextReference.Str -> formatAnnotated(value)
        is TextReference.Annotated -> value
        is TextReference.Combined -> buildAnnotatedString {
            refs.forEach {
                append(it.resolveAnnotatedReference())
            }
        }
        is TextReference.StyledRes -> {
            val args = formatArgs.map { if (it is TextReference) it.resolveReference() else it }.toTypedArray()
            val text = stringResourceSafe(id = id, *args)

            createStyledText(
                text = text,
                spanStyleReference = spanStyleReference,
                onClick = onClick,
            )
        }
        is TextReference.StyledStr -> createStyledText(
            text = value,
            spanStyleReference = spanStyleReference,
            onClick = onClick,
        )
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
        is TextReference.StyledRes,
        is TextReference.StyledStr,
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

/**
 * Returns the TextReference itself if hide is false, otherwise returns a reference with STARS.
 *
 * @param maskWithStars A boolean flag that determines whether to hide the string.
 * If true, the original TextReference will be replaced by reference with STARS.
 * If false, the original TextReference will be returned.
 *
 * @return The original reference if hide is false, or reference with STARS if hide is true.
 */
fun TextReference.orMaskWithStars(maskWithStars: Boolean): TextReference {
    return if (maskWithStars) stringReference(THREE_STARS) else this
}

@ReadOnlyComposable
@Composable
private fun createStyledText(
    text: String,
    spanStyleReference: SpanStyleReference,
    onClick: (() -> Unit)?,
): AnnotatedString = buildAnnotatedString {
    if (onClick != null) {
        withLink(
            link = LinkAnnotation.Clickable(
                tag = text,
                linkInteractionListener = { onClick() },
            ),
            block = {
                appendStyled(
                    text = text,
                    spanStyle = spanStyleReference(),
                )
            },
        )
    } else {
        appendStyled(
            text = text,
            spanStyle = spanStyleReference(),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TextReference_Preview(@PreviewParameter(TextReferencePreviewProvider::class) params: TextReference) {
    TangemThemePreview {
        val uriHandler = LocalUriHandler.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors.background.primary)
                .padding(4.dp),
        ) {
            Text(
                text = params.resolveAnnotatedReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = styledResourceReference(
                    id = R.string.common_read_more,
                    spanStyleReference = {
                        TangemTheme.typography.body1.copy(TangemTheme.colors.text.accent).toSpanStyle()
                    },
                    onClick = {
                        uriHandler.openUri("https://tangem.com")
                    },
                ).resolveAnnotatedReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = stringReference("To be masked").orMaskWithStars(true).resolveAnnotatedReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
        }
    }
}

private class TextReferencePreviewProvider : PreviewParameterProvider<TextReference> {
    override val values: Sequence<TextReference>
        get() = sequenceOf(
            stringReference("Simple string"),
            resourceReference(R.string.common_tangem),
            pluralReference(
                id = R.plurals.common_days,
                count = 5,
                formatArgs = wrappedList(5),
            ),
            styledStringReference(
                value = "Styled string",
                spanStyleReference = {
                    TangemTheme.typography.subtitle2.copy(TangemTheme.colors.text.accent).toSpanStyle()
                },
            ),
            styledResourceReference(
                id = R.string.common_tangem,
                spanStyleReference = {
                    TangemTheme.typography.caption1.copy(TangemTheme.colors.text.accent).toSpanStyle()
                },
            ),
            combinedReference(
                stringReference("Simple string"),
                resourceReference(R.string.common_tangem),
                pluralReference(
                    id = R.plurals.common_days,
                    count = 5,
                    formatArgs = wrappedList(5),
                ),
                styledStringReference(
                    value = "Styled string",
                    spanStyleReference = {
                        TangemTheme.typography.subtitle2.copy(TangemTheme.colors.text.accent).toSpanStyle()
                    },
                ),
                styledResourceReference(
                    id = R.string.common_tangem,
                    spanStyleReference = {
                        TangemTheme.typography.caption1.copy(TangemTheme.colors.text.warning).toSpanStyle()
                    },
                ),
            ),
        )
}
// endregion