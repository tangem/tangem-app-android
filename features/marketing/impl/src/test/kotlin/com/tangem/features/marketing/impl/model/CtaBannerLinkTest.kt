package com.tangem.features.marketing.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CtaBannerLinkTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN campaign link WHEN resolved THEN classified by how Android opens it`(model: ResolveModel) {
        // Act
        val actual = resolveCtaBannerLink(model.link)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(val link: String?, val expected: CtaBannerLink)

    private fun provideTestModels() = listOf(
        ResolveModel(link = "tangem://support", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://onramp?type=concierge_waitlist", expected = CtaBannerLink.CONCIERGE_WAITLIST),
        ResolveModel(
            link = "tangem://onramp?foo=bar&type=concierge_waitlist",
            expected = CtaBannerLink.CONCIERGE_WAITLIST,
        ),
        ResolveModel(link = "tangem://onramp?type=concierge%5Fwaitlist", expected = CtaBannerLink.CONCIERGE_WAITLIST),
        ResolveModel(link = "https://partner.example/otc?ref=tangem", expected = CtaBannerLink.PARTNER_PAGE),
        ResolveModel(link = "tangem://onramp", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://onramp?type=other&type=concierge_waitlist", expected = CtaBannerLink.APP_LINK),
        ResolveModel(
            link = "tangem://onramp?type=concierge_waitlist&type=other",
            expected = CtaBannerLink.CONCIERGE_WAITLIST,
        ),
        ResolveModel(link = "tangem://onramp?type=concierge_waitlist%26x=1", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://onramp?type%3Dconcierge_waitlist", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://onramp?tx_id=1&type=concierge_waitlist", expected = CtaBannerLink.APP_LINK),
        ResolveModel(
            link = "tangem://onramp?tx_id=&type=concierge_waitlist",
            expected = CtaBannerLink.CONCIERGE_WAITLIST,
        ),
        ResolveModel(
            link = "tangem://onramp?result=success&type=concierge_waitlist",
            expected = CtaBannerLink.APP_LINK,
        ),
        ResolveModel(
            link = "tangem://onramp?result=other&type=concierge_waitlist",
            expected = CtaBannerLink.CONCIERGE_WAITLIST,
        ),
        ResolveModel(link = "tangem://onramp?type=concierge_waitlist_v2", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://swap", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://buy?currency=BTC", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://token_chart?token_id=bitcoin", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://joint_account", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://pay-app-main", expected = CtaBannerLink.APP_LINK),
        ResolveModel(link = "tangem://", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "tangem:swap", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "TANGEM://support", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = " tangem://support", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "http://partner.example", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "https://[REDACTED_EMAIL]", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "https://evil.example\\@tangem.com", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "https:///no-host", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "wc://session", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "not a link", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = "", expected = CtaBannerLink.UNSUPPORTED),
        ResolveModel(link = null, expected = CtaBannerLink.UNSUPPORTED),
    )
}