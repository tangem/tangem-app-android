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