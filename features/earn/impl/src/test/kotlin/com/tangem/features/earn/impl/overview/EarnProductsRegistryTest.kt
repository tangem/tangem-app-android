package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnProductsRegistryTest {

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)

    private val yieldSource = TestRateSource(id = EarnProductId("yield"), rank = 5)
    private val bonusSource = TestRateSource(id = EarnProductId("bonus"), rank = 3)
    private val infoSource = TestRateSource(id = EarnProductId("info"), rank = 7, badgeAccent = EarnBadgeAccent.Info)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Construction {

        @Test
        fun `GIVEN products inserted out of rank order WHEN registry created THEN products are ordered by rank`() {
            // Act
            val registry = EarnProductsRegistry(products = setOf(vault, staking, lending), rateSources = emptySet())

            // Assert
            assertThat(registry.products).containsExactly(lending, staking, vault).inOrder()
        }

        @Test
        fun `GIVEN rate sources out of rank order WHEN registry created THEN rate sources are ordered by rank`() {
            // Act
            val registry = EarnProductsRegistry(products = emptySet(), rateSources = setOf(yieldSource, bonusSource))

            // Assert
            assertThat(registry.rateSources).containsExactly(bonusSource, yieldSource).inOrder()
        }

        @Test
        fun `GIVEN products whose rank order is the reverse of id order WHEN registry created THEN ordered by rank`() {
            // Arrange
            val alpha = RankedProduct(id = EarnProductId("alpha"), rank = 9)
            val zeta = RankedProduct(id = EarnProductId("zeta"), rank = 1)

            // Act
            val registry = EarnProductsRegistry(products = setOf(alpha, zeta), rateSources = emptySet())

            // Assert
            assertThat(registry.products).containsExactly(zeta, alpha).inOrder()
        }

        @Test
        fun `GIVEN rate sources whose rank order is the reverse of id order WHEN registry created THEN by rank`() {
            // Arrange
            val alpha = TestRateSource(id = EarnProductId("alpha"), rank = 9)
            val zeta = TestRateSource(id = EarnProductId("zeta"), rank = 1)

            // Act
            val registry = EarnProductsRegistry(products = emptySet(), rateSources = setOf(alpha, zeta))

            // Assert
            assertThat(registry.rateSources).containsExactly(zeta, alpha).inOrder()
        }

        @Test
        fun `GIVEN staking without session and lending WHEN registry created THEN both products are accepted`() {
            // Act
            val registry = EarnProductsRegistry(products = setOf(staking, lending), rateSources = emptySet())

            // Assert
            assertThat(registry.products).containsExactly(lending, staking).inOrder()
        }

        @Test
        fun `GIVEN no products and no rate sources WHEN registry created THEN both lists are empty`() {
            // Act
            val registry = EarnProductsRegistry(products = emptySet(), rateSources = emptySet())

            // Assert
            assertThat(registry.products).isEmpty()
            assertThat(registry.rateSources).isEmpty()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN duplicate ids WHEN registry created THEN IllegalArgumentException is thrown`(
            model: DuplicateIdsModel,
        ) {
            // Act
            val actual = runCatching {
                EarnProductsRegistry(products = model.products, rateSources = model.rateSources)
            }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(actual).hasMessageThat().contains("Duplicate earn product ids")
        }

        private fun provideTestModels() = listOf(
            DuplicateIdsModel(
                name = "two products of one preset",
                products = setOf(lending, RankedProduct(id = lending.id, rank = 0)),
                rateSources = emptySet(),
            ),
            DuplicateIdsModel(
                name = "a product and a rate source",
                products = setOf(lending),
                rateSources = setOf(TestRateSource(id = lending.id, rank = 7)),
            ),
            DuplicateIdsModel(
                name = "two rate sources",
                products = emptySet(),
                rateSources = setOf(yieldSource, TestRateSource(id = yieldSource.id, rank = 9)),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Find {

        private val registry = EarnProductsRegistry(
            products = setOf(vault, staking, lending),
            rateSources = setOf(yieldSource),
        )

        @Test
        fun `GIVEN several products WHEN find by id THEN the matching product is returned`() {
            // Act
            val actual = registry.find(staking.id)

            // Assert
            assertThat(actual).isEqualTo(staking)
        }

        @Test
        fun `GIVEN unknown id WHEN find THEN null is returned`() {
            // Act
            val actual = registry.find(EarnProductId("unknown"))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN id of a rate source WHEN find THEN null is returned`() {
            // Act
            val actual = registry.find(yieldSource.id)

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RankOf {

        private val registry = EarnProductsRegistry(
            products = setOf(vault, staking, lending),
            rateSources = setOf(yieldSource),
        )

        @Test
        fun `GIVEN unknown id WHEN rankOf THEN Int MAX_VALUE is returned`() {
            // Act
            val actual = registry.rankOf(EarnProductId("unknown"))

            // Assert
            assertThat(actual).isEqualTo(Int.MAX_VALUE)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN registered id WHEN rankOf THEN its rank is returned`(model: RankModel) {
            // Act
            val actual = registry.rankOf(model.id)

            // Assert
            assertThat(actual).isEqualTo(model.expectedRank)
        }

        private fun provideTestModels() = listOf(
            RankModel(id = lending.id, expectedRank = 0),
            RankModel(id = staking.id, expectedRank = 1),
            RankModel(id = vault.id, expectedRank = 2),
            RankModel(id = yieldSource.id, expectedRank = 5),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BadgeAccentOf {

        private val registry = EarnProductsRegistry(
            products = setOf(vault, staking, lending),
            rateSources = setOf(yieldSource, infoSource),
        )

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN registered id WHEN badgeAccentOf THEN the accent of that product or rate source is returned`(
            model: AccentModel,
        ) {
            // Act
            val actual = registry.badgeAccentOf(model.id)

            // Assert
            assertThat(actual).isEqualTo(model.expectedAccent)
        }

        private fun provideTestModels() = listOf(
            AccentModel(id = lending.id, expectedAccent = EarnBadgeAccent.Success),
            AccentModel(id = staking.id, expectedAccent = EarnBadgeAccent.Info),
            AccentModel(id = vault.id, expectedAccent = EarnBadgeAccent.Success),
            AccentModel(id = yieldSource.id, expectedAccent = EarnBadgeAccent.Success),
            AccentModel(id = infoSource.id, expectedAccent = EarnBadgeAccent.Info),
        )

        @Test
        fun `GIVEN an id that is in neither list WHEN badgeAccentOf THEN IllegalStateException names the id`() {
            // Act
            val actual = runCatching { registry.badgeAccentOf(EarnProductId("unknown")) }.exceptionOrNull()

            // Assert
            assertThat(actual).isInstanceOf(IllegalStateException::class.java)
            assertThat(actual).hasMessageThat().contains("unknown")
        }
    }

    internal data class AccentModel(val id: EarnProductId, val expectedAccent: EarnBadgeAccent) {
        override fun toString(): String = "${id.value} -> $expectedAccent"
    }

    internal data class DuplicateIdsModel(
        val name: String,
        val products: Set<EarnProduct>,
        val rateSources: Set<EarnRateSource>,
    ) {
        override fun toString(): String = name
    }

    internal data class RankModel(val id: EarnProductId, val expectedRank: Int) {
        override fun toString(): String = "${id.value} -> $expectedRank"
    }
}

private class RankedProduct(
    override val id: EarnProductId,
    override val rank: Int,
    delegate: EarnProduct = createFakeEarnProduct(FakeEarnProductPreset.Lending),
) : EarnProduct by delegate

private class TestRateSource(
    override val id: EarnProductId,
    override val rank: Int,
    override val badgeAccent: EarnBadgeAccent = EarnBadgeAccent.Success,
) : EarnRateSource {

    override fun rates(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnRateEntry>> = emptyFlow()
}