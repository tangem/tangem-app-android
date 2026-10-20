package com.tangem.grow.datasource.stakekit

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO.BalanceTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.TokenDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardClaimingDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.MetadataDTO.RewardScheduleDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.RewardTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionStatusDTO
import com.tangem.grow.datasource.stakekit.models.response.model.transaction.StakingTransactionTypeDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.lang.reflect.Type

internal class StakeKitEnumFallbackAdaptersTest {

    private val moshi: Moshi = Moshi.Builder().addStakeKitEnumFallbackAdapters().build()
    private val plainMoshi: Moshi = Moshi.Builder().build()
    private val dtoMoshi: Moshi = legacyDataStoreMoshi.newBuilder().addStakeKitEnumFallbackAdapters().build()

    private fun decode(moshi: Moshi, enumClass: Class<out Enum<*>>, json: String): Enum<*>? =
        moshi.adapter<Enum<*>>(enumClass as Type).fromJson(json)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KnownValues {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN known json name WHEN decoded with fallback adapters THEN maps to constant`(model: KnownModel) {
            // Act
            val actual = decode(moshi, model.enumClass, model.json)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            KnownModel(BalanceTypeDTO::class.java, "\"staked\"", BalanceTypeDTO.STAKED),
            KnownModel(NetworkTypeDTO::class.java, "\"avalanche-c\"", NetworkTypeDTO.AVALANCHE_C),
            KnownModel(NetworkTypeDTO::class.java, "\"binance\"", NetworkTypeDTO.BINANCE),
            KnownModel(NetworkTypeDTO::class.java, "\"ethereum-holesky\"", NetworkTypeDTO.ETHEREUM_HOLESKY),
            KnownModel(RewardClaimingDTO::class.java, "\"manual\"", RewardClaimingDTO.MANUAL),
            KnownModel(RewardScheduleDTO::class.java, "\"epoch\"", RewardScheduleDTO.EPOCH),
            KnownModel(RewardTypeDTO::class.java, "\"apy\"", RewardTypeDTO.APY),
            KnownModel(
                StakingActionStatusDTO::class.java,
                "\"WAITING_FOR_NEXT\"",
                StakingActionStatusDTO.WAITING_FOR_NEXT,
            ),
            KnownModel(StakingActionTypeDTO::class.java, "\"VOTE_LOCKED\"", StakingActionTypeDTO.VOTE_LOCKED),
            KnownModel(
                StakingTransactionStatusDTO::class.java,
                "\"BROADCASTED\"",
                StakingTransactionStatusDTO.BROADCASTED,
            ),
            KnownModel(
                StakingTransactionTypeDTO::class.java,
                "\"UNFREEZE_LEGACY_ENERGY\"",
                StakingTransactionTypeDTO.UNFREEZE_LEGACY_ENERGY,
            ),
            KnownModel(ValidatorStatusDTO::class.java, "\"jailed\"", ValidatorStatusDTO.JAILED),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnknownValues {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN unknown json string WHEN decoded with fallback adapters THEN maps to UNKNOWN`(model: UnknownModel) {
            // Act
            val actual = decode(moshi, model.enumClass, "\"brand-new-value\"")

            // Assert
            assertThat(actual).isEqualTo(model.unknown)
        }

        // Pins current behaviour: fallback adapters are not null-safe, JSON null fails the whole decode
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN json null WHEN decoded with fallback adapters THEN throws JsonDataException`(model: UnknownModel) {
            // Act
            val error = runCatching { decode(moshi, model.enumClass, "null") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Expected a string but was NULL at path $")
        }

        private fun provideTestModels() = listOf(
            UnknownModel(BalanceTypeDTO::class.java, BalanceTypeDTO.UNKNOWN),
            UnknownModel(NetworkTypeDTO::class.java, NetworkTypeDTO.UNKNOWN),
            UnknownModel(RewardClaimingDTO::class.java, RewardClaimingDTO.UNKNOWN),
            UnknownModel(RewardScheduleDTO::class.java, RewardScheduleDTO.UNKNOWN),
            UnknownModel(RewardTypeDTO::class.java, RewardTypeDTO.UNKNOWN),
            UnknownModel(StakingActionStatusDTO::class.java, StakingActionStatusDTO.UNKNOWN),
            UnknownModel(StakingActionTypeDTO::class.java, StakingActionTypeDTO.UNKNOWN),
            UnknownModel(StakingTransactionStatusDTO::class.java, StakingTransactionStatusDTO.UNKNOWN),
            UnknownModel(StakingTransactionTypeDTO::class.java, StakingTransactionTypeDTO.UNKNOWN),
            UnknownModel(ValidatorStatusDTO::class.java, ValidatorStatusDTO.UNKNOWN),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CaseAndConstantNames {

        @Test
        fun `GIVEN kotlin constant name instead of json name WHEN decoded THEN maps to UNKNOWN`() {
            // Act
            val actual = decode(moshi, NetworkTypeDTO::class.java, "\"AVALANCHE_C\"")

            // Assert
            assertThat(actual).isEqualTo(NetworkTypeDTO.UNKNOWN)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NestedDtos {

        @Test
        fun `GIVEN validator with unknown status WHEN decoded THEN status is UNKNOWN and rest kept`() {
            // Arrange
            val json = """{"address":"v1","status":"probation","name":"Node","preferred":true}"""

            // Act
            val actual = dtoMoshi.adapter(YieldDTO.ValidatorDTO::class.java).fromJson(json)

            // Assert
            val expected = YieldDTO.ValidatorDTO(
                address = "v1",
                status = ValidatorStatusDTO.UNKNOWN,
                name = "Node",
                image = null,
                website = null,
                apr = null,
                commission = null,
                stakedBalance = null,
                votingPower = null,
                preferred = true,
            )
            assertThat(actual).isEqualTo(expected)
        }

        // Pins current behaviour: fallback adapters are not null-safe, JSON null fails the whole decode
        @Test
        fun `GIVEN validator with null status WHEN decoded THEN throws JsonDataException`() {
            // Arrange
            val json = """{"address":"v1","status":null,"preferred":true}"""

            // Act
            val error = runCatching { dtoMoshi.adapter(YieldDTO.ValidatorDTO::class.java).fromJson(json) }
                .exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(JsonDataException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Expected a string but was NULL at path $.status")
        }

        @Test
        fun `GIVEN token with unknown network WHEN decoded THEN network is UNKNOWN`() {
            // Arrange
            val json = """{"name":"Bitcoin","network":"bitcoin","symbol":"BTC","decimals":8}"""

            // Act
            val actual = dtoMoshi.adapter(TokenDTO::class.java).fromJson(json)

            // Assert
            val expected = TokenDTO(
                name = "Bitcoin",
                network = NetworkTypeDTO.UNKNOWN,
                symbol = "BTC",
                decimals = 8,
                address = null,
                coinGeckoId = null,
                logoURI = null,
                isPoints = null,
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SerialNames {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN serializable stakekit enum WHEN encoded by moshi and kotlinx THEN json names are equal`(
            model: SerialNameModel,
        ) {
            // Arrange
            val moshiAdapter = plainMoshi.adapter<Enum<*>>(model.entry.declaringJavaClass as Type)

            // Act
            val moshiJson = moshiAdapter.toJson(model.entry)

            @Suppress("UNCHECKED_CAST")
            val kotlinxJson = Json.encodeToString(model.serializer as KSerializer<Enum<*>>, model.entry)

            // Assert
            assertThat(moshiJson).isEqualTo(kotlinxJson)
        }

        @ParameterizedTest
        @EnumSource(NetworkTypeDTO::class)
        fun `GIVEN every network json name WHEN decoded with fallback adapters THEN round trips to the same constant`(
            entry: NetworkTypeDTO,
        ) {
            // Arrange
            val json = plainMoshi.adapter(NetworkTypeDTO::class.java).toJson(entry)

            // Act
            val actual = moshi.adapter(NetworkTypeDTO::class.java).fromJson(json)

            // Assert
            assertThat(actual).isEqualTo(entry)
        }

        private fun provideTestModels(): List<SerialNameModel> =
            NetworkTypeDTO.entries.map { SerialNameModel(it, NetworkTypeDTO.serializer()) } +
                BalanceTypeDTO.entries.map { SerialNameModel(it, BalanceTypeDTO.serializer()) } +
                ValidatorStatusDTO.entries.map { SerialNameModel(it, ValidatorStatusDTO.serializer()) } +
                RewardScheduleDTO.entries.map { SerialNameModel(it, RewardScheduleDTO.serializer()) } +
                RewardClaimingDTO.entries.map { SerialNameModel(it, RewardClaimingDTO.serializer()) } +
                RewardTypeDTO.entries.map { SerialNameModel(it, RewardTypeDTO.serializer()) } +
                StakingActionTypeDTO.entries.map { SerialNameModel(it, StakingActionTypeDTO.serializer()) }
    }

    internal data class KnownModel(val enumClass: Class<out Enum<*>>, val json: String, val expected: Enum<*>)

    internal data class UnknownModel(val enumClass: Class<out Enum<*>>, val unknown: Enum<*>)

    internal data class SerialNameModel(val entry: Enum<*>, val serializer: KSerializer<out Enum<*>>)
}