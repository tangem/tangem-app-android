package com.tangem.data.staking.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import com.tangem.grow.datasource.stakekit.addStakeKitEnumFallbackAdapters
import com.tangem.grow.datasource.stakekit.models.request.Address
import com.tangem.grow.datasource.stakekit.models.response.model.AddressArgumentDTO
import com.tangem.grow.datasource.stakekit.models.response.model.BalanceDTO
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.TokenDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.action.StakingActionTypeDTO
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the staking yields and balances: files written by the Moshi serializer must read back
 * unchanged after the stores moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still
 * used while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class StakingStoredFormatTest {

    /** Mirrors the production Moshi: Joda dates as `DateTime.toString()` and the StakeKit enum fallbacks. */
    private val moshi: Moshi = legacyDataStoreMoshi.newBuilder()
        .add(DateTimeAdapter)
        .addStakeKitEnumFallbackAdapters()
        .build()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Yields {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yields WHEN encoded by Moshi THEN matches the stored format`(model: YieldsModel) {
            // Act
            val actual = moshi.adapter<List<YieldDTO>>().toJson(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN decoded by Moshi THEN yields are kept`(model: YieldsModel) {
            // Act
            val actual = moshi.adapter<List<YieldDTO>>().fromJson(model.json)

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN read from DataStore THEN yields are kept`(model: YieldsModel) {
            // Act
            val actual = readStoredJson(json = model.json, defaultValue = emptyList<YieldDTO>())

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN yields WHEN written to DataStore THEN keeps the stored format`(model: YieldsModel) {
            // Act
            val actual = writeStoredJson(value = model.value, defaultValue = emptyList())

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        private fun provideTestModels() = listOf(
            YieldsModel(value = listOf(YIELD), json = "[$YIELD_JSON]"),
            YieldsModel(value = listOf(MINIMAL_YIELD), json = """[{"id":"minimal","rewardType":"apr"}]"""),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Balances {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balances WHEN encoded by Moshi THEN matches the stored format`(model: BalancesModel) {
            // Act
            val actual = moshi.adapter<Map<String, Set<YieldBalanceWrapperDTO>>>().toJson(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN decoded by Moshi THEN balances are kept`(model: BalancesModel) {
            // Act
            val actual = moshi.adapter<Map<String, Set<YieldBalanceWrapperDTO>>>().fromJson(model.json)

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN read from DataStore THEN balances are kept`(model: BalancesModel) {
            // Act
            val actual = readStoredJson(
                json = model.json,
                defaultValue = emptyMap<String, Set<YieldBalanceWrapperDTO>>(),
            )

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN balances WHEN written to DataStore THEN keeps the stored format`(model: BalancesModel) {
            // Act
            val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        private fun provideTestModels() = listOf(
            BalancesModel(value = mapOf("wallet" to setOf(BALANCES)), json = """{"wallet":[$BALANCES_JSON]}"""),
            BalancesModel(
                value = mapOf(
                    "wallet" to setOf(
                        BALANCES.copy(
                            addresses = Address(address = "addr"),
                            balances = listOf(BALANCE.copy(date = null, pendingActions = emptyList())),
                            integrationId = null,
                        ),
                    ),
                ),
                json = """{"wallet":[{"addresses":{"address":"addr"},"balances":[$BALANCE_MINIMAL_JSON]}]}""",
            ),
        )
    }

    data class YieldsModel(val value: List<YieldDTO>, val json: String)

    data class BalancesModel(val value: Map<String, Set<YieldBalanceWrapperDTO>>, val json: String)

    private object DateTimeAdapter {
        @FromJson
        fun fromJson(value: String): DateTime = DateTime.parse(value)

        @ToJson
        fun toJson(value: DateTime): String = value.toString()
    }

    private companion object {
        val TOKEN = TokenDTO(
            name = "Solana",
            network = NetworkTypeDTO.SOLANA,
            symbol = "SOL",
            decimals = 9,
            address = null,
            coinGeckoId = "solana",
            logoURI = "https://logo",
            isPoints = null,
        )
        const val TOKEN_JSON = """{"name":"Solana","network":"solana","symbol":"SOL","decimals":9,""" +
            """"coinGeckoId":"solana","logoURI":"https://logo"}"""

        val YIELD = YieldDTO(
            id = "solana-sol-native-multivalidator-staking",
            token = TOKEN,
            tokens = listOf(TOKEN),
            args = YieldDTO.ArgsDTO(
                enter = YieldDTO.ArgsDTO.Enter(
                    addresses = YieldDTO.ArgsDTO.Enter.Addresses(
                        address = AddressArgumentDTO(required = true, network = "solana"),
                        additionalAddresses = mapOf("cosmosPubKey" to AddressArgumentDTO(required = false)),
                    ),
                    args = mapOf(
                        "amount" to AddressArgumentDTO(
                            required = true,
                            minimum = BigDecimal("0.00000001"),
                            maximum = BigDecimal("100"),
                        ),
                    ),
                ),
                exit = null,
            ),
            status = YieldDTO.StatusDTO(enter = true, exit = false),
            apy = BigDecimal("0.0712"),
            rewardRate = 7.12,
            rewardType = YieldDTO.RewardTypeDTO.APY,
            metadata = YieldDTO.MetadataDTO(
                name = "Solana",
                logoUri = "https://logo",
                description = "Staking",
                documentation = null,
                gasFeeTokenDTO = TOKEN,
                tokenDTO = TOKEN,
                tokensDTO = null,
                type = "staking",
                rewardSchedule = YieldDTO.MetadataDTO.RewardScheduleDTO.EPOCH,
                cooldownPeriod = YieldDTO.MetadataDTO.PeriodDTO(days = 2, seconds = null),
                warmupPeriod = YieldDTO.MetadataDTO.PeriodDTO(days = 0, seconds = 0),
                rewardClaiming = YieldDTO.MetadataDTO.RewardClaimingDTO.AUTO,
                defaultValidator = "validator",
                minimumStake = 1,
                supportsMultipleValidators = true,
                revshare = YieldDTO.MetadataDTO.EnabledDTO(enabled = true),
                fee = YieldDTO.MetadataDTO.EnabledDTO(enabled = null),
            ),
            validators = listOf(
                YieldDTO.ValidatorDTO(
                    address = "validator",
                    status = YieldDTO.ValidatorDTO.ValidatorStatusDTO.ACTIVE,
                    name = "Validator",
                    image = null,
                    website = null,
                    apr = BigDecimal("6.5"),
                    commission = 0.05,
                    stakedBalance = "1000",
                    votingPower = 0.01,
                    preferred = true,
                ),
            ),
            isAvailable = true,
        )
        const val YIELD_JSON = """{"id":"solana-sol-native-multivalidator-staking","token":$TOKEN_JSON,""" +
            """"tokens":[$TOKEN_JSON],"args":{"enter":{"addresses":{"address":{"required":true,""" +
            """"network":"solana"},"additionalAddresses":{"cosmosPubKey":{"required":false}}},""" +
            """"args":{"amount":{"required":true,"minimum":"1E-8","maximum":"100"}}}},""" +
            """"status":{"enter":true,"exit":false},"apy":"0.0712","rewardRate":7.12,"rewardType":"apy",""" +
            """"metadata":{"name":"Solana","logoURI":"https://logo","description":"Staking",""" +
            """"gasFeeToken":$TOKEN_JSON,"token":$TOKEN_JSON,"type":"staking","rewardSchedule":"epoch",""" +
            """"cooldownPeriod":{"days":2},"warmupPeriod":{"days":0,"seconds":0},"rewardClaiming":"auto",""" +
            """"defaultValidator":"validator","minimumStake":1,"supportsMultipleValidators":true,""" +
            """"revshare":{"enabled":true},"fee":{}},"validators":[{"address":"validator","status":"active",""" +
            """"name":"Validator","apr":"6.5","commission":0.05,"stakedBalance":"1000","votingPower":0.01,""" +
            """"preferred":true}],"isAvailable":true}"""

        /** Moshi's StakeKit enum adapters aren't null-safe, so a stored yield always has its enums set. */
        val MINIMAL_YIELD = YieldDTO(
            id = "minimal",
            token = null,
            tokens = null,
            args = null,
            status = null,
            apy = null,
            rewardRate = null,
            rewardType = YieldDTO.RewardTypeDTO.APR,
            metadata = null,
            validators = null,
            isAvailable = null,
        )

        val BALANCE = BalanceDTO(
            groupId = "group",
            type = BalanceDTO.BalanceTypeDTO.UNSTAKING,
            amount = BigDecimal("1.5"),
            date = DateTime(2026, 10, 7, 12, 30, DateTimeZone.UTC),
            pricePerShare = BigDecimal("1"),
            pendingActions = listOf(
                BalanceDTO.PendingAction(
                    type = StakingActionTypeDTO.WITHDRAW,
                    passthrough = "passthrough",
                    args = BalanceDTO.PendingAction.PendingActionArgs(
                        amount = BalanceDTO.PendingAction.PendingActionArgs.Amount(
                            minimum = BigDecimal("0.1"),
                            maximum = null,
                        ),
                        duration = null,
                        validatorAddress = BalanceDTO.Required(required = true),
                        validatorAddresses = null,
                        nfts = null,
                        tronResource = null,
                        signatureVerification = null,
                    ),
                ),
            ),
            pendingActionConstraints = listOf(
                BalanceDTO.PendingActionConstraints(
                    type = StakingActionTypeDTO.CLAIM_REWARDS,
                    amountArg = BalanceDTO.PendingAction.PendingActionArgs.Amount(
                        required = false,
                        minimum = null,
                        maximum = BigDecimal("2"),
                    ),
                ),
            ),
            tokenDTO = TOKEN,
            validatorAddress = "validator",
            validatorAddresses = listOf("validator"),
            providerId = null,
        )
        val BALANCES = YieldBalanceWrapperDTO(
            addresses = Address(
                address = "addr",
                additionalAddresses = Address.AdditionalAddresses(cosmosPubKey = "pubkey"),
            ),
            balances = listOf(BALANCE),
            integrationId = "solana-sol-native-multivalidator-staking",
        )
        const val BALANCE_HEAD_JSON = """"groupId":"group","type":"unstaking","amount":"1.5""""
        const val BALANCE_TAIL_JSON = """"token":$TOKEN_JSON,"validatorAddress":"validator",""" +
            """"validatorAddresses":["validator"]"""
        const val BALANCES_JSON = """{"addresses":{"address":"addr","additionalAddresses":""" +
            """{"cosmosPubKey":"pubkey"}},"balances":[{$BALANCE_HEAD_JSON,"date":"2026-10-07T12:30:00.000Z",""" +
            """"pricePerShare":"1","pendingActions":[{"type":"WITHDRAW","passthrough":"passthrough",""" +
            """"args":{"amount":{"required":true,"minimum":"0.1"},"validatorAddress":{"required":true}}}],""" +
            """"pendingActionConstraints":[{"type":"CLAIM_REWARDS","amount":{"required":false,"maximum":"2"}}],""" +
            """$BALANCE_TAIL_JSON}],"integrationId":"solana-sol-native-multivalidator-staking"}"""
        const val BALANCE_MINIMAL_JSON = """{$BALANCE_HEAD_JSON,"pricePerShare":"1","pendingActions":[],""" +
            """"pendingActionConstraints":[{"type":"CLAIM_REWARDS","amount":{"required":false,"maximum":"2"}}],""" +
            """$BALANCE_TAIL_JSON}"""
    }
}