package com.tangem.data.earn.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.earn.DefaultEarnErrorResolver
import com.tangem.data.earn.datastore.EarnFilterStore
import com.tangem.data.earn.datastore.EarnNetworksStore
import com.tangem.data.earn.datastore.EarnTokensCatalogStore
import com.tangem.data.earn.datastore.EarnTopTokensStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.EarnListResponse
import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.datasource.api.tangemTech.models.EarnTokenResponse
import com.tangem.datasource.api.tangemTech.models.MetaEarnListResponse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.earn.EarnError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The catalogue is paged until `meta.hasNext` says otherwise. These pin that rule, the fallback for a
 * response that omits the flag, and the guards around both.
 *
 * Fixtures mirror the wire: lowercase `type`/`rewardType`, fractional apy strings.
 */
internal class DefaultEarnRepositoryCatalogTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val selectedWallet = MutableStateFlow<UserWallet?>(MockUserWalletFactory.create())

    private val catalogStore = EarnTokensCatalogStore()

    private val repository = DefaultEarnRepository(
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        userWalletsListRepository = userWalletsListRepository,
        earnNetworksStore = EarnNetworksStore(),
        earnTopTokensStore = EarnTopTokensStore(),
        earnTokensCatalogStore = catalogStore,
        earnFilterStore = EarnFilterStore(),
        earnErrorResolver = DefaultEarnErrorResolver(),
        excludedBlockchains = ExcludedBlockchains(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository)
        selectedWallet.value = MockUserWalletFactory.create()
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
    }

    @Test
    fun `GIVEN hasNext is false WHEN the catalogue is fetched THEN only one page is requested`() = runTest {
        // Arrange
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns page(hasNext = false)

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        coVerify(exactly = 1) { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) }
        assertThat(catalogStore.get().first()?.isRight()).isTrue()
    }

    @Test
    fun `GIVEN hasNext turns false on the third page WHEN the catalogue is fetched THEN paging stops there`() =
        runTest {
            // Arrange
            coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returnsMany listOf(
                page(hasNext = true),
                page(hasNext = true),
                page(hasNext = false),
            )

            // Act
            repository.fetchEarnTokensCatalog()

            // Assert
            coVerify(exactly = 3) { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN a full page without hasNext WHEN the catalogue is fetched THEN paging follows the server limit`() =
        runTest {
            // Arrange — the server clamped our request to 20, so 20 rows is a full page, not a short one
            coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returnsMany listOf(
                page(itemCount = SERVER_LIMIT, serverLimit = SERVER_LIMIT, hasNext = null),
                page(itemCount = 3, serverLimit = SERVER_LIMIT, hasNext = null),
            )

            // Act
            repository.fetchEarnTokensCatalog()

            // Assert
            coVerify(exactly = 2) { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN rows the mapping drops WHEN a full page is fetched THEN paging still continues`() = runTest {
        // Arrange — an unsupported network maps to nothing, which must not read as the end of the list
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returnsMany listOf(
            page(hasNext = true, networkId = "not-a-real-network"),
            page(hasNext = false),
        )

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        coVerify(exactly = 2) { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the same row on two pages WHEN the catalogue is fetched THEN it is kept once`() = runTest {
        // Arrange — a duplicate would become a duplicate Compose key in the search list
        val duplicated = earnResponse(tokenId = "usd-coin", networkId = "ethereum")
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returnsMany listOf(
            response(items = listOf(duplicated), hasNext = true),
            response(items = listOf(duplicated), hasNext = false),
        )

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        assertThat(catalogStore.get().first()?.getOrNull()).hasSize(1)
    }

    @Test
    fun `GIVEN the same token on three networks WHEN the catalogue is fetched THEN all three are kept`() = runTest {
        // Arrange — the real payload lists usd-coin on base, ethereum and avalanche
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns response(
            items = listOf(
                earnResponse(tokenId = "usd-coin", networkId = "base", address = "0x833589"),
                earnResponse(tokenId = "usd-coin", networkId = "ethereum", address = "0xa0b869"),
                earnResponse(tokenId = "usd-coin", networkId = "avalanche", address = "0xb97ef9"),
            ),
            hasNext = false,
        )

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        val stored = catalogStore.get().first()?.getOrNull().orEmpty()
        assertThat(stored.map { it.earnToken.networkId }).containsExactly("base", "ethereum", "avalanche")
    }

    @Test
    fun `GIVEN the api fails WHEN the catalogue is fetched THEN the failure is stored`() = runTest {
        // Arrange
        coEvery { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) } returns httpError()

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        val stored = catalogStore.get().first()?.leftOrNull()
        assertThat(stored).isInstanceOf(EarnError.HttpError::class.java)
        assertThat((stored as EarnError.HttpError).code).isEqualTo(500)
    }

    @Test
    fun `GIVEN no selected wallet WHEN the catalogue is fetched THEN nothing is requested or stored`() = runTest {
        // Arrange
        selectedWallet.value = null

        // Act
        repository.fetchEarnTokensCatalog()

        // Assert
        coVerify(exactly = 0) { tangemTechApi.getEarnTokens(any(), any(), any(), any(), any()) }
        assertThat(catalogStore.get().first()).isNull()
    }

    /** `ApiResponse.Error` is an `ApiResponse<Nothing>`, and the type is invariant. */
    @Suppress("UNCHECKED_CAST")
    private fun httpError(): ApiResponse<EarnListResponse> = ApiResponse.Error(
        cause = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            message = "boom",
            errorBody = null,
        ),
    ) as ApiResponse<EarnListResponse>

    private fun page(
        hasNext: Boolean?,
        itemCount: Int = 3,
        serverLimit: Int = PAGE_SIZE,
        networkId: String = "ethereum",
    ): ApiResponse<EarnListResponse> = response(
        items = List(itemCount) { index ->
            earnResponse(tokenId = "usd-coin-$index", networkId = networkId, address = "0xabc$index")
        },
        hasNext = hasNext,
        serverLimit = serverLimit,
    )

    private fun response(
        items: List<EarnResponse>,
        hasNext: Boolean?,
        serverLimit: Int = PAGE_SIZE,
    ): ApiResponse<EarnListResponse> = ApiResponse.Success(
        data = EarnListResponse(
            meta = MetaEarnListResponse(page = 1, limit = serverLimit, total = 51, hasNext = hasNext),
            items = items,
        ),
    )

    private fun earnResponse(
        tokenId: String,
        networkId: String,
        address: String? = null,
        type: String = "yield",
    ) = EarnResponse(
        apy = "0.038400",
        networkId = networkId,
        rewardType = "apy",
        type = type,
        token = EarnTokenResponse(
            id = tokenId,
            symbol = "USDC",
            name = "USDC",
            address = address,
            decimalCount = 6,
        ),
    )

    private companion object {

        const val PAGE_SIZE = 50
        const val SERVER_LIMIT = 20
        const val MAX_PAGES = 10
    }
}