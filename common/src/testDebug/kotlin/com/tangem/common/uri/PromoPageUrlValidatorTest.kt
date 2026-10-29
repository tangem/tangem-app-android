package com.tangem.common.uri

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoPageUrlValidatorTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN url WHEN isAllowed THEN only https allowlisted hosts pass`(model: Model) {
        // Act
        val actual = PromoPageUrlValidator.isAllowed(uri = createUri(model.url))

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun createUri(url: String): Uri {
        val uri = URI(url)

        return mockk {
            every { scheme } returns uri.scheme
            every { userInfo } returns uri.userInfo
            every { host } returns uri.host
        }
    }

    data class Model(val url: String, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(url = "https://promo.tangem.com/prize-draw-status/?success", expected = true),
        Model(url = "https://tangem.com", expected = true),
        Model(url = "https://www.tangem.com", expected = true),
        Model(url = "https://buy.tangem.com/?promocode=NEWINAPP", expected = true),
        Model(url = "https://app.tangem.com", expected = true),
        Model(url = "https://tangem.surveysparrow.com/s/tangem-pay/tt-F8XXH", expected = true),
        Model(url = "https://feedback.tangem.com", expected = true),
        Model(url = "https://PROMO.Tangem.com/prize-draw-status/", expected = true),
        Model(url = "http://promo.tangem.com", expected = false),
        Model(url = "https://[REDACTED_EMAIL]", expected = false),
        Model(url = "https://fake.promo.tangem.com", expected = false),
        Model(url = "https://express.tangem.com", expected = false),
        Model(url = "https://surveysparrow.com", expected = false),
        Model(url = "https://tangem.com.evil.com", expected = false),
        Model(url = "https://promo.tangem.com.evil.com", expected = false),
        Model(url = "https://eviltangem.com", expected = false),
        Model(url = "promo.tangem.com", expected = false),
    )
}