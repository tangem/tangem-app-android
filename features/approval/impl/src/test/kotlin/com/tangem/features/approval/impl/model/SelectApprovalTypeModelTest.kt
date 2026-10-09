package com.tangem.features.approval.impl.model

import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SelectApprovalTypeModelTest {

    private val urlOpener: UrlOpener = mockk(relaxUnitFun = true)

    @Test
    fun `GIVEN select approval sheet WHEN learn more clicked THEN permission blog post opened`() = runTest {
        // Arrange
        mockkObject(TangemBlogUrlBuilder)
        try {
            coEvery { TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.GiveRevokePermission) } returns BLOG_URL
            val model = createModel()

            // Act
            model.onLearnMoreClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl(BLOG_URL) }
            model.onDestroy()
        } finally {
            unmockkObject(TangemBlogUrlBuilder)
        }
    }

    private fun createModel() = SelectApprovalTypeModel(
        dispatchers = TestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            SelectApprovalTypeComponent.Params(
                userWalletId = UserWalletId("0011"),
                cryptoCurrencyStatus = mockk(),
                amountFooter = stringReference("subtitle"),
                initialApproveType = ApproveType.LIMITED,
                spenderAddress = "0xSpender",
                callback = mockk(),
            ),
        ),
        urlOpener = urlOpener,
    )

    private companion object {
        const val BLOG_URL = "https://tangem.com/blog/give-revoke-permission"
    }
}