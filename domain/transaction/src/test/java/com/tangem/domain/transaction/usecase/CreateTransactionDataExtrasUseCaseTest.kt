package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class CreateTransactionDataExtrasUseCaseTest {

    private val repository: TransactionRepository = mockk()
    private val network: Network = mockk(relaxed = true)
    private val useCase = CreateTransactionDataExtrasUseCase(repository)

    @Test
    fun `GIVEN well-formed call data WHEN invoke THEN bytes are decoded as sent`() {
        val callData = slot<SmartContractCallData>()
        every {
            repository.createTransactionDataExtras(capture(callData), network, null, null)
        } returns mockk<TransactionExtras>()

        val result = useCase(data = "0xa9059cbb", network = network)

        assertThat(result.isRight()).isTrue()
        assertThat((callData.captured as CompiledSmartContractCallData).data)
            .isEqualTo(byteArrayOf(0xa9.toByte(), 0x05, 0x9c.toByte(), 0xbb.toByte()))
    }

    @Test
    fun `GIVEN odd-length call data WHEN invoke THEN Left instead of a truncated payload`() {
        val result = useCase(data = "0xa9059cb", network = network)

        assertThat(result.isLeft()).isTrue()
        verify(exactly = 0) { repository.createTransactionDataExtras(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN non-hex call data WHEN invoke THEN Left`() {
        val result = useCase(data = "0xzz", network = network)

        assertThat(result.isLeft()).isTrue()
        verify(exactly = 0) { repository.createTransactionDataExtras(any(), any(), any(), any()) }
    }
}
