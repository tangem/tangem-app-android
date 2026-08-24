package com.tangem.features.introduction.impl.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LegalCaptionPartsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun splitLegalCaption(model: SplitModel) {
        // Act
        val actual = splitLegalCaption(caption = model.caption, titles = model.titles)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
    }

    internal data class SplitModel(
        val caption: String,
        val titles: Map<LegalDocument, String>,
        val expected: List<LegalCaptionPart>,
    )

    private fun provideTestModels() = listOf(
        SplitModel(
            caption = "By continuing, you agree with Terms of service and Privacy Policy",
            titles = BOTH_TITLES,
            expected = listOf(
                LegalCaptionPart.Plain("By continuing, you agree with "),
                LegalCaptionPart.Link("Terms of service", LegalDocument.TermsOfService),
                LegalCaptionPart.Plain(" and "),
                LegalCaptionPart.Link("Privacy Policy", LegalDocument.PrivacyPolicy),
            ),
        ),
        // A translation is free to put the privacy policy first; the links must follow the text, not the arguments.
        SplitModel(
            caption = "Privacy Policy and Terms of service is what you accept",
            titles = BOTH_TITLES,
            expected = listOf(
                LegalCaptionPart.Link("Privacy Policy", LegalDocument.PrivacyPolicy),
                LegalCaptionPart.Plain(" and "),
                LegalCaptionPart.Link("Terms of service", LegalDocument.TermsOfService),
                LegalCaptionPart.Plain(" is what you accept"),
            ),
        ),
        // A translation that does not contain a title verbatim degrades to plain text rather than breaking.
        SplitModel(
            caption = "You agree with Privacy Policy",
            titles = BOTH_TITLES,
            expected = listOf(
                LegalCaptionPart.Plain("You agree with "),
                LegalCaptionPart.Link("Privacy Policy", LegalDocument.PrivacyPolicy),
            ),
        ),
        SplitModel(
            caption = "You agree with our legal documents",
            titles = BOTH_TITLES,
            expected = listOf(LegalCaptionPart.Plain("You agree with our legal documents")),
        ),
        // An empty title matches at index 0 in every string, so it must be ignored instead of splitting the text.
        SplitModel(
            caption = "You agree with Privacy Policy",
            titles = mapOf(LegalDocument.TermsOfService to "", LegalDocument.PrivacyPolicy to "Privacy Policy"),
            expected = listOf(
                LegalCaptionPart.Plain("You agree with "),
                LegalCaptionPart.Link("Privacy Policy", LegalDocument.PrivacyPolicy),
            ),
        ),
        SplitModel(
            caption = "Terms of service",
            titles = BOTH_TITLES,
            expected = listOf(LegalCaptionPart.Link("Terms of service", LegalDocument.TermsOfService)),
        ),
        SplitModel(
            caption = "",
            titles = BOTH_TITLES,
            expected = emptyList(),
        ),
    )

    private companion object {
        val BOTH_TITLES = mapOf(
            LegalDocument.TermsOfService to "Terms of service",
            LegalDocument.PrivacyPolicy to "Privacy Policy",
        )
    }
}