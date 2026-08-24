package com.tangem.features.introduction.impl.ui

internal enum class LegalDocument { TermsOfService, PrivacyPolicy }

internal sealed interface LegalCaptionPart {

    data class Plain(val text: String) : LegalCaptionPart

    data class Link(val text: String, val document: LegalDocument) : LegalCaptionPart
}

/**
 * Titles are spliced in order of appearance rather than in argument order, because a translation is free to
 * reorder the placeholders. A title the translation does not contain verbatim is dropped, so the caption
 * degrades to plain text instead of producing an invalid range.
 */
internal fun splitLegalCaption(caption: String, titles: Map<LegalDocument, String>): List<LegalCaptionPart> {
    val links = titles
        .filterValues(String::isNotEmpty)
        .map { (document, title) -> Triple(caption.indexOf(title), title, document) }
        .filter { (index, _, _) -> index >= 0 }
        .sortedBy { (index, _, _) -> index }

    return buildList {
        var cursor = 0
        links.forEach { (index, title, document) ->
            if (index < cursor) return@forEach
            if (index > cursor) add(LegalCaptionPart.Plain(caption.substring(cursor, index)))
            add(LegalCaptionPart.Link(text = title, document = document))
            cursor = index + title.length
        }
        if (cursor < caption.length) add(LegalCaptionPart.Plain(caption.substring(cursor)))
    }
}