package com.tangem.domain.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.nft.NFTAccountFixtures.accountList
import com.tangem.domain.nft.NFTAccountFixtures.ethereum
import com.tangem.domain.nft.NFTAccountFixtures.jointAccount
import com.tangem.domain.nft.NFTAccountFixtures.jointEthereum
import com.tangem.domain.nft.NFTAccountFixtures.personalAccount
import com.tangem.domain.nft.NFTAccountFixtures.userWalletId
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.repository.NFTRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetNFTCollectionsUseCaseTest {

    private val nftRepository: NFTRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = GetNFTCollectionsUseCase(
        nftRepository = nftRepository,
        singleAccountListSupplier = singleAccountListSupplier,
    )

    @Test
    fun `GIVEN personal and joint accounts WHEN invoke THEN only personal collections are observed`() = runTest {
        // Arrange
        val collections = listOf(mockk<NFTCollections>())
        every { singleAccountListSupplier(userWalletId) } returns flowOf(accountList)
        every { nftRepository.observeCollections(userWalletId, listOf(ethereum.network)) } returns flowOf(collections)

        // Act
        val actual = useCase(userWalletId).first()

        // Assert
        assertThat(actual.collections).containsExactly(personalAccount, collections)
        assertThat(actual.collections).doesNotContainKey(jointAccount)
        verify(exactly = 0) { nftRepository.observeCollections(userWalletId, listOf(jointEthereum.network)) }
    }
}