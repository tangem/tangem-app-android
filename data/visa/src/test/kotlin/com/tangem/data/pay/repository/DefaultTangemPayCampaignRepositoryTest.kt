package com.tangem.data.pay.repository

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import com.tangem.spend.datasource.pay.models.response.ImagesResponse
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultTangemPayCampaignRepositoryTest {

    private val tangemPayApi: TangemPayApi = mockk()
    private val requestHelper: TangemPayRequestPerformer = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(tangemPayApi, requestHelper)

        coEvery { requestHelper.performWithStaticToken<Any>(requestBlock = any()) } coAnswers {
            val block = firstArg<suspend () -> ApiResponse<Any>>()
            when (val response = block()) {
                is ApiResponse.Success -> response.data.right()
                is ApiResponse.Error -> VisaApiError.Unspecified.left()
            }
        }
        coEvery { tangemPayApi.getImages(any()) } returns ApiResponse.Success(imagesResponse(mainUrl = MAIN_URL))
    }

    @Test
    fun `GIVEN images with MAIN WHEN getCampaign THEN the MAIN url is the card art`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val actual = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(actual).isEqualTo(TangemPayCampaign(cardArtUrl = MAIN_URL).right())
        coVerify(exactly = 1) { tangemPayApi.getImages(deeplinkValue = CAMPAIGN_KEY) }
    }

    @Test
    fun `GIVEN images without MAIN WHEN getCampaign THEN the campaign has no art`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getImages(any()) } returns ApiResponse.Success(
            ImagesResponse(result = listOf(ImagesResponse.Image(type = "BACKGROUND", url = BACKGROUND_URL))),
        )
        val repository = createRepository()

        // Act
        val actual = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(actual).isEqualTo(TangemPayCampaign(cardArtUrl = null).right())
    }

    @Test
    fun `GIVEN empty result WHEN getCampaign THEN the campaign has no art`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getImages(any()) } returns ApiResponse.Success(ImagesResponse(result = emptyList()))
        val repository = createRepository()

        // Act
        val actual = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(actual).isEqualTo(TangemPayCampaign(cardArtUrl = null).right())
    }

    @Test
    fun `GIVEN a blank MAIN url WHEN getCampaign THEN the campaign has no art`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getImages(any()) } returns ApiResponse.Success(
            ImagesResponse(result = listOf(ImagesResponse.Image(type = "MAIN", url = " "))),
        )
        val repository = createRepository()

        // Act
        val actual = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(actual).isEqualTo(TangemPayCampaign(cardArtUrl = null).right())
    }

    @Test
    fun `GIVEN api error WHEN getCampaign THEN error returned AND next call hits backend again`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getImages(any()) } returnsMany listOf(
            ApiResponse.Error(ApiResponseError.NetworkException()) as ApiResponse<ImagesResponse>,
            ApiResponse.Success(imagesResponse(mainUrl = MAIN_URL)),
        )
        val repository = createRepository()

        // Act
        val first = repository.getCampaign(CAMPAIGN_KEY)
        val second = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(first).isEqualTo(VisaApiError.Unspecified.left())
        assertThat(second).isEqualTo(TangemPayCampaign(cardArtUrl = MAIN_URL).right())
        coVerify(exactly = 2) { tangemPayApi.getImages(any()) }
    }

    @Test
    fun `GIVEN campaign fetched WHEN getCampaign again THEN cached without a backend call`() = runTest {
        // Arrange
        val repository = createRepository()

        // Act
        val first = repository.getCampaign(CAMPAIGN_KEY)
        val second = repository.getCampaign(CAMPAIGN_KEY)

        // Assert
        assertThat(second).isEqualTo(first)
        coVerify(exactly = 1) { tangemPayApi.getImages(any()) }
    }

    @Test
    fun `GIVEN another key WHEN getCampaign THEN it is fetched on its own`() = runTest {
        // Arrange
        coEvery { tangemPayApi.getImages(OTHER_CAMPAIGN_KEY) } returns
            ApiResponse.Success(imagesResponse(mainUrl = OTHER_MAIN_URL))
        val repository = createRepository()

        // Act
        val first = repository.getCampaign(CAMPAIGN_KEY)
        val second = repository.getCampaign(OTHER_CAMPAIGN_KEY)

        // Assert
        assertThat(first).isEqualTo(TangemPayCampaign(cardArtUrl = MAIN_URL).right())
        assertThat(second).isEqualTo(TangemPayCampaign(cardArtUrl = OTHER_MAIN_URL).right())
        coVerify(exactly = 1) { tangemPayApi.getImages(deeplinkValue = CAMPAIGN_KEY) }
        coVerify(exactly = 1) { tangemPayApi.getImages(deeplinkValue = OTHER_CAMPAIGN_KEY) }
    }

    private fun imagesResponse(mainUrl: String) = ImagesResponse(
        result = listOf(
            ImagesResponse.Image(type = "BACKGROUND", url = BACKGROUND_URL),
            ImagesResponse.Image(type = "MAIN", url = mainUrl),
        ),
    )

    private fun createRepository() = DefaultTangemPayCampaignRepository(
        tangemPayApi = tangemPayApi,
        requestHelper = requestHelper,
    )

    private companion object {
        const val CAMPAIGN_KEY = "tpay_for_oxford"
        const val OTHER_CAMPAIGN_KEY = "tpay_for_harvard"
        const val MAIN_URL = "https://images.us.paera.com/oxford.png"
        const val OTHER_MAIN_URL = "https://images.us.paera.com/harvard.png"
        const val BACKGROUND_URL = "https://images.us.paera.com/oxford_bg.png"
    }
}