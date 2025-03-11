package com.tangem.common.uri

import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
[REDACTED_AUTHOR]
 */
@RunWith(Parameterized::class)
class ExternalUrlValidatorTest(private val model: Model) {

    @Test
    fun test() {
        val actual = ExternalUrlValidator.isUriTrusted(externalUri = model.url)

        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Model> = listOf(
            Model(url = "https://tangem.com", expected = true),
            Model(url = "https://tange.com", expected = false),
            Model(url = "https://fake.tangem.com", expected = false),
            Model(url = "http://tangem.com", expected = false),
            Model(url = "http://tandem.com", expected = false),
            Model(url = "adawdawdassdw", expected = false),
        )

        data class Model(val url: String, val expected: Boolean)
    }
}