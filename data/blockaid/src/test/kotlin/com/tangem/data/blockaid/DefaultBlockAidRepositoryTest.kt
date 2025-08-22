package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.google.common.truth.Truth
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.datasource.api.common.blockaid.models.request.DomainScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.EvmTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.SolanaTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.response.DomainScanResponse
import com.tangem.datasource.api.common.blockaid.models.response.SolanaTransactionResponse
import com.tangem.datasource.api.common.blockaid.models.response.TransactionScanResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DefaultBlockAidRepositoryTest {

    @MockK
    private lateinit var api: BlockAidApi

    @MockK
    private lateinit var mapper: BlockAidMapper

    private lateinit var repository: DefaultBlockAidRepository

    private val dispatchers = TestingCoroutineDispatcherProvider()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = DefaultBlockAidRepository(api, dispatchers, mapper)
    }

    @Test
    fun `when verify app domain then calls api and maps result`() = runTest {
        val url = "https://example.com"
        val domainData = DAppData(url)
        val domainResponse = DomainScanResponse(status = "hit", isMalicious = false)
        val expectedResult = CheckDAppResult.SAFE

        coEvery { api.scanDomain(DomainScanRequest(url)) } returns domainResponse
        every { mapper.mapToDomain(domainResponse) } returns expectedResult

        val result = repository.verifyDAppDomain(domainData)

        Truth.assertThat(result).isEqualTo(expectedResult)
        coVerify { api.scanDomain(DomainScanRequest(url)) }
        verify { mapper.mapToDomain(domainResponse) }
    }

    @Test
    fun `when verify evm transaction then calls scan json rpc and maps result`() = runTest {
        val data = TransactionData(
            chain = "ethereum",
            accountAddress = "0xabc",
            domainUrl = "https://uniswap.org",
            method = "eth_sendTransaction",
            params = TransactionParams.Evm(params = "some-params"),
        )

        val request = mockk<EvmTransactionScanRequest>()
        val response = mockk<TransactionScanResponse>()
        val expectedResult = mockk<CheckTransactionResult>()

        every { mapper.mapToEvmRequest(data) } returns request
        coEvery { api.scanJsonRpc(request) } returns response
        every { mapper.mapToDomain(response) } returns expectedResult

        val result = repository.verifyTransaction(data)

        Truth.assertThat(result).isEqualTo(expectedResult)
        coVerify { api.scanJsonRpc(request) }
        verify { mapper.mapToEvmRequest(data) }
        verify { mapper.mapToDomain(response) }
    }

    @Test
    fun `when verify solana transaction then calls scan solana message and maps result`() = runTest {
        val data = TransactionData(
            chain = "mainnet",
            accountAddress = "/Rd2TLl...",
            domainUrl = "https://example.com",
            method = "signTransaction",
            params = TransactionParams.Solana(transactions = listOf("TX_PAYLOAD_BASE64")),
        )

        val request = mockk<SolanaTransactionScanRequest>()
        val response = mockk<SolanaTransactionResponse>()
        val expectedResult = mockk<CheckTransactionResult>()

        every { mapper.mapToSolanaRequest(data) } returns request
        coEvery { api.scanSolanaMessage(request) } returns response
        every { mapper.mapToDomain(response) } returns expectedResult

        val result = repository.verifyTransaction(data)

        Truth.assertThat(result).isEqualTo(expectedResult)
        coVerify { api.scanSolanaMessage(request) }
        verify { mapper.mapToSolanaRequest(data) }
        verify { mapper.mapToDomain(response) }
    }
}