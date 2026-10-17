package com.tangem.domain.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.nft.NFTAccountFixtures.accountList
import com.tangem.domain.nft.NFTAccountFixtures.ethereum
import com.tangem.domain.nft.NFTAccountFixtures.jointAccount
import com.tangem.domain.nft.NFTAccountFixtures.personalAccount
import com.tangem.domain.nft.NFTAccountFixtures.userWalletId
import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.domain.nft.repository.NFTRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetNFTNetworksUseCaseTest {

    private val nftRepository: NFTRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = GetNFTNetworksUseCase(
        singleAccountListSupplier = singleAccountListSupplier,
        nftRepository = nftRepository,
    )

    @Test
    fun `GIVEN personal account WHEN invoke THEN its networks are offered`() = runTest {
        // Arrange
        every { singleAccountListSupplier(userWalletId) } returns flowOf(accountList)
        coEvery { nftRepository.isNFTSupported(userWalletId, ethereum.network) } returns true
        coEvery { nftRepository.getNFTSupportedNetworks(userWalletId) } returns emptyList()

        // Act
        val actual = useCase(personalAccount.accountId).toList()

        // Assert
        val expected = NFTNetworks(availableNetworks = listOf(ethereum.network), unavailableNetworks = emptyList())
        assertThat(actual).containsExactly(expected)
    }

    @Test
    fun `GIVEN joint account WHEN invoke THEN nothing is offered`() = runTest {
        // Arrange
        every { singleAccountListSupplier(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = useCase(jointAccount.accountId).toList()

        // Assert
        assertThat(actual).isEmpty()
    }
}