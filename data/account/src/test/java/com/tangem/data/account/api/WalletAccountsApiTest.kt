package com.tangem.data.account.api

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.*
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import io.mockk.*
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
    fun `GIVEN toggle is off WHEN getAccounts THEN v1 is requested`() = runTest {
        // Arrange
        toggle(enabled = false)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery { tangemTechApi.getWalletAccounts(version = "v1", walletId = walletId, eTag = eTag) } returns response

        // Act
        val actual = walletAccountsApi.getAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
    }

    @Test
    fun `GIVEN toggle is on WHEN getAccounts THEN api v2 is requested`() = runTest {
        // Arrange
        toggle(enabled = true)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery {
            tangemTechApi.getWalletAccounts(version = "api/v2", walletId = walletId, eTag = eTag)
        } returns response

        // Act
        val actual = walletAccountsApi.getAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
    }

    @Test
    fun `GIVEN toggle is off WHEN saveAccounts THEN v1 receives the same rows without types`() = runTest {
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
                SaveWalletAccountsV1Request.AccountDTO(
                    id = jointRow.id,
                    name = "Family",
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
        coVerify(exactly = 0) {
            tangemTechApi.saveWalletAccounts(version = any(), walletId = any(), eTag = any(), body = any())
        }
    }

    @Test
    fun `GIVEN toggle is on WHEN saveAccounts THEN api v2 receives the body as is`() = runTest {
        // Arrange
        toggle(enabled = true)
        val response = ApiResponse.Success(data = mockk<GetWalletAccountsResponse>())
        coEvery { tangemTechApi.saveWalletAccounts("api/v2", walletId, eTag, body) } returns response

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
        coEvery {
            tangemTechApi.getWalletArchivedAccounts(version = "v1", walletId = walletId, eTag = eTag)
        } returns response

        // Act
        val actual = walletAccountsApi.getArchivedAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
    }

    @Test
    fun `GIVEN toggle is on WHEN getArchivedAccounts THEN api v2 is requested`() = runTest {
        // Arrange
        toggle(enabled = true)
        val response = ApiResponse.Success(data = mockk<GetWalletArchivedAccountsResponse>())
        coEvery {
            tangemTechApi.getWalletArchivedAccounts(version = "api/v2", walletId = walletId, eTag = eTag)
        } returns response

        // Act
        val actual = walletAccountsApi.getArchivedAccounts(walletId = walletId, eTag = eTag)

        // Assert
        assertThat(actual).isEqualTo(response)
    }

    private fun toggle(enabled: Boolean) {
        every { jointAccountFeatureToggles.isJointAccountCreationEnabled } returns enabled
    }
}