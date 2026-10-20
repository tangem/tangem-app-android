package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.domain.collectibles.models.CollectiblesError
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class GetCollectibleBuybackOfferUseCaseTest {

    private val repository: CollectiblesRepository = mockk()

    private val useCase = GetCollectibleBuybackOfferUseCase(repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN an offer WHEN invoke THEN it is returned after a single request`() = runTest {
        // Arrange
        val offer = CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = null)
        coEvery { repository.getBuybackOffer(WALLET_ID, NFT_ADDRESS) } returns offer

        // Act
        val actual = useCase(WALLET_ID, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(offer.right())
        coVerify(exactly = 1) { repository.getBuybackOffer(WALLET_ID, NFT_ADDRESS) }
    }

    @Test
    fun `GIVEN no offer yet WHEN invoke THEN null is returned without asking again`() = runTest {
        // Arrange
        coEvery { repository.getBuybackOffer(WALLET_ID, NFT_ADDRESS) } returns null

        // Act
        val actual = useCase(WALLET_ID, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(null.right())
        coVerify(exactly = 1) { repository.getBuybackOffer(WALLET_ID, NFT_ADDRESS) }
    }

    @Test
    fun `GIVEN repository throws WHEN invoke THEN returns an Api error carrying the cause`() = runTest {
        // Arrange
        val cause = IllegalStateException("backend is down")
        coEvery { repository.getBuybackOffer(any(), any()) } throws cause

        // Act
        val actual = useCase(WALLET_ID, NFT_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesError.Api(cause).left())
    }

    private companion object {

        val WALLET_ID = UserWalletId("aa")
        const val NFT_ADDRESS = "nft"
    }
}