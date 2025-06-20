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

fun AnnotatedString.Builder.appendSpace() = append(" ")

fun AnnotatedString.Builder.appendColored(text: String, color: Color) = withStyle(SpanStyle(color = color)) {
    append(text)
}

/**
 * Appends text from a template string to the AnnotatedString.Builder, replacing a placeholder (default "%s")
 * with custom styled content provided by a lambda. The lambda allows you to insert styled or complex content
 * (e.g., colored, bold, or annotated text) at the placeholder position.
 *
 * If the placeholder is not found in the template, the function does nothing.
 *
 * Example usage:
 *   builder.appendWithStyledPlaceholder(
 *       template = "Ensure you %s network address, as errors may result in lost transfers"
 *   ) {
 *       withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
 *           append("Ethereum")
 *       }
 *   }
 *
 * @param template The template string containing the placeholder to be replaced.
 * @param placeholder The placeholder string to be replaced by styled content. Default is "%s".
 * @param styledContent Lambda to build the styled content to insert at the placeholder position.
 */
fun AnnotatedString.Builder.appendWithStyledPlaceholder(
    template: String,
    placeholder: String = "%s",
    styledContent: AnnotatedString.Builder.() -> Unit,
) {
    val index = template.indexOf(placeholder)
    if (index < 0) return
    // Append before placeholder
    if (index > 0) append(template.substring(0, index))
    // Append styled content
    styledContent()
    // Append after placeholder
    val afterIndex = index + placeholder.length
    if (afterIndex < template.length) append(template.substring(afterIndex))
}