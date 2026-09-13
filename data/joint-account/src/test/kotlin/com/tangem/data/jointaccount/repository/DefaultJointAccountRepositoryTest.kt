package com.tangem.data.jointaccount.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.jointaccount.FIXTURE_CRYPTO_ACCOUNT_ID
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.datasource.api.jointaccount.JointAccountApi
import com.tangem.datasource.api.jointaccount.models.CreateJointAccountRequest
import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountParticipant
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointAccountRepositoryTest {

    private val jointAccountApi: JointAccountApi = mockk()
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val invitesStore: JointAccountInvitesStore = mockk(relaxUnitFun = true)

    private val repository = DefaultJointAccountRepository(
        jointAccountApi = jointAccountApi,
        walletAccountsFetcher = walletAccountsFetcher,
        invitesStore = invitesStore,
        converter = JointAccountDtoConverter(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountApi, walletAccountsFetcher, invitesStore)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun getFreeOwnerDerivationIndex(model: IndexModel) = runTest {
        // Arrange
        coEvery { walletAccountsFetcher.fetch(WALLET_ID) } returns createAccountsResponse(model.counter)

        // Act
        val actual = repository.getFreeOwnerDerivationIndex(userWalletId = WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        // Always from a fresh fetch: a cached counter yields an occupied index and a 409 after the tap
        coVerify(exactly = 1) { walletAccountsFetcher.fetch(WALLET_ID) }
    }

    private fun provideTestModels() = listOf(
        IndexModel(counter = null, expected = 0), // the backend does not report the counter yet
        IndexModel(counter = 0, expected = 0),
        IndexModel(counter = 3, expected = 3),
        IndexModel(counter = 19, expected = 19),
    )

    @Test
    fun `GIVEN payload WHEN create THEN it is sent verbatim and the account is returned`() = runTest {
        // Arrange
        val bodySlot = slot<CreateJointAccountRequest>()
        coEvery {
            jointAccountApi.createJointAccount(walletId = WALLET_ID.stringValue, body = capture(bodySlot))
        } returns ApiResponse.Success(createJointAccountDto())

        // Act
        val actual = repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)

        // Assert
        val expected = JointAccountCreationResult.Created(account = createJointAccount(source = StatusSource.ACTUAL))
        assertThat(actual).isEqualTo(expected)
        // The signature covers the canonical form of exactly these values — trimming any of them breaks it
        val body = bodySlot.captured
        assertThat(body.signature).isEqualTo(SIGNATURE)
        assertThat(body.payload.config.name).isEqualTo(" Family ")
        assertThat(body.payload.config.membersCount).isEqualTo(3)
        assertThat(body.payload.config.threshold).isEqualTo(2)
        assertThat(body.payload.creator.name).isEqualTo(" Alice ")
        assertThat(body.payload.creator.walletId).isEqualTo(WALLET_ID.stringValue)
        assertThat(body.payload.creator.address).isEqualTo(OWNER_ADDRESS)
        assertThat(body.payload.creator.derivation).isEqualTo(7)
    }

    @Test
    fun `GIVEN response with invites WHEN create THEN invites persisted for the created account`() = runTest {
        // Arrange
        val dto = createJointAccountDto(
            invites = listOf(JointAccountDto.Invite(id = INVITE_A), JointAccountDto.Invite(id = INVITE_B)),
        )
        coEvery { jointAccountApi.createJointAccount(any(), any()) } returns ApiResponse.Success(dto)

        // Act
        repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)

        // Assert
        coVerify(exactly = 1) {
            invitesStore.store(
                userWalletId = WALLET_ID,
                cryptoAccountId = FIXTURE_CRYPTO_ACCOUNT_ID,
                invites = listOf(INVITE_A, INVITE_B),
            )
        }
    }

    @Test
    fun `GIVEN invites store fails WHEN create THEN the created account is still returned`() = runTest {
        // Arrange
        // The account already exists on the backend — a storage failure must not push the user into a retry
        // that can only end in a 409
        coEvery {
            jointAccountApi.createJointAccount(any(), any())
        } returns ApiResponse.Success(createJointAccountDto())
        coEvery { invitesStore.store(any(), any(), any()) } throws IllegalStateException("keystore unavailable")

        // Act
        val actual = repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)

        // Assert
        val expected = JointAccountCreationResult.Created(account = createJointAccount(source = StatusSource.ACTUAL))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN conflict WHEN create THEN result says the creator is already registered`() = runTest {
        // Arrange
        // 409 is a recoverable branch, not a failure: the caller resolves the account by re-reading the list
        coEvery { jointAccountApi.createJointAccount(any(), any()) } returns errorResponse(
            code = ApiResponseError.HttpException.Code.CONFLICT,
        )

        // Act
        val actual = repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(JointAccountCreationResult.CreatorAlreadyRegistered)
        coVerify(exactly = 0) { invitesStore.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN server error WHEN create THEN it is thrown`() = runTest {
        // Arrange
        val response = errorResponse(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)
        coEvery { jointAccountApi.createJointAccount(any(), any()) } returns response

        // Act
        val actual = runCatching {
            repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isEqualTo((response as ApiResponse.Error).cause)
    }

    private fun createPayload() = JointAccountCreationPayload(
        config = JointAccountConfig(
            name = " Family ",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 3,
            threshold = 2,
        ),
        creator = JointAccountParticipant(
            walletId = WALLET_ID.stringValue,
            name = " Alice ",
            address = OWNER_ADDRESS,
            derivation = 7,
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun errorResponse(code: ApiResponseError.HttpException.Code): ApiResponse<JointAccountDto> {
        val cause = ApiResponseError.HttpException(code = code, message = null, errorBody = null)

        return ApiResponse.Error(cause = cause) as ApiResponse<JointAccountDto>
    }

    private fun createAccountsResponse(totalJointAccounts: Int?): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = null,
                sort = null,
                totalAccounts = 1,
                totalArchivedAccounts = 0,
                totalJointAccounts = totalJointAccounts,
            ),
            accounts = emptyList(),
            unassignedTokens = emptyList(),
        )
    }

    internal data class IndexModel(val counter: Int?, val expected: Int)

    private companion object {
        val WALLET_ID = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")
        const val OWNER_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val SIGNATURE = "0xdeadbeef"
        const val INVITE_A = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
        const val INVITE_B = "BB11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
    }
}