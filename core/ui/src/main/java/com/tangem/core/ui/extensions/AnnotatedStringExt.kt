package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
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
 * @param boldColor color of the bold (`**...**`) sections
 * @param linkColor color of the inline link (`[text](url)`) sections. Unspecified keeps the surrounding text color
 * @param onLinkClick called with the link destination on click. Inline links are not clickable if it's null
 */
@Composable
fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    boldColor: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified,
    onLinkClick: ((url: String) -> Unit)? = null,
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            appendMarkdownNodes(
                markdownText = markdownText,
                nodes = node.children,
                boldColor = boldColor,
                linkColor = linkColor,
                onLinkClick = onLinkClick,
            )
        }
        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = boldColor)) {
                appendMarkdownNodes(
                    markdownText = markdownText,
                    nodes = node.children.drop(2).dropLast(2),
                    boldColor = boldColor,
                    linkColor = linkColor,
                    onLinkClick = onLinkClick,
                )
            }
        }
        MarkdownElementTypes.INLINE_LINK -> {
            appendMarkdownLink(
                markdownText = markdownText,
                node = node,
                boldColor = boldColor,
                linkColor = linkColor,
                onLinkClick = onLinkClick,
            )
        }
        else -> {
            append(node.getTextInNode(markdownText).toString())
        }
    }
    return this
}

@Composable
private fun AnnotatedString.Builder.appendMarkdownNodes(
    markdownText: String,
    nodes: List<ASTNode>,
    boldColor: Color,
    linkColor: Color,
    onLinkClick: ((url: String) -> Unit)?,
): AnnotatedString.Builder {
    nodes.forEach { node ->
        appendMarkdown(
            markdownText = markdownText,
            node = node,
            boldColor = boldColor,
            linkColor = linkColor,
            onLinkClick = onLinkClick,
        )
    }
    return this
}

@Composable
private fun AnnotatedString.Builder.appendMarkdownLink(
    markdownText: String,
    node: ASTNode,
    boldColor: Color,
    linkColor: Color,
    onLinkClick: ((url: String) -> Unit)?,
): AnnotatedString.Builder {
    val url = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(markdownText)?.toString()
    val linkTextNodes = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)
        ?.children
        ?.drop(1)
        ?.dropLast(1)

    when {
        url == null || linkTextNodes == null -> append(node.getTextInNode(markdownText).toString())
        onLinkClick == null -> appendMarkdownNodes(
            markdownText = markdownText,
            nodes = linkTextNodes,
            boldColor = boldColor,
            linkColor = linkColor,
            onLinkClick = null,
        )
        else -> withLink(
            link = LinkAnnotation.Clickable(
                tag = url,
                styles = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                ),
                linkInteractionListener = { onLinkClick(url) },
            ),
        ) {
            appendMarkdownNodes(
                markdownText = markdownText,
                nodes = linkTextNodes,
                boldColor = boldColor,
                linkColor = linkColor,
                onLinkClick = onLinkClick,
            )
        }
    }
    return this
}

/**
 * Parses [rawString] as markdown and returns an [AnnotatedString] where bold (`**...**`) sections
 * are styled with [boldColor].
 */
@Composable
fun formatAnnotatedWithBoldColor(rawString: String, boldColor: Color): AnnotatedString {
    val markdownParser = rememberMarkdownParser()
    val parsedTree = markdownParser.parse(MarkdownElementTypes.MARKDOWN_FILE, rawString, true)
    return buildAnnotatedString {
        appendMarkdown(markdownText = rawString, node = parsedTree, boldColor = boldColor)
    }
}

/**
 * Parses [rawString] as markdown and returns an [AnnotatedString] where inline links (`[text](url)`) are
 * underlined and clickable, and bold (`**...**`) sections are styled with [boldColor].
 *
 * Links keep the color of the text they sit in unless [linkColor] is specified.
 *
 * @param onLinkClick called with the link destination when a link is clicked
 */
@Composable
fun formatAnnotatedWithLinks(
    rawString: String,
    boldColor: Color = Color.Unspecified,
    linkColor: Color = Color.Unspecified,
    onLinkClick: (url: String) -> Unit,
): AnnotatedString {
    val markdownParser = rememberMarkdownParser()
    val parsedTree = markdownParser.parse(MarkdownElementTypes.MARKDOWN_FILE, rawString, true)
    return buildAnnotatedString {
        appendMarkdown(
            markdownText = rawString,
            node = parsedTree,
            boldColor = boldColor,
            linkColor = linkColor,
            onLinkClick = onLinkClick,
        )
    }
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