package com.tangem.data.staking.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.AddressArgument
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.Yield.Validator.ValidatorStatus
import com.tangem.grow.datasource.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.store.datasource.txhistory.db.entity.staking.StakingValidatorEntity
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class YieldConverterTest {

    @Test
    fun `GIVEN validator without name WHEN convert THEN name falls back to a placeholder`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = null))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().name).isEqualTo("No name")
    }

    @Test
    fun `GIVEN validator with name WHEN convert THEN name is kept`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = "Tangem"))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().name).isEqualTo("Tangem")
    }

    @Test
    fun `GIVEN nameless validator WHEN convertListIgnoreErrors THEN yield is not dropped`() {
        // Arrange
        val yieldDTO = createYieldDTO(
            createValidatorDTO(address = "pool1", name = null),
            createValidatorDTO(address = "pool2", name = "Named pool"),
        )

        // Act
        val actual = YieldConverter.convertListIgnoreErrors(listOf(yieldDTO))

        // Assert
        assertThat(actual).hasSize(1)
        assertThat(actual.single().validators.map { it.name }).containsExactly("No name", "Named pool")
    }

    @Test
    fun `GIVEN partner name WHEN convert THEN validator is a strategic partner`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = POOL_ADDRESS, name = "Meria"))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().isStrategicPartner).isTrue()
    }

    @Test
    fun `GIVEN validator without preferred flag WHEN convert THEN validator is not preferred`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(preferred = null))

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.single().isPreferred).isFalse()
    }

    @Test
    fun `GIVEN active and jailed validators WHEN convert THEN only active are selectable but all are resolvable`() {
        // Arrange
        val yieldDTO = createYieldDTO(
            createValidatorDTO(address = "active", status = ValidatorStatusDTO.ACTIVE),
            createValidatorDTO(address = "jailed", status = ValidatorStatusDTO.JAILED),
            createValidatorDTO(address = "inactive", status = ValidatorStatusDTO.INACTIVE),
        )

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.validators.map { it.address }).containsExactly("active")
        assertThat(actual.allValidators.map { it.address }).containsExactly("active", "jailed", "inactive")
    }

    @Test
    fun `GIVEN jailed validator without address WHEN convert THEN it is skipped`() {
        // Arrange
        val yieldDTO = createYieldDTO(
            createValidatorDTO(address = "active", status = ValidatorStatusDTO.ACTIVE),
            createValidatorDTO(address = null, status = ValidatorStatusDTO.JAILED),
        )

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        assertThat(actual.allValidators.map { it.address }).containsExactly("active")
    }

    @Test
    fun `GIVEN validator with all optional fields WHEN convert THEN they are kept`() {
        // Arrange
        val yieldDTO = createYieldDTO(
            createValidatorDTO(
                name = "Name",
                preferred = true,
                apr = BigDecimal("0.09"),
                commission = 0.1,
                image = "img",
                website = "web",
                stakedBalance = "100",
                votingPower = 0.5,
            ),
        )

        // Act
        val actual = YieldConverter.convert(yieldDTO)

        // Assert
        val expected = Yield.Validator(
            address = POOL_ADDRESS,
            status = ValidatorStatus.ACTIVE,
            name = "Name",
            image = "img",
            website = "web",
            rewardInfo = RewardInfo(BigDecimal("0.10000000"), RewardType.UNKNOWN),
            commission = 0.1,
            stakedBalance = "100",
            votingPower = 0.5,
            isPreferred = true,
            isStrategicPartner = false,
        )
        assertThat(actual.validators).containsExactly(expected)
    }

    @Test
    fun `GIVEN available yield with enabled flags WHEN convert THEN availability status and flags mapped`() {
        // Act
        val actual = YieldConverter.convert(MockYieldDTOFactory.create())

        // Assert
        assertThat(actual.isAvailable).isTrue()
        assertThat(actual.status).isEqualTo(Yield.Status(canEnter = true, canExit = true))
        assertThat(actual.metadata.revshare).isEqualTo(Yield.Metadata.Enabled(isEnabled = true))
    }

    @Test
    fun `GIVEN metadata with optional fields WHEN convert THEN optional fields kept`() {
        // Arrange
        val yieldDTO = withMetadata {
            it.copy(
                documentation = "docs",
                defaultValidator = "validator",
                minimumStake = 5,
                supportsMultipleValidators = true,
            )
        }
        val withoutOptionalFields = YieldConverter.convert(MockYieldDTOFactory.create()).metadata

        // Act
        val actual = YieldConverter.convert(yieldDTO).metadata

        // Assert
        val expected = withoutOptionalFields.copy(
            documentation = "docs",
            defaultValidator = "validator",
            minimumStake = 5,
            isMultipleValidatorsSupported = true,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN validator without address WHEN convert THEN error is thrown`() {
        // Arrange
        val yieldDTO = createYieldDTO(createValidatorDTO(address = null, name = "Named pool"))

        // Act
        val actual = runCatching { YieldConverter.convert(yieldDTO) }.exceptionOrNull()

        // Assert
        assertThat(actual).hasMessageThat().isEqualTo("address must not be null")
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RewardRate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN apr and commission WHEN convert THEN gross rate computed`(model: RewardModel) {
            // Arrange
            val yieldDTO = createYieldDTO(
                YieldDTO.RewardTypeDTO.APR,
                createValidatorDTO(apr = model.apr, commission = model.commission),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO).validators.single()

            // Assert
            val expectedRewardInfo = model.expectedRate?.let { RewardInfo(rate = it, type = RewardType.APR) }
            assertThat(actual.rewardInfo).isEqualTo(expectedRewardInfo)
            assertThat(actual.commission).isEqualTo(model.commission)
        }

        @Test
        fun `GIVEN apy yield WHEN convert THEN validator reward type is APY`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                YieldDTO.RewardTypeDTO.APY,
                createValidatorDTO(apr = BigDecimal("0.05")),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.validators.single().rewardInfo).isEqualTo(RewardInfo(BigDecimal("0.05"), RewardType.APY))
            assertThat(actual.rewardType).isEqualTo(RewardType.APY)
        }

        private fun provideTestModels() = listOf(
            RewardModel(BigDecimal("0.09"), 0.1, BigDecimal("0.10000000")),
            RewardModel(BigDecimal("0.05"), null, BigDecimal("0.05")),
            RewardModel(null, 0.1, null),
            RewardModel(BigDecimal("0.05"), 1.0, BigDecimal("0.05")),
            RewardModel(BigDecimal("0.05"), 1.5, BigDecimal("0.05")),
            RewardModel(BigDecimal("0.05"), 0.0, BigDecimal("0.05000000")),
            RewardModel(BigDecimal("0.05"), -0.25, BigDecimal("0.04000000")),
            RewardModel(BigDecimal("0.05"), 0.07, BigDecimal("0.05376344")),
            RewardModel(BigDecimal("0.05"), Double.NaN, BigDecimal("0.05")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sorting {

        @Test
        fun `GIVEN validators with different rates WHEN convert THEN sorted by rate descending with null last`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                createValidatorDTO(address = "low", apr = BigDecimal("0.05")),
                createValidatorDTO(address = "none", apr = null),
                createValidatorDTO(address = "high", apr = BigDecimal("0.10")),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.validators.map { it.address }).containsExactly("high", "low", "none").inOrder()
        }

        @Test
        fun `GIVEN two partners WHEN convert THEN partners keep rate order between them`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                createValidatorDTO(address = "other", apr = BigDecimal("0.50")),
                createValidatorDTO(address = COSMOS_PARTNER, apr = BigDecimal("0.01")),
                createValidatorDTO(address = "meria-node", name = "Meria", apr = BigDecimal("0.02")),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.validators.map { it.address })
                .containsExactly("meria-node", COSMOS_PARTNER, "other")
                .inOrder()
        }

        @Test
        fun `GIVEN duplicate addresses WHEN convert THEN first occurrence kept`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                createValidatorDTO(address = "dup", name = "First"),
                createValidatorDTO(address = "dup", name = "Second"),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.allValidators.single().name).isEqualTo("First")
        }

        @Test
        fun `GIVEN sorted validators WHEN convert THEN validators keep allValidators order`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                createValidatorDTO(address = "a", status = ValidatorStatusDTO.ACTIVE, apr = BigDecimal("0.01")),
                createValidatorDTO(address = "b", status = ValidatorStatusDTO.JAILED, apr = BigDecimal("0.20")),
                createValidatorDTO(address = "c", status = ValidatorStatusDTO.ACTIVE, apr = BigDecimal("0.10")),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.allValidators.map { it.address }).containsExactly("b", "c", "a").inOrder()
            assertThat(actual.validators.map { it.address }).containsExactly("c", "a").inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StrategicPartners {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN validator address and name WHEN convert THEN strategic partner flag resolved`(model: PartnerModel) {
            // Arrange
            val yieldDTO = createYieldDTO(createValidatorDTO(address = model.address, name = model.name))

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.validators.single().isStrategicPartner).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PartnerModel(SOLANA_PARTNER, null, true),
            PartnerModel(COSMOS_PARTNER, null, true),
            PartnerModel("pool", "Meria", true),
            PartnerModel("pool", "MERIA", true),
            PartnerModel("pool", "Meria Pool", false),
            PartnerModel(SOLANA_PARTNER.lowercase(), null, false),
            PartnerModel("pool", null, false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EnumMapping {

        @ParameterizedTest
        @EnumSource(ValidatorStatusDTO::class)
        fun `GIVEN validator status dto WHEN convert THEN domain status has the same name`(dto: ValidatorStatusDTO) {
            // Arrange
            val yieldDTO = createYieldDTO(createValidatorDTO(address = "v1", status = dto))

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.allValidators.single().status.name).isEqualTo(dto.name)
        }

        @Test
        fun `GIVEN validator without status WHEN convert THEN status UNKNOWN and not selectable`() {
            // Arrange
            val yieldDTO = createYieldDTO(createValidatorDTO(address = "v1", status = null))

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.allValidators.single().status).isEqualTo(ValidatorStatus.UNKNOWN)
            assertThat(actual.validators).isEmpty()
        }

        @ParameterizedTest
        @EnumSource(YieldDTO.MetadataDTO.RewardScheduleDTO::class)
        fun `GIVEN reward schedule dto WHEN convert THEN domain schedule has the same name`(
            dto: YieldDTO.MetadataDTO.RewardScheduleDTO,
        ) {
            // Arrange
            val yieldDTO = withMetadata { it.copy(rewardSchedule = dto) }

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.metadata.rewardSchedule.name).isEqualTo(dto.name)
        }

        @ParameterizedTest
        @EnumSource(YieldDTO.MetadataDTO.RewardClaimingDTO::class)
        fun `GIVEN reward claiming dto WHEN convert THEN domain claiming has the same name`(
            dto: YieldDTO.MetadataDTO.RewardClaimingDTO,
        ) {
            // Arrange
            val yieldDTO = withMetadata { it.copy(rewardClaiming = dto) }

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.metadata.rewardClaiming.name).isEqualTo(dto.name)
        }

        @ParameterizedTest
        @EnumSource(YieldDTO.RewardTypeDTO::class)
        fun `GIVEN reward type dto WHEN convert THEN domain reward type has the same name`(
            dto: YieldDTO.RewardTypeDTO,
        ) {
            // Arrange
            val yieldDTO = MockYieldDTOFactory.create().copy(rewardType = dto)

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.rewardType.name).isEqualTo(dto.name)
        }

        // Pins current behaviour: FULL validators never reach validators, so the flag means no active ones
        @Test
        fun `GIVEN only full validators WHEN convert THEN none selectable and allValidatorsFull true`() {
            // Arrange
            val yieldDTO = createYieldDTO(
                createValidatorDTO(address = "f1", status = ValidatorStatusDTO.FULL),
                createValidatorDTO(address = "f2", status = ValidatorStatusDTO.FULL),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.validators).isEmpty()
            assertThat(actual.allValidators).hasSize(2)
            assertThat(actual.areAllValidatorsFull).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Args {

        @Test
        fun `GIVEN amount and address args WHEN convert THEN keys mapped and values kept`() {
            // Arrange
            val yieldDTO = withArgs(
                enter = enterDTO(
                    address = AddressArgumentDTO(required = true, network = "solana"),
                    args = mapOf(
                        "amount" to AddressArgumentDTO(
                            required = true,
                            minimum = BigDecimal("0.01"),
                            maximum = BigDecimal("-1"),
                        ),
                        "address" to AddressArgumentDTO(required = false),
                    ),
                ),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO).args

            // Assert
            val expected = Yield.Args(
                enter = Yield.Args.Enter(
                    addresses = Yield.Args.Enter.Addresses(
                        address = AddressArgument(isRequired = true, network = "solana"),
                        additionalAddresses = null,
                    ),
                    args = mapOf(
                        Yield.Args.ArgType.AMOUNT to AddressArgument(
                            isRequired = true,
                            minimum = BigDecimal("0.01"),
                            maximum = BigDecimal("-1"),
                        ),
                        Yield.Args.ArgType.ADDRESS to AddressArgument(isRequired = false),
                    ),
                ),
                exit = null,
            )
            assertThat(actual).isEqualTo(expected)
            assertThat(actual.enter.isPartialAmountDisabled).isTrue()
        }

        @Test
        fun `GIVEN additional address with known key WHEN convert THEN key mapped`() {
            // Arrange
            val yieldDTO = withArgs(
                enter = YieldDTO.ArgsDTO.Enter(
                    addresses = YieldDTO.ArgsDTO.Enter.Addresses(
                        address = AddressArgumentDTO(true),
                        additionalAddresses = mapOf("address" to AddressArgumentDTO(required = false)),
                    ),
                    args = emptyMap(),
                ),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            val expected = mapOf(Yield.Args.ArgType.ADDRESS to AddressArgument(isRequired = false))
            assertThat(actual.args.enter.addresses.additionalAddresses).isEqualTo(expected)
        }

        // Pins current behaviour: unknown arg keys overwrite each other
        @Test
        fun `GIVEN unknown arg keys WHEN convert THEN they collapse into one UNKNOWN key`() {
            // Arrange
            val yieldDTO = withArgs(
                enter = enterDTO(
                    args = mapOf(
                        "validatorAddress" to AddressArgumentDTO(required = true, minimum = BigDecimal.ONE),
                        "duration" to AddressArgumentDTO(required = false, minimum = BigDecimal("2")),
                    ),
                ),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            val expected = mapOf(
                Yield.Args.ArgType.UNKNOWN to AddressArgument(isRequired = false, minimum = BigDecimal("2")),
            )
            assertThat(actual.args.enter.args).isEqualTo(expected)
        }

        // Pins current behaviour: unknown arg keys overwrite each other
        @Test
        fun `GIVEN cosmos additional address WHEN convert THEN key becomes UNKNOWN`() {
            // Arrange
            val yieldDTO = withArgs(
                enter = YieldDTO.ArgsDTO.Enter(
                    addresses = YieldDTO.ArgsDTO.Enter.Addresses(
                        address = AddressArgumentDTO(true),
                        additionalAddresses = mapOf(
                            "cosmosPubKey" to AddressArgumentDTO(required = true, network = "cosmos"),
                        ),
                    ),
                    args = emptyMap(),
                ),
            )

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            val expected = mapOf(
                Yield.Args.ArgType.UNKNOWN to AddressArgument(isRequired = true, network = "cosmos"),
            )
            assertThat(actual.args.enter.addresses.additionalAddresses).isEqualTo(expected)
        }

        @Test
        fun `GIVEN exit args WHEN convert THEN exit converted`() {
            // Arrange
            val exit = enterDTO(
                address = AddressArgumentDTO(false),
                args = mapOf("amount" to AddressArgumentDTO(true)),
            )
            val yieldDTO = withArgs(enter = enterDTO(), exit = exit)

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            val expected = Yield.Args.Enter(
                Yield.Args.Enter.Addresses(AddressArgument(false)),
                mapOf(Yield.Args.ArgType.AMOUNT to AddressArgument(true)),
            )
            assertThat(actual.args.exit).isEqualTo(expected)
        }

        @Test
        fun `GIVEN cooldown and warmup periods WHEN convert THEN periods converted`() {
            // Arrange
            val yieldDTO = withMetadata { metadata ->
                metadata.copy(
                    cooldownPeriod = YieldDTO.MetadataDTO.PeriodDTO(days = 7, seconds = 60),
                    warmupPeriod = YieldDTO.MetadataDTO.PeriodDTO(days = 2, seconds = null),
                )
            }

            // Act
            val actual = YieldConverter.convert(yieldDTO)

            // Assert
            assertThat(actual.metadata.cooldownPeriod).isEqualTo(Yield.Metadata.Period(7, 60))
            assertThat(actual.metadata.warmupPeriod).isEqualTo(Yield.Metadata.Period(2, null))
        }

        private fun enterDTO(
            address: AddressArgumentDTO = AddressArgumentDTO(required = false),
            args: Map<String, AddressArgumentDTO> = emptyMap(),
        ) = YieldDTO.ArgsDTO.Enter(
            addresses = YieldDTO.ArgsDTO.Enter.Addresses(address = address),
            args = args,
        )

        private fun withArgs(enter: YieldDTO.ArgsDTO.Enter, exit: YieldDTO.ArgsDTO.Enter? = null): YieldDTO {
            return MockYieldDTOFactory.create().copy(args = YieldDTO.ArgsDTO(enter = enter, exit = exit))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MandatoryFields {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN missing mandatory field WHEN convert THEN IllegalStateException names the field`(
            model: MandatoryModel,
        ) {
            // Arrange
            val yieldDTO = model.mutate(MockYieldDTOFactory.create())

            // Act
            val error = runCatching { YieldConverter.convert(yieldDTO) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo(model.message)
        }

        @Test
        fun `GIVEN valid and invalid yields WHEN convertListIgnoreErrors THEN only valid yield returned`() {
            // Arrange
            val base = MockYieldDTOFactory.create()

            // Act
            val actual = YieldConverter.convertListIgnoreErrors(listOf(base, base.copy(id = null, rewardRate = 2.0)))

            // Assert
            assertThat(actual).hasSize(1)
            assertThat(actual.single().rewardRate).isEqualTo(1.0)
        }

        private fun provideTestModels() = listOf(
            MandatoryModel("id must not be null") { it.copy(id = null) },
            MandatoryModel("token must not be null") { it.copy(token = null) },
            MandatoryModel("tokens must not be null") { it.copy(tokens = null) },
            MandatoryModel("args must not be null") { it.copy(args = null) },
            MandatoryModel("status must not be null") { it.copy(status = null) },
            MandatoryModel("apy must not be null") { it.copy(apy = null) },
            MandatoryModel("rewardRate must not be null") { it.copy(rewardRate = null) },
            MandatoryModel("rewardType must not be null") { it.copy(rewardType = null) },
            MandatoryModel("metadata must not be null") { it.copy(metadata = null) },
            MandatoryModel("validators must not be null") { it.copy(validators = null) },
            MandatoryModel("isAvailable must not be null") { it.copy(isAvailable = null) },
            MandatoryModel("enter must not be null") { it.copy(args = it.args!!.copy(enter = null)) },
            MandatoryModel("args must not be null") {
                it.copy(args = it.args!!.copy(enter = it.args!!.enter!!.copy(args = null)))
            },
            MandatoryModel("addresses must not be null") {
                it.copy(args = it.args!!.copy(enter = it.args!!.enter!!.copy(addresses = null)))
            },
            MandatoryModel("enter must not be null") { it.copy(status = it.status!!.copy(enter = null)) },
            MandatoryModel("name must not be null") { it.copy(metadata = it.metadata!!.copy(name = null)) },
            MandatoryModel("warmupPeriod must not be null") {
                it.copy(metadata = it.metadata!!.copy(warmupPeriod = null))
            },
            MandatoryModel("days must not be null") {
                it.copy(metadata = it.metadata!!.copy(warmupPeriod = it.metadata!!.warmupPeriod!!.copy(days = null)))
            },
            MandatoryModel("rewardSchedule must not be null") {
                it.copy(metadata = it.metadata!!.copy(rewardSchedule = null))
            },
            MandatoryModel("revshare must not be null") { it.copy(metadata = it.metadata!!.copy(revshare = null)) },
            MandatoryModel("enabled must not be null") {
                it.copy(metadata = it.metadata!!.copy(fee = it.metadata!!.fee!!.copy(enabled = null)))
            },
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertFromEntity {

        @Test
        fun `GIVEN full entity WHEN convertFromEntity THEN validator restored with UNKNOWN reward type`() {
            // Arrange
            val entity = createEntity(
                address = "v1",
                status = "JAILED",
                name = "Name",
                image = "img",
                website = "web",
                apr = "0.09",
                commission = 0.1,
                stakedBalance = "100",
                votingPower = 0.5,
                isPreferred = true,
            )

            // Act
            val actual = YieldConverter.convertFromEntity(entity)

            // Assert
            val expected = Yield.Validator(
                address = "v1",
                status = ValidatorStatus.JAILED,
                name = "Name",
                image = "img",
                website = "web",
                rewardInfo = RewardInfo(BigDecimal("0.10000000"), RewardType.UNKNOWN),
                commission = 0.1,
                stakedBalance = "100",
                votingPower = 0.5,
                isPreferred = true,
                isStrategicPartner = false,
            )
            assertThat(actual).isEqualTo(expected)
        }

        // Pins current behaviour: live DTO falls back to "No name", entity falls back to the address
        @Test
        fun `GIVEN partner entity without name WHEN convertFromEntity THEN address used as name and partner kept`() {
            // Act
            val actual = YieldConverter.convertFromEntity(createEntity(address = SOLANA_PARTNER, name = null))

            // Assert
            assertThat(actual.name).isEqualTo(SOLANA_PARTNER)
            assertThat(actual.isStrategicPartner).isTrue()
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        inner class StoredStatus {

            // Pins current behaviour: the stored status is resolved by exact enum name, anything else is UNKNOWN
            @ParameterizedTest
            @ProvideTestModels
            fun `GIVEN stored status name WHEN convertFromEntity THEN status resolved by enum name`(
                model: StatusModel,
            ) {
                // Act
                val actual = YieldConverter.convertFromEntity(createEntity(status = model.stored))

                // Assert
                assertThat(actual.status).isEqualTo(model.expected)
            }

            private fun provideTestModels() = listOf(
                StatusModel("ACTIVE", ValidatorStatus.ACTIVE),
                StatusModel("FULL", ValidatorStatus.FULL),
                StatusModel("UNKNOWN", ValidatorStatus.UNKNOWN),
                StatusModel("active", ValidatorStatus.UNKNOWN),
                StatusModel("retired", ValidatorStatus.UNKNOWN),
                StatusModel(null, ValidatorStatus.UNKNOWN),
            )
        }

        @Test
        fun `GIVEN non numeric apr WHEN convertFromEntity THEN reward info is null`() {
            // Act
            val actual = YieldConverter.convertFromEntity(createEntity(apr = "n/a"))

            // Assert
            assertThat(actual.rewardInfo).isNull()
        }

        @Test
        fun `GIVEN preferred flag missing WHEN convertFromEntity THEN not preferred`() {
            // Act
            val actual = YieldConverter.convertFromEntity(createEntity(isPreferred = null))

            // Assert
            assertThat(actual.isPreferred).isFalse()
        }
    }

    private fun createYieldDTO(vararg validators: YieldDTO.ValidatorDTO): YieldDTO {
        return MockYieldDTOFactory.create().copy(validators = validators.toList())
    }

    private fun createYieldDTO(rewardType: YieldDTO.RewardTypeDTO, vararg validators: YieldDTO.ValidatorDTO): YieldDTO {
        return MockYieldDTOFactory.create().copy(rewardType = rewardType, validators = validators.toList())
    }

    private fun withMetadata(transform: (YieldDTO.MetadataDTO) -> YieldDTO.MetadataDTO): YieldDTO {
        val base = MockYieldDTOFactory.create()
        return base.copy(metadata = transform(base.metadata!!))
    }

    @Suppress("LongParameterList")
    private fun createValidatorDTO(
        address: String? = POOL_ADDRESS,
        name: String? = null,
        status: ValidatorStatusDTO? = ValidatorStatusDTO.ACTIVE,
        preferred: Boolean? = false,
        apr: BigDecimal? = null,
        commission: Double? = null,
        image: String? = null,
        website: String? = null,
        stakedBalance: String? = null,
        votingPower: Double? = null,
    ): YieldDTO.ValidatorDTO {
        return YieldDTO.ValidatorDTO(
            address = address,
            status = status,
            name = name,
            image = image,
            website = website,
            apr = apr,
            commission = commission,
            stakedBalance = stakedBalance,
            votingPower = votingPower,
            preferred = preferred,
        )
    }

    @Suppress("LongParameterList")
    private fun createEntity(
        address: String = "v1",
        status: String? = "ACTIVE",
        name: String? = "Name",
        image: String? = null,
        website: String? = null,
        apr: String? = null,
        commission: Double? = null,
        stakedBalance: String? = null,
        votingPower: Double? = null,
        isPreferred: Boolean? = null,
    ) = StakingValidatorEntity(
        address = address,
        status = status,
        name = name,
        image = image,
        website = website,
        apr = apr,
        commission = commission,
        stakedBalance = stakedBalance,
        votingPower = votingPower,
        isPreferred = isPreferred,
    )

    internal data class RewardModel(val apr: BigDecimal?, val commission: Double?, val expectedRate: BigDecimal?)

    internal data class PartnerModel(val address: String, val name: String?, val expected: Boolean)

    internal data class StatusModel(val stored: String?, val expected: ValidatorStatus)

    internal class MandatoryModel(val message: String, val mutate: (YieldDTO) -> YieldDTO) {
        override fun toString() = message
    }

    private companion object {
        const val POOL_ADDRESS = "19521342b6c4cefb13f091a075a221a3e765435a0646902dd5379f3a"
        const val SOLANA_PARTNER = "H2tJNyMHnRF6ahCQLQ1sSycM4FGchymuzyYzUqKEuydk"
        const val COSMOS_PARTNER = "cosmosvaloper1wrx0x9m9ykdhw9sg04v7uljme53wuj03aa5d4f"
    }
}