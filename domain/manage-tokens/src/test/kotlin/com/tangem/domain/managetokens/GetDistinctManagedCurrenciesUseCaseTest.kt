package com.tangem.domain.managetokens

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.pagination.Batch
import org.junit.Test
import java.util.UUID
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest

class GetDistinctManagedTokensUseCaseTest {

    private val useCase = GetDistinctManagedCurrenciesUseCase(TestingCoroutineDispatcherProvider())

    @Test
    fun `GIVEN generated batch with multiple duplicates WHEN invoke THEN batches with unique manage currencies`() =
        runTest {
            // GIVEN
            val totalBatches = 10
            val batchSize = 20
            val duplicatePerBatch = 5
            val uniquePerBatch = batchSize - duplicatePerBatch
            val globalTokens = mutableListOf<ManagedCryptoCurrency>()
            val batches = mutableListOf<Batch<Int, List<ManagedCryptoCurrency>>>()

            repeat(totalBatches) { batchIndex ->
                val duplicates = if (batchIndex == 0) {
                    emptyList()
                } else {
                    globalTokens.take(duplicatePerBatch)
                }

                val newTokens = List(uniquePerBatch) {
                    val token = testToken(id = "token_${batchIndex}_$it")
                    globalTokens += token
                    token
                }

                val batch = Batch(batchIndex, duplicates + newTokens)
                batches += batch
            }

            // WHEN
            val result = useCase(batches)

            // THEN
            val seenIds = mutableSetOf<ManagedCryptoCurrency.ID>()

            result.forEach { batch ->
                batch.data.forEach { token ->
                    assertThat(seenIds).doesNotContain(token.id)
                    seenIds.add(token.id)
                }
            }

            val totalTokens = result.sumOf { it.data.size }
            assertThat(totalTokens).isEqualTo(totalBatches * (batchSize - duplicatePerBatch))
        }

    @Test
    fun `GIVEN static batch with multiple duplicates WHEN invoke THEN batches with unique manage currencies`() =
        runTest {
            // GIVEN
            val token1 = testToken(id = "A", name = "Delta")
            val token2 = testToken(id = "B", name = "Delta")
            val token3 = testToken(id = "C", name = "Delta")
            val token4 = testToken(id = "D", name = "Delta")
            val token5 = testToken(id = "E", name = "Delta")
            val token6 = testToken(id = "F", name = "Delta")
            val token7 = testToken(id = "G", name = "Delta")
            val token8 = testToken(id = "H", name = "Delta")
            val token9 = testToken(id = "I", name = "Delta")
            val token10 = testToken(id = "J", name = "Delta")

            val batches = listOf(
                Batch(0, listOf(token1, token2, token3, token4, token5)),
                Batch(1, listOf(token1, token7, token8, token9, token6)),
                Batch(2, listOf(token2, token8, token8, token6, token10)),
            )

            // WHEN
            val result = useCase(batches)

            // THEN
            val seenIds = mutableSetOf<ManagedCryptoCurrency.ID>()

            result.forEach { batch ->
                batch.data.forEach { token ->
                    assertThat(seenIds).doesNotContain(token.id)
                    seenIds.add(token.id)
                }
            }

            val totalTokens = result.sumOf { it.data.size }
            assertThat(totalTokens).isEqualTo(10)
        }
}

fun testToken(
    id: String = UUID.randomUUID().toString(),
    name: String = "Test Token",
    symbol: String = "TTK",
): ManagedCryptoCurrency {
    return ManagedCryptoCurrency.Token(
        id = ManagedCryptoCurrency.ID(id),
        name = name,
        symbol = symbol,
        iconUrl = "null",
        availableNetworks = emptyList(),
        addedIn = emptySet(),
    )
}