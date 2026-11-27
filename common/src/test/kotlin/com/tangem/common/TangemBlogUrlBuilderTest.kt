package com.tangem.common

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemBlogUrlBuilderTest {

    private val appInstanceIdProvider: AppInstanceIdProvider = mockk {
        coEvery { getAppInstanceId() } returns null
    }

    private val builder = TangemBlogUrlBuilder(TangemSiteUrlBuilder(appInstanceIdProvider))

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN post WHEN build THEN returns its page url with utm tags`(model: BuildModel) = runTest {
        // Act
        val actual = builder.build(model.post)

        // Assert
        assertThat(actual).startsWith("${model.expectedPage}?utm_source=tangem-app&utm_medium=app")
    }

    data class BuildModel(val post: TangemBlogUrlBuilder.Post, val expectedPage: String)

    private fun provideTestModels() = listOf(
        BuildModel(
            post = TangemBlogUrlBuilder.Post.HowYieldModeWorks,
            expectedPage = "https://tangem.com/embed/blog/post/yield-mode",
        ),
        BuildModel(
            post = TangemBlogUrlBuilder.Post.LendingOnAave,
            expectedPage = "https://tangem.com/en/help-center/vaults/lending-on-aave",
        ),
    )
}