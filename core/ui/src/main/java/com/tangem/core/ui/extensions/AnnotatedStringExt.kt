package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/** Markdown parser */
@Composable
fun rememberMarkdownParser() = remember {
    MarkdownParser(CommonMarkFlavourDescriptor())
}

/**
 * Styling markdown tree recursively
 *
 * @param markdownText original text
 * @param node current processed node
 */
@Composable
fun AnnotatedString.Builder.appendMarkdown(markdownText: String, node: ASTNode): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            node.children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                )
            }
        }
        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium)) {
                node.children
                    .drop(2)
                    .dropLast(2)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                        )
                    }
            }
        }
        else -> {
            append(node.getTextInNode(markdownText).toString())
        }
    }
    return this
}

/**
 * Appends a single space character to the [AnnotatedString.Builder].
 */
fun AnnotatedString.Builder.appendSpace() = append(" ")

/**
 * Appends text with the specified [Color] to the [AnnotatedString.Builder].
 *
 * @param text The text to append.
 * @param color The [Color] to apply to the appended text.
 */
fun AnnotatedString.Builder.appendColored(text: String, color: Color) = withStyle(SpanStyle(color = color)) {
    append(text)
}

/**
 * Appends text with the specified [SpanStyle] to the [AnnotatedString.Builder].
 *
 * @param text The text to append.
 * @param spanStyle The [SpanStyle] to apply to the appended text.
 */
fun AnnotatedString.Builder.appendStyled(text: String, spanStyle: SpanStyle) = withStyle(spanStyle) {
    append(text)
}

/**
 * Appends text from a template string to the AnnotatedString.Builder, replacing positional placeholders
 * (%1$s, %2$s, ...) with custom styled content provided by lambdas.
 *
 * Placeholders are matched by their numeric index (1-based) and replaced in the order they appear
 * in the template. Unmatched placeholders are left as-is.
 *
 * Example usage:
 *   builder.appendWithStyledPlaceholders(
 *       template = "By using swap functionality, you agree with provider's %1$s and %2$s.",
 *      {
 *           withLink(LinkAnnotation.Url("https://example.com/terms")) { append("Terms of Use") }
 *      },
 *      {
 *           withLink(LinkAnnotation.Url("https://example.com/privacy")) { append("Privacy Policy") }
 *      }
 *   )
 *
 * @param template The template string with positional placeholders (%1$s, %2$s, ...).
 * @param styledContents Lambdas indexed by placeholder number (first lambda → %1$s, second → %2$s, ...).
 */
fun AnnotatedString.Builder.appendWithStyledPlaceholders(
    template: String,
    vararg styledContents: AnnotatedString.Builder.() -> Unit,
) {
    data class PlaceholderMatch(val position: Int, val argIndex: Int, val length: Int)
    val matches = styledContents.indices
        .mapNotNull { i ->
            val placeholder = "%${i + 1}${'$'}s"
            val pos = template.indexOf(placeholder)
            if (pos >= 0) PlaceholderMatch(pos, i, placeholder.length) else null
        }
        .sortedBy { it.position }

    var lastIndex = 0
    for (match in matches) {
        if (match.position > lastIndex) {
            append(template.substring(lastIndex, match.position))
        }
        styledContents[match.argIndex](this)
        lastIndex = match.position + match.length
    }
    if (lastIndex < template.length) {
        append(template.substring(lastIndex))
    }
}