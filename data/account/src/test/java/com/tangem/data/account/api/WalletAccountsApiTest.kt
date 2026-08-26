package com.tangem.data.account.api

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletArchivedAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsV1Request
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletAccountsApiTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()

    private val walletAccountsApi = WalletAccountsApi(
        tangemTechApi = tangemTechApi,
        jointAccountFeatureToggles = jointAccountFeatureToggles,
    )

    private val walletId = "011"
    private val eTag = "etag"

    private val personalRow = SaveWalletAccountsResponse.AccountDTO(
        id = "A".repeat(64),
        name = null,
        derivationIndex = 0,
        icon = "Wallet",
        iconColor = "DullLavender",
        type = WalletAccountDTO.Type.CRYPTO.value,
    )
    private val jointRow = personalRow.copy(
        id = "B".repeat(64),
        name = "Family",
        type = WalletAccountDTO.Type.JOINT.value,
    )
    private val body = SaveWalletAccountsResponse(accounts = listOf(personalRow, jointRow))

    @AfterEach
    fun tearDown() {
        clearMocks(tangemTechApi, jointAccountFeatureToggles)
    }

    @Test
    fun `GIVEN toggle is off WHEN eTagKey THEN v1 validator is used`() {
        // Arrange
        toggle(enabled = false)

        // Act
        val actual = walletAccountsApi.eTagKey

        // Assert
        assertThat(actual).isEqualTo(ETagsStore.Key.WalletAccountsV1)
    }

    @Test
    fun `GIVEN toggle is on WHEN eTagKey THEN v2 validator is used`() {
        // Arrange
        toggle(enabled = true)

        // Act
        val actual = walletAccountsApi.eTagKey

        // Assert
        assertThat(actual).isEqualTo(ETagsStore.Key.WalletAccountsV2)
    }

    @Test
    fun `GIVEN toggle is off WHEN getAccounts THEN v1 is requested`() = runTest {
        // Arrange
        toggle(enabled = false)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery { tangemTechApi.getWalletAccountsV1(walletId = walletId, eTag = eTag) } returns response

        // Act
        val actual = walletAccountsApi.getAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
        coVerify(exactly = 0) { tangemTechApi.getWalletAccountsV2(walletId = any(), eTag = any()) }
    }

    @Test
    fun `GIVEN toggle is on WHEN getAccounts THEN v2 is requested`() = runTest {
        // Arrange
        toggle(enabled = true)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery { tangemTechApi.getWalletAccountsV2(walletId = walletId, eTag = eTag) } returns response

        // Act
        val actual = walletAccountsApi.getAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
        coVerify(exactly = 0) { tangemTechApi.getWalletAccountsV1(walletId = any(), eTag = any()) }
    }

    @Test
    fun `GIVEN toggle is off WHEN saveAccounts THEN v1 receives the body without types and joint rows`() = runTest {
        // Arrange
        toggle(enabled = false)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        val expectedBody = SaveWalletAccountsV1Request(
            accounts = listOf(
                SaveWalletAccountsV1Request.AccountDTO(
                    id = personalRow.id,
                    name = null,
                    derivationIndex = 0,
                    icon = "Wallet",
                    iconColor = "DullLavender",
                ),
            ),
        )
        coEvery { tangemTechApi.saveWalletAccountsV1(walletId, eTag, expectedBody) } returns response

        // Act
        val actual = walletAccountsApi.saveAccounts(walletId = walletId, eTag = eTag, body = body)

        // Assert
        assertThat(actual).isEqualTo(response)
        coVerify(exactly = 1) { tangemTechApi.saveWalletAccountsV1(walletId, eTag, expectedBody) }
        coVerify(exactly = 0) { tangemTechApi.saveWalletAccountsV2(walletId = any(), eTag = any(), body = any()) }
    }

    @Test
    fun `GIVEN toggle is on WHEN saveAccounts THEN v2 receives the body as is`() = runTest {
        // Arrange
        toggle(enabled = true)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery { tangemTechApi.saveWalletAccountsV2(walletId, eTag, body) } returns response

        // Act
        val actual = walletAccountsApi.saveAccounts(walletId = walletId, eTag = eTag, body = body)

        // Assert
        assertThat(actual).isEqualTo(response)
        coVerify(exactly = 0) { tangemTechApi.saveWalletAccountsV1(walletId = any(), eTag = any(), body = any()) }
    }

    @Test
    fun `GIVEN toggle is off WHEN getArchivedAccounts THEN v1 is requested`() = runTest {
        // Arrange
        toggle(enabled = false)
        val response = ApiResponse.Success(data = mockk<GetWalletArchivedAccountsResponse>())
        coEvery { tangemTechApi.getWalletArchivedAccountsV1(walletId = walletId, eTag = eTag) } returns response

        // Act
        val actual = walletAccountsApi.getArchivedAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
        coVerify(exactly = 0) { tangemTechApi.getWalletArchivedAccountsV2(walletId = any(), eTag = any()) }
    }

    private fun toggle(enabled: Boolean) {
        every { jointAccountFeatureToggles.isJointAccountCreationEnabled } returns enabled
    }
}