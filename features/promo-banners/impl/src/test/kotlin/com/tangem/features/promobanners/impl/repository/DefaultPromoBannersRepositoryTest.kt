package com.tangem.features.promobanners.impl.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.promobanners.PromoBannerDisplayDTO
import com.tangem.datasource.api.tangemTech.models.promobanners.PromoBannerDisplaysResponse
import com.tangem.features.promobanners.api.PromoBannersBlockComponent.Placeholder
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayDTOConverter
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class DefaultPromoBannersRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetBannersSync {

        @Test
        fun `GIVEN key not cached WHEN getBannersSync called twice at once THEN one request serves both`() = runTest {
            // Arrange
            val repository = createRepository()
            val response = CompletableDeferred<ApiResponse<PromoBannerDisplaysResponse>>()
            coEvery { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.MAIN.value, any()) } coAnswers {
                response.await()
            }

            // Act
            val first = async { repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE) }
            val second = async { repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE) }
            runCurrent()
            response.complete(ApiResponse.Success(PromoBannerDisplaysResponse(items = listOf(createDto(id = 1)))))

            // Assert
            assertThat(first.await()).containsExactly(createBanner(id = 1))
            assertThat(second.await()).containsExactly(createBanner(id = 1))
            coVerify(exactly = 1) { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.MAIN.value, any()) }
        }

        @Test
        fun `GIVEN refresh in flight WHEN getBannersSync THEN it returns the refreshed banners`() = runTest {
            // Arrange
            val repository = createRepository()
            stubBanners(Placeholder.MAIN, createDto(id = 1))
            repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE)
            val refreshed = CompletableDeferred<ApiResponse<PromoBannerDisplaysResponse>>()
            coEvery { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.MAIN.value, any()) } coAnswers {
                refreshed.await()
            }
            launch { repository.refreshBanners(walletIds = setOf(WALLET_ID), languageISOCode = LANGUAGE) }
            runCurrent()

            // Act
            val banners = async { repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE) }
            runCurrent()
            refreshed.complete(ApiResponse.Success(PromoBannerDisplaysResponse(items = listOf(createDto(id = 2)))))

            // Assert
            assertThat(banners.await()).containsExactly(createBanner(id = 2))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RefreshBanners {

        @Test
        fun `GIVEN banners cached for two placeholders WHEN refreshBanners THEN both refetched and updates emit`() =
            runTest {
                // Arrange
                val repository = createRepository()
                stubBanners(Placeholder.MAIN, createDto(id = 1), createDto(id = 2))
                stubBanners(Placeholder.FEED, createDto(id = 3))
                repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE)
                repository.getBannersSync(WALLET_ID, Placeholder.FEED, LANGUAGE)
                stubBanners(Placeholder.MAIN, createDto(id = 2))
                stubBanners(Placeholder.FEED)

                repository.getBanners(WALLET_ID, Placeholder.MAIN).test {
                    assertThat(awaitItem()).containsExactly(createBanner(id = 1), createBanner(id = 2)).inOrder()

                    // Act
                    repository.refreshBanners(walletIds = setOf(WALLET_ID), languageISOCode = LANGUAGE)

                    // Assert
                    assertThat(awaitItem()).containsExactly(createBanner(id = 2))
                    expectNoEvents()
                }
                assertThat(repository.getCachedBanners(WALLET_ID, Placeholder.FEED)).isEmpty()
                coVerify(exactly = 2) { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.MAIN.value, any()) }
                coVerify(exactly = 2) { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.FEED.value, any()) }
            }

        @Test
        fun `GIVEN wallet not cached WHEN refreshBanners THEN no request`() = runTest {
            // Arrange
            val repository = createRepository()
            stubBanners(Placeholder.MAIN, createDto(id = 1))
            repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE)

            // Act
            repository.refreshBanners(walletIds = setOf(OTHER_WALLET_ID), languageISOCode = LANGUAGE)

            // Assert
            coVerify(exactly = 0) { tangemTechApi.getPromoBannerDisplays(OTHER_WALLET_ID, any(), any()) }
            coVerify(exactly = 1) { tangemTechApi.getPromoBannerDisplays(WALLET_ID, any(), any()) }
        }

        @Test
        fun `GIVEN refetch fails WHEN refreshBanners THEN cached banners kept`() = runTest {
            // Arrange
            val repository = createRepository()
            stubBanners(Placeholder.MAIN, createDto(id = 1))
            repository.getBannersSync(WALLET_ID, Placeholder.MAIN, LANGUAGE)
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemTechApi.getPromoBannerDisplays(WALLET_ID, Placeholder.MAIN.value, any()) } returns
                ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<PromoBannerDisplaysResponse>

            // Act
            repository.refreshBanners(walletIds = setOf(WALLET_ID), languageISOCode = LANGUAGE)

            // Assert
            assertThat(repository.getCachedBanners(WALLET_ID, Placeholder.MAIN)).containsExactly(createBanner(id = 1))
        }
    }

    private fun createRepository() = DefaultPromoBannersRepository(
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        cache = RuntimeSharedStore(),
    )

    private fun stubBanners(placeholder: Placeholder, vararg banners: PromoBannerDisplayDTO) {
        coEvery { tangemTechApi.getPromoBannerDisplays(WALLET_ID, placeholder.value, any()) } returns
            ApiResponse.Success(PromoBannerDisplaysResponse(items = banners.toList()))
    }

    private fun createBanner(id: Int) = PromoBannerDisplayDTOConverter().convert(createDto(id))

    private fun createDto(id: Int) = PromoBannerDisplayDTO(
        id = id,
        placeholder = Placeholder.MAIN.value,
        priority = "MEDIUM",
        title = "Title $id",
        subtitle = "Subtitle",
        iconUrl = null,
        deeplink = null,
        buttonEnabled = false,
        buttonText = null,
        dismissable = false,
        campaignId = null,
    )

    private companion object {
        const val WALLET_ID = "00aa"
        const val OTHER_WALLET_ID = "00bb"
        const val LANGUAGE = "en"
    }
}