package com.tangem.data.jointaccount.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.jointaccount.FIXTURE_CRYPTO_ACCOUNT_ID
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.converter.JointOperationConverter
import com.tangem.data.jointaccount.converter.SafeTxConverter
import com.tangem.data.jointaccount.converter.JointAccountInvitePreviewDtoConverter
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.data.jointaccount.createJointAccountInvitePreview
import com.tangem.data.jointaccount.createJointAccountInvitePreviewDto
import com.tangem.data.jointaccount.createSafeTx
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.store.datasource.jointaccount.models.CreateJointOperationRequest
import com.tangem.store.datasource.jointaccount.models.JointOperationDto
import org.junit.jupiter.params.provider.MethodSource
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.ActivateJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.CreateJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.JoinJointAccountRequest
import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.jointaccount.model.JointAccountActivationPayload
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.model.JointAccountCreationResult
import com.tangem.domain.jointaccount.model.JointAccountInvitePreviewError
import com.tangem.domain.jointaccount.model.JointAccountJoinPayload
import com.tangem.domain.jointaccount.model.JointAccountJoinResult
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
        converter = JointAccountDtoConverter(operationConverter = JointOperationConverter(safeTxConverter = SafeTxConverter())),
        previewConverter = JointAccountInvitePreviewDtoConverter(),
        safeTxConverter = SafeTxConverter(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        moshi = Moshi.Builder().build(),
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
        val response = errorResponse<JointAccountDto>(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)
        coEvery { jointAccountApi.createJointAccount(any(), any()) } returns response

        // Act
        val actual = runCatching {
            repository.create(userWalletId = WALLET_ID, payload = createPayload(), signature = SIGNATURE)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isEqualTo((response as ApiResponse.Error).cause)
    }

    @Test
    fun `GIVEN payload WHEN join THEN it is sent verbatim and the account returned`() = runTest {
        // Arrange
        val bodySlot = slot<JoinJointAccountRequest>()
        coEvery {
            jointAccountApi.joinJointAccount(walletId = WALLET_ID.stringValue, body = capture(bodySlot))
        } returns ApiResponse.Success(createJointAccountDto())

        // Act
        val actual = repository.join(userWalletId = WALLET_ID, payload = joinPayload(), signature = SIGNATURE)

        // Assert
        val expected = JointAccountJoinResult.Joined(account = createJointAccount(source = StatusSource.ACTUAL))
        assertThat(actual).isEqualTo(expected)
        // The signature covers the canonical form of exactly these values — trimming any of them breaks it
        val body = bodySlot.captured
        assertThat(body.signature).isEqualTo(SIGNATURE)
        assertThat(body.payload.inviteId).isEqualTo(INVITE_A)
        assertThat(body.payload.config.name).isEqualTo(" Family ")
        assertThat(body.payload.member.name).isEqualTo(" Bob ")
        assertThat(body.payload.member.walletId).isEqualTo(WALLET_ID.stringValue)
        assertThat(body.payload.member.address).isEqualTo(OWNER_ADDRESS)
        assertThat(body.payload.member.derivation).isEqualTo(4)
    }

    @Test
    fun `GIVEN conflict WHEN join THEN Conflict result`() = runTest {
        // Arrange — a spent invite and an already-registered address are the same 409; the domain resolves it
        coEvery { jointAccountApi.joinJointAccount(any(), any()) } returns errorResponse(
            code = ApiResponseError.HttpException.Code.CONFLICT,
        )

        // Act
        val actual = repository.join(userWalletId = WALLET_ID, payload = joinPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(JointAccountJoinResult.Conflict)
    }

    @Test
    fun `GIVEN server error WHEN join THEN it is thrown`() = runTest {
        // Arrange
        val response = errorResponse<JointAccountDto>(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)
        coEvery { jointAccountApi.joinJointAccount(any(), any()) } returns response

        // Act
        val actual = runCatching {
            repository.join(userWalletId = WALLET_ID, payload = joinPayload(), signature = SIGNATURE)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isEqualTo((response as ApiResponse.Error).cause)
    }

    @Test
    fun `GIVEN payload WHEN activate THEN it is sent verbatim and Right returned`() = runTest {
        // Arrange
        val bodySlot = slot<ActivateJointAccountRequest>()
        coEvery {
            jointAccountApi.activateJointAccount(walletId = WALLET_ID.stringValue, body = capture(bodySlot))
        } returns ApiResponse.Success(createJointAccountDto())

        // Act
        val actual = repository.activate(userWalletId = WALLET_ID, payload = activationPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        // The signature covers the canonical form of exactly these values — trimming any of them breaks it
        val body = bodySlot.captured
        assertThat(body.signature).isEqualTo(SIGNATURE)
        assertThat(body.payload.walletId).isEqualTo(WALLET_ID.stringValue)
        assertThat(body.payload.cryptoAccountId).isEqualTo(FIXTURE_CRYPTO_ACCOUNT_ID)
        assertThat(body.payload.config.name).isEqualTo(" Family ")
        assertThat(body.payload.config.safeAddress).isEqualTo(SAFE_ADDRESS)
    }

    @Test
    fun `GIVEN timeout then success WHEN activate THEN retried once and Right returned`() = runTest {
        // Arrange — the endpoint is idempotent, a lost response must not surface as an error
        coEvery { jointAccountApi.activateJointAccount(any(), any()) } returnsMany listOf(
            timeoutResponse(),
            ApiResponse.Success(createJointAccountDto()),
        )

        // Act
        val actual = repository.activate(userWalletId = WALLET_ID, payload = activationPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 2) { jointAccountApi.activateJointAccount(any(), any()) }
    }

    @Test
    fun `GIVEN timeout twice WHEN activate THEN Left with the retry error`() = runTest {
        // Arrange
        val retryResponse = timeoutResponse()
        coEvery { jointAccountApi.activateJointAccount(any(), any()) } returnsMany listOf(
            timeoutResponse(),
            retryResponse,
        )

        // Act
        val actual = repository.activate(userWalletId = WALLET_ID, payload = activationPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(Either.Left((retryResponse as ApiResponse.Error).cause))
        coVerify(exactly = 2) { jointAccountApi.activateJointAccount(any(), any()) }
    }

    @Test
    fun `GIVEN server error WHEN activate THEN Left without a retry`() = runTest {
        // Arrange
        val response = errorResponse<JointAccountDto>(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)
        coEvery { jointAccountApi.activateJointAccount(any(), any()) } returns response

        // Act
        val actual = repository.activate(userWalletId = WALLET_ID, payload = activationPayload(), signature = SIGNATURE)

        // Assert
        assertThat(actual).isEqualTo(Either.Left((response as ApiResponse.Error).cause))
        coVerify(exactly = 1) { jointAccountApi.activateJointAccount(any(), any()) }
    }

    @Test
    fun `GIVEN successful response WHEN getInvitePreview THEN the converted preview is returned`() = runTest {
        // Arrange
        val dto = createJointAccountInvitePreviewDto()
        coEvery {
            jointAccountApi.getJointAccountInvite(walletId = WALLET_ID.stringValue, inviteId = INVITE_A)
        } returns ApiResponse.Success(dto)

        // Act
        val actual = repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_A)

        // Assert
        assertThat(actual).isEqualTo(Either.Right(createJointAccountInvitePreview()))
    }

    @Test
    fun `GIVEN conflict response WHEN getInvitePreview THEN InvalidLink`() = runTest {
        // Arrange
        coEvery { jointAccountApi.getJointAccountInvite(any(), any()) } returns
            invitePreviewErrorResponse(code = ApiResponseError.HttpException.Code.CONFLICT)

        // Act
        val actual = repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_A)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(JointAccountInvitePreviewError.InvalidLink))
    }

    /** A link the backend does not know is as dead as one it refuses — retrying it can only fail again */
    @Test
    fun `GIVEN not found response WHEN getInvitePreview THEN InvalidLink`() = runTest {
        // Arrange
        val response = invitePreviewErrorResponse(code = ApiResponseError.HttpException.Code.NOT_FOUND)
        coEvery { jointAccountApi.getJointAccountInvite(any(), any()) } returns response

        // Act
        val actual = repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_A)

        // Assert
        assertThat(actual).isEqualTo(Either.Left(JointAccountInvitePreviewError.InvalidLink))
    }

    @Test
    fun `GIVEN server error WHEN getInvitePreview THEN LoadFailed`() = runTest {
        // Arrange
        val response = invitePreviewErrorResponse(code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)
        coEvery { jointAccountApi.getJointAccountInvite(any(), any()) } returns response

        // Act
        val actual = repository.getInvitePreview(walletId = WALLET_ID, inviteId = INVITE_A)

        // Assert
        val expectedCause = (response as ApiResponse.Error).cause
        assertThat(actual).isEqualTo(Either.Left(JointAccountInvitePreviewError.LoadFailed(cause = expectedCause)))
    }

    @Test
    fun `GIVEN stored invites WHEN getInvites THEN they are returned`() = runTest {
        // Arrange
        coEvery { invitesStore.get(WALLET_ID, FIXTURE_CRYPTO_ACCOUNT_ID) } returns listOf("invite-a", "invite-b")

        // Act
        val actual = repository.getInvites(userWalletId = WALLET_ID, cryptoAccountId = FIXTURE_CRYPTO_ACCOUNT_ID)

        // Assert
        assertThat(actual).containsExactly("invite-a", "invite-b").inOrder()
    }

    @Test
    fun `GIVEN unreadable store WHEN getInvites THEN empty instead of throwing`() = runTest {
        // Arrange
        // The members are worth showing even when the invite actions cannot be offered
        coEvery { invitesStore.get(WALLET_ID, FIXTURE_CRYPTO_ACCOUNT_ID) } throws
            IllegalStateException("keystore unavailable")

        // Act
        val actual = runCatching {
            repository.getInvites(userWalletId = WALLET_ID, cryptoAccountId = FIXTURE_CRYPTO_ACCOUNT_ID)
        }

        // Assert
        assertThat(actual.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN nothing stored WHEN getInvites THEN empty - this device is not the creator's`() = runTest {
        // Arrange
        coEvery { invitesStore.get(WALLET_ID, FIXTURE_CRYPTO_ACCOUNT_ID) } returns null

        // Act
        val actual = repository.getInvites(userWalletId = WALLET_ID, cryptoAccountId = FIXTURE_CRYPTO_ACCOUNT_ID)

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun joinPayload() = JointAccountJoinPayload(
        inviteId = INVITE_A,
        config = JointAccountConfig(
            name = " Family ",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 3,
            threshold = 2,
        ),
        member = JointAccountParticipant(
            walletId = WALLET_ID.stringValue,
            name = " Bob ",
            address = OWNER_ADDRESS,
            derivation = 4,
        ),
    )

    private fun activationPayload() = JointAccountActivationPayload(
        walletId = WALLET_ID.stringValue,
        cryptoAccountId = FIXTURE_CRYPTO_ACCOUNT_ID,
        config = JointAccountActivationPayload.ConfirmedConfig(
            base = JointAccountConfig(
                name = " Family ",
                icon = "Family",
                iconColor = "Azure",
                membersCount = 3,
                threshold = 2,
            ),
            safeAddress = SAFE_ADDRESS,
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun timeoutResponse(): ApiResponse<JointAccountDto> {
        return ApiResponse.Error(cause = ApiResponseError.TimeoutException()) as ApiResponse<JointAccountDto>
    }

    @Test
    fun `GIVEN safe tx WHEN createOperation THEN the request carries it verbatim and Right is returned`() = runTest {
        // Arrange
        val bodySlot = slot<CreateJointOperationRequest>()
        val safeTx = createSafeTx()
        coEvery {
            jointAccountApi.createJointOperation(walletId = WALLET_ID.stringValue, body = capture(bodySlot))
        } returns ApiResponse.Success(createJointOperationDto())

        // Act
        val actual = repository.createOperation(
            userWalletId = WALLET_ID,
            safeAddress = SAFE_ADDRESS,
            chainId = CHAIN_ID,
            safeTx = safeTx,
            signature = SIGNATURE,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(bodySlot.captured).isEqualTo(
            CreateJointOperationRequest(
                safeAddress = SAFE_ADDRESS,
                chainId = CHAIN_ID,
                safeTx = SafeTxConverter().convertBack(safeTx),
                signature = SIGNATURE,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("provideCreateErrorModels")
    fun createOperationErrors(model: CreateErrorModel) = runTest {
        // Arrange
        coEvery { jointAccountApi.createJointOperation(any(), any()) } returns operationErrorResponse(
            code = model.code,
            errorBody = model.errorBody,
        )

        // Act
        val actual = repository.createOperation(
            userWalletId = WALLET_ID,
            safeAddress = SAFE_ADDRESS,
            chainId = CHAIN_ID,
            safeTx = createSafeTx(),
            signature = SIGNATURE,
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(model.expected)
    }

    internal data class CreateErrorModel(
        val code: ApiResponseError.HttpException.Code,
        val errorBody: String?,
        val expected: Class<out JointOperationCreateError>,
    )

    private fun provideCreateErrorModels() = listOf(
        CreateErrorModel(
            code = ApiResponseError.HttpException.Code.CONFLICT,
            errorBody = null,
            expected = JointOperationCreateError.SlotBusy::class.java,
        ),
        CreateErrorModel(
            code = ApiResponseError.HttpException.Code.BAD_REQUEST,
            errorBody = """{"statusCode":400,"error":"Bad Request","code":"NONCE_MISMATCH"}""",
            expected = JointOperationCreateError.NetworkStateMoved::class.java,
        ),
        CreateErrorModel(
            code = ApiResponseError.HttpException.Code.BAD_REQUEST,
            errorBody = """{"error":"bad payload"}""",
            expected = JointOperationCreateError.Failed::class.java,
        ),
        CreateErrorModel(
            code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            errorBody = null,
            expected = JointOperationCreateError.Failed::class.java,
        ),
    )

    private fun createJointOperationDto() = JointOperationDto(
        operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
        chainId = CHAIN_ID,
        safeTx = SafeTxConverter().convertBack(createSafeTx()),
        status = "awaiting_signatures",
        initiator = null,
        createdAt = null,
        votes = emptyList(),
    )

    private fun operationErrorResponse(
        code: ApiResponseError.HttpException.Code,
        errorBody: String?,
    ): ApiResponse<JointOperationDto> {
        val cause = ApiResponseError.HttpException(code = code, message = null, errorBody = errorBody)

        @Suppress("UNCHECKED_CAST")
        return ApiResponse.Error(cause = cause) as ApiResponse<JointOperationDto>
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
    private fun invitePreviewErrorResponse(
        code: ApiResponseError.HttpException.Code,
    ): ApiResponse<JointAccountInvitePreviewDto> {
        val cause = ApiResponseError.HttpException(code = code, message = null, errorBody = null)

        return ApiResponse.Error(cause = cause) as ApiResponse<JointAccountInvitePreviewDto>
    }

    @Test
    fun `GIVEN an operation WHEN reject THEN the call carries the id and no body`() = runTest {
        // Arrange
        coEvery {
            jointAccountApi.rejectJointOperation(walletId = WALLET_ID.stringValue, operationId = OPERATION_ID)
        } returns ApiResponse.Success(Unit)

        // Act
        val actual = repository.rejectOperation(userWalletId = WALLET_ID, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            jointAccountApi.rejectJointOperation(walletId = WALLET_ID.stringValue, operationId = OPERATION_ID)
        }
    }

    @Test
    fun `GIVEN the threshold was reached first WHEN reject THEN ThresholdReached`() = runTest {
        // Arrange
        coEvery { jointAccountApi.rejectJointOperation(any(), any()) } returns
            errorResponse(ApiResponseError.HttpException.Code.CONFLICT)

        // Act
        val actual = repository.rejectOperation(userWalletId = WALLET_ID, operationId = OPERATION_ID)

        // Assert
        assertThat(actual).isEqualTo(JointOperationRejectError.ThresholdReached.left())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> errorResponse(code: ApiResponseError.HttpException.Code): ApiResponse<T> {
        val cause = ApiResponseError.HttpException(code = code, message = null, errorBody = null)

        return ApiResponse.Error(cause = cause) as ApiResponse<T>
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
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val SIGNATURE = "0xdeadbeef"
        const val OPERATION_ID = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708"
        const val CHAIN_ID = 137L
        const val INVITE_A = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
        const val INVITE_B = "BB11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"
    }
}