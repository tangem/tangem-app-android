package com.tangem.features.staking.impl.presentation.state.transformers.notifications

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.uncapped
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.model.CooldownPeriod
import com.tangem.domain.staking.model.Period
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.blockchains.UtxoAmountLimit
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.fixtures.polygonToken
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.StakingNotification
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.createAssentConfirmationUiState
import com.tangem.features.staking.impl.presentation.state.feeContent
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class AddStakingNotificationsTransformerTest {

    private val clickIntents: StakingClickIntents = mockk(relaxed = true)

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
    private val ton = coin(Blockchain.TON)
    private val tonIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Ton)
    private val feeWarning = CryptoCurrencyWarning.BalanceNotEnoughForFee(polygonToken(), coin(Blockchain.Ethereum))

    private val extraReserveInfo = StakingNotification.Info.Ordinary(
        title = resourceReference(R.string.staking_notification_ton_extra_reserve_title),
        text = resourceReference(R.string.staking_notification_ton_extra_reserve_info),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(clickIntents)
    }

    @Test
    fun `GIVEN approval in progress WHEN transform THEN approval notification kept AND primary button disabled`() {
        // Arrange
        val transformer = transformer(isApprovalInProgress = true)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.notifications).contains(StakingNotification.Warning.TransactionInProgress.Approval)
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN no approval in progress WHEN transform THEN primary button enabled`() {
        // Arrange
        val transformer = transformer(isApprovalInProgress = false)

        // Act
        val actual = transformer.transform(createAssentConfirmationUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.notifications)
            .doesNotContain(StakingNotification.Warning.TransactionInProgress.Approval)
        assertThat(confirmation.isPrimaryButtonEnabled).isTrue()
    }

    @Nested
    inner class Guards {

        @Test
        fun `GIVEN empty confirmation WHEN transform THEN same state returned`() {
            // Arrange
            val input = stakingUiState(
                amountState = amountData(),
                confirmationState = StakingStates.ConfirmationState.Empty(),
            )

            // Act
            val actual = transformer().transform(input)

            // Assert
            assertThat(actual).isSameInstanceAs(input)
        }

        @Test
        fun `GIVEN empty amount state WHEN transform THEN same state returned`() {
            // Arrange
            val input = state().copy(amountState = AmountState.Empty)

            // Act
            val actual = transformer().transform(input)

            // Assert
            assertThat(actual).isSameInstanceAs(input)
        }

        @Test
        fun `GIVEN cached network source WHEN transform THEN primary disabled`() {
            // Arrange
            val transformer = transformer(status = currencyStatus(networkSource = StatusSource.CACHE))

            // Act
            val actual = confirmation(transformer.transform(state()))

            // Assert
            assertThat(actual.notifications).isEmpty()
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN cached staking contribution WHEN transform THEN primary disabled`() {
            // Arrange
            val status = currencyStatus(stakingBalance = MockStaking.stakeKitBalance(source = StatusSource.CACHE))

            // Act
            val actual = confirmation(transformer(status = status).transform(state()))

            // Assert
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }
    }

    @Nested
    inner class Ton {

        @Test
        fun `GIVEN uninitialized ton account and enter WHEN transform THEN only initialize notification and primary disabled`() {
            // Arrange
            val transformer = tonTransformer(amount = "10", isInitialized = false)

            // Act
            val actual = confirmation(transformer.transform(state()))
            val initialize = actual.notifications.single()
            (initialize.config.buttonsState as NotificationConfig.ButtonsState.PrimaryButtonConfig).onClick()

            // Assert
            assertThat(initialize).isInstanceOf(StakingNotification.Warning.InitializeTonAccount::class.java)
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
            verify(exactly = 1) { clickIntents.onActivateTonAccountNotificationShow() }
            verify(exactly = 1) { clickIntents.onActivateTonAccountNotificationClick() }
        }

        // Pins current behaviour: an uninitialized account skips every check outside TON enter.
        @Test
        fun `GIVEN uninitialized ton account and exit WHEN transform THEN no notifications and primary enabled`() {
            // Arrange
            val transformer = tonTransformer(amount = "10", isInitialized = false)

            // Act
            val actual = confirmation(transformer.transform(state(actionType = exit)))

            // Assert
            assertThat(actual.notifications).isEmpty()
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }

        // Pins current behaviour: the initialization flag disables fee and staking error checks on any chain.
        @Test
        fun `GIVEN uninitialized solana account WHEN transform THEN all checks skipped and primary enabled`() {
            // Arrange
            val transformer = transformer(
                isInitialized = false,
                stakingError = StakingError.StakeKitUnknownError(),
                feeError = GetFeeError.UnknownError,
            )

            // Act
            val actual = confirmation(transformer.transform(state()))

            // Assert
            assertThat(actual.notifications).isEmpty()
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }

        @Test
        fun `GIVEN ton balance below extra fee threshold WHEN transform THEN extra fee error and primary disabled`() {
            // Act
            val actual = confirmation(tonTransformer(amount = "0.1").transform(state()))

            // Assert
            assertThat(actual.notifications).containsExactly(NotificationUM.Error.TonStakingExtraFeeError)
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        // Pins current behaviour: the threshold is BigDecimal(0.2) built from a double, slightly above 0.2.
        @Test
        fun `GIVEN ton balance exactly point two WHEN transform THEN extra fee error not info`() {
            // Act
            val actual = confirmation(tonTransformer(amount = "0.2").transform(state()))

            // Assert
            assertThat(actual.notifications).containsExactly(NotificationUM.Error.TonStakingExtraFeeError)
        }

        @Test
        fun `GIVEN ton balance equal to extra fee threshold WHEN transform THEN no extra fee error`() {
            // Act
            val actual = confirmation(
                transformer(
                    status = currencyStatus(ton, amount = BigDecimal(0.2)),
                    integration = tonIntegration,
                ).transform(state()),
            )

            // Assert
            assertThat(actual.notifications).doesNotContain(NotificationUM.Error.TonStakingExtraFeeError)
        }

        @Test
        fun `GIVEN solana balance below ton threshold WHEN transform THEN no ton extra fee error`() {
            // Act
            val actual = confirmation(
                transformer(status = currencyStatus(amount = BigDecimal("0.1"))).transform(state(amount = "0.01", fee = "0.001")),
            )

            // Assert
            assertThat(actual.notifications).doesNotContain(NotificationUM.Error.TonStakingExtraFeeError)
        }

        @Test
        fun `GIVEN ton balance above threshold WHEN transform THEN extra reserve info only`() {
            // Act
            val actual = confirmation(tonTransformer(amount = "0.3").transform(state()))

            // Assert
            assertThat(actual.notifications).containsExactly(extraReserveInfo)
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }
    }

    @Nested
    inner class Dust {

        @Test
        fun `GIVEN cardano with dust value WHEN transform THEN no dust error`() {
            // Arrange
            val transformer = transformer(
                status = currencyStatus(coin(Blockchain.Cardano), amount = BigDecimal("100")),
                check = check(dust = BigDecimal.ONE),
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Cardano),
            )

            // Act
            val actual = confirmation(transformer.transform(state(actionType = exit)))

            // Assert
            assertThat(actual.notifications).containsExactly(
                StakingNotification.Info.Unstake(
                    cooldownPeriod = CooldownPeriod.Fixed(Period.Days(3)),
                    subtitleRes = R.string.staking_notification_unstake_text,
                ),
            )
        }

        // Pins current behaviour: an exit sends zero, which is always below the dust value.
        @Test
        fun `GIVEN solana exit with dust value WHEN transform THEN dust error and primary disabled`() {
            // Arrange
            val status = currencyStatus(amount = BigDecimal("100"))
            val transformer = transformer(status = status, check = check(dust = BigDecimal.ONE))

            // Act
            val actual = confirmation(transformer.transform(state(actionType = exit)))

            // Assert
            assertThat(actual.notifications).containsExactly(
                NotificationUM.Error.MinimumAmountError(BigDecimal.ONE.format { crypto(status.currency) }),
                StakingNotification.Info.Unstake(
                    cooldownPeriod = CooldownPeriod.Fixed(Period.Days(3)),
                    subtitleRes = R.string.staking_notification_unstake_text,
                ),
            ).inOrder()
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN no dust value WHEN transform THEN no dust error`() {
            // Arrange
            val transformer = transformer(
                status = currencyStatus(amount = BigDecimal("100")),
                check = check(dust = null),
            )

            // Act
            val actual = confirmation(transformer.transform(state(actionType = exit)))

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Error.MinimumAmountError>()).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StakingErrors {

        @Test
        fun `GIVEN stakeKit api error WHEN transform THEN common error with error text`() {
            // Arrange
            val error = StakingError.StakeKitApiError(message = "m", code = 1, details = null, methodName = null)

            // Act
            val actual = confirmation(transformer(stakingError = error).transform(state()))

            // Assert
            assertThat(actual.notifications)
                .contains(StakingNotification.Error.Common(stringReference(error.toString())))
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN other staking error WHEN transform THEN network fee unreachable`(model: OtherStakingErrorModel) {
            // Act
            val actual = confirmation(transformer(stakingError = model.error).transform(state()))
            val unreachable = actual.notifications.filterIsInstance<NotificationUM.Warning.NetworkFeeUnreachable>()
            unreachable.single().onRefresh()

            // Assert
            assertThat(unreachable).hasSize(1)
            verify(exactly = 1) { clickIntents.getFee() }
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN fee unknown error WHEN transform THEN network fee unreachable`() {
            // Act
            val actual = confirmation(transformer(feeError = GetFeeError.UnknownError).transform(state()))

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Warning.NetworkFeeUnreachable>())
                .hasSize(1)
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN tron activation fee error WHEN transform THEN tron not activated warning`() {
            // Arrange
            val tron = coin(Blockchain.Tron)
            val transformer = transformer(
                status = currencyStatus(tron),
                feeError = GetFeeError.BlockchainErrors.TronActivationError,
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron),
            )

            // Act
            val actual = confirmation(transformer.transform(state()))

            // Assert
            assertThat(actual.notifications).containsExactly(NotificationUM.Warning.TronAccountNotActivated(tron.name))
        }

        private fun provideTestModels() = listOf(
            OtherStakingErrorModel(StakingError.StakeKitUnknownError()),
            OtherStakingErrorModel(StakingError.DomainError("d")),
            OtherStakingErrorModel(StakingError.InvalidAmount("i")),
        )
    }

    @Nested
    inner class Verdict {

        private val symbol = currencyStatus().currency.symbol

        @Test
        fun `GIVEN unsafe verdict WHEN transform THEN unsafe error and primary disabled`() {
            // Act
            val actual = confirmation(transformer().transform(state(verdict = StakingTransactionVerdict.UNSAFE)))

            // Assert
            assertThat(actual.notifications).contains(StakingNotification.Error.TransactionValidationUnsafe(symbol))
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN warning verdict WHEN transform THEN validation warning and primary enabled`() {
            // Act
            val actual = confirmation(transformer().transform(state(verdict = StakingTransactionVerdict.WARNING)))

            // Assert
            assertThat(actual.notifications)
                .contains(StakingNotification.Warning.TransactionValidationWarning(symbol))
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }

        @Test
        fun `GIVEN safe verdict WHEN transform THEN no validation notification`() {
            // Act
            val actual = confirmation(transformer().transform(state(verdict = StakingTransactionVerdict.SAFE)))

            // Assert
            assertThat(actual.notifications)
                .doesNotContain(StakingNotification.Error.TransactionValidationUnsafe(symbol))
            assertThat(actual.notifications)
                .doesNotContain(StakingNotification.Warning.TransactionValidationWarning(symbol))
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }
    }

    @Nested
    inner class ExceedsBalance {

        @Test
        fun `GIVEN enter exceeding balance with subtract available WHEN transform THEN total exceeds balance`() {
            // Act
            val actual = confirmation(transformer(isSubtract = true).transform(state(amount = "12", fee = "1")))

            // Assert
            assertThat(actual.notifications).contains(NotificationUM.Error.TotalExceedsBalance)
            verify(exactly = 1) { clickIntents.onNotEnoughFeeNotificationShow() }
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN exit fee exceeding balance WHEN transform THEN exceeds balance with currency fields`() {
            // Arrange
            val currency = currencyStatus().currency

            // Act
            val actual = confirmation(transformer(isSubtract = true).transform(state(actionType = exit, fee = "11")))
            val exceeds = actual.notifications.filterIsInstance<NotificationUM.Error.ExceedsBalance>().single()
            exceeds.onClick?.invoke()

            // Assert
            assertThat(exceeds.networkName).isEqualTo(currency.name)
            assertThat(exceeds.currencyName).isEqualTo(currency.name)
            assertThat(exceeds.feeName).isEqualTo(currency.name)
            assertThat(exceeds.feeSymbol).isEqualTo(currency.symbol)
            assertThat(exceeds.shouldMergeFeeNetworkName).isFalse()
            verify(exactly = 1) { clickIntents.openTokenDetails(currency) }
        }

        @Test
        fun `GIVEN subtract unavailable WHEN transform THEN no exceeds balance`() {
            // Act
            val actual = confirmation(transformer(isSubtract = false).transform(state(amount = "12")))

            // Assert
            assertThat(actual.notifications).doesNotContain(NotificationUM.Error.TotalExceedsBalance)
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Error.ExceedsBalance>()).isEmpty()
        }

        @Test
        fun `GIVEN balance not enough for fee warning WHEN transform THEN token exceeds balance`() {
            // Act
            val actual = confirmation(transformer(warning = feeWarning).transform(state()))

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Error.TokenExceedsBalance>()).hasSize(1)
            verify(exactly = 1) { clickIntents.onNotEnoughFeeNotificationShow() }
        }
    }

    @Nested
    inner class CurrencyChecks {

        @Test
        fun `GIVEN utxo limit with fee WHEN transform THEN transaction limit error`() {
            // Arrange
            val utxo = UtxoAmountLimit(
                limit = BigDecimal("100"),
                availableToSpend = BigDecimal("5"),
                availableToSend = BigDecimal("5"),
            )

            // Act
            val actual = confirmation(transformer(check = check(utxo = utxo)).transform(state()))
            val limit = actual.notifications.filterIsInstance<NotificationUM.Error.TransactionLimitError>().single()
            limit.onConfirmClick()

            // Assert
            assertThat(limit.amountLimit).isEqualTo(BigDecimal("5").format { crypto(currencyStatus().currency) })
            verify(exactly = 1) {
                clickIntents.onAmountReduceToClick(
                    BigDecimal("5"),
                    NotificationUM.Error.TransactionLimitError::class.java,
                )
            }
        }

        // Pins current behaviour: the transformer passes isAccountFunded = false regardless of the check.
        @Test
        fun `GIVEN reserve above sending with funded account WHEN transform THEN reserve error anyway`() {
            // Arrange
            val status = currencyStatus()
            val transformer = transformer(check = check(reserve = BigDecimal.ONE, isAccountFunded = true))

            // Act
            val actual = confirmation(transformer.transform(state(actionType = exit)))

            // Assert
            assertThat(actual.notifications)
                .contains(NotificationUM.Error.ReserveAmount(BigDecimal.ONE.format { crypto(status.currency) }))
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN rent warning WHEN transform THEN rent info and primary disabled`() {
            // Arrange
            val rent = CryptoCurrencyWarning.Rent(
                rent = BigDecimal("0.001"),
                exemptionAmount = BigDecimal("0.002"),
                cryptoCurrency = currencyStatus().currency,
            )

            // Act
            val actual = confirmation(transformer(check = check(rent = rent)).transform(state()))

            // Assert
            assertThat(actual.notifications).contains(NotificationUM.Solana.RentInfo(rent))
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN existential deposit above remainder WHEN transform THEN existential deposit error`() {
            // Act
            val actual = confirmation(
                transformer(check = check(ed = BigDecimal.ONE)).transform(state(amount = "9", fee = "0.5")),
            )
            val deposit = actual.notifications.filterIsInstance<NotificationUM.Error.ExistentialDeposit>().single()
            deposit.onConfirmClick()

            // Assert
            assertThat(deposit.isActionable).isTrue()
            verify(exactly = 1) {
                clickIntents.onAmountReduceByClick(
                    BigDecimal.ONE,
                    BigDecimal("0.5"),
                    NotificationUM.Error.ExistentialDeposit::class.java,
                )
            }
        }

        @Test
        fun `GIVEN existential deposit below remainder WHEN transform THEN no existential error`() {
            // Act
            val actual = confirmation(
                transformer(check = check(ed = BigDecimal.ONE)).transform(state(amount = "1", fee = "0.1")),
            )

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Error.ExistentialDeposit>()).isEmpty()
        }
    }

    @Nested
    inner class FeeCurrency {

        private val eth = coin(Blockchain.Ethereum)
        private val polygonIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.EthereumToken.Polygon)

        @Test
        fun `GIVEN polygon token with ETH fee status and reserve WHEN transform THEN network account not funded in fee coin`() {
            // Arrange
            val transformer = transformer(
                status = currencyStatus(polygonToken(), amount = BigDecimal("100")),
                feeStatus = currencyStatus(eth),
                check = check(reserve = BigDecimal.ONE),
                integration = polygonIntegration,
            )

            // Act
            val actual = confirmation(transformer.transform(state()))

            // Assert
            assertThat(actual.notifications).containsExactly(
                NotificationUM.Error.NetworkAccountNotFunded(
                    coinName = eth.name,
                    reserveAmount = BigDecimal.ONE.format { crypto(symbol = "", decimals = eth.decimals) },
                    reserveSymbol = eth.symbol,
                ),
            )
            assertThat(actual.isPrimaryButtonEnabled).isFalse()
        }

        @Test
        fun `GIVEN polygon token without fee status and reserve WHEN transform THEN no reserve notification`() {
            // Arrange
            val transformer = transformer(
                status = currencyStatus(polygonToken(), amount = BigDecimal("100")),
                feeStatus = null,
                check = check(reserve = BigDecimal.ONE),
                integration = polygonIntegration,
            )

            // Act
            val actual = confirmation(transformer.transform(state()))

            // Assert
            assertThat(actual.notifications).isEmpty()
            assertThat(actual.isPrimaryButtonEnabled).isTrue()
        }
    }

    @Nested
    inner class ReduceAmountBy {

        @Test
        fun `GIVEN reduceAmountBy that makes the amount need fee subtraction WHEN transform THEN fee coverage for the subtracted part`() {
            // Arrange
            val status = currencyStatus(amount = BigDecimal("10"))
            val transformer = transformer(status = status, isSubtract = true)

            // Act
            val actual = confirmation(
                transformer.transform(state(amount = "9", fee = "1", reduceAmountBy = BigDecimal.ONE)),
            )

            // Assert
            val coverage = actual.notifications.single() as NotificationUM.Warning.FeeCoverageNotification
            assertThat(coverage.cryptoAmount).isEqualTo(BigDecimal.ONE.format { crypto(status.currency).uncapped() })
        }

        @Test
        fun `GIVEN same amount without reduceAmountBy WHEN transform THEN stake entire balance info instead of fee coverage`() {
            // Arrange
            val transformer = transformer(status = currencyStatus(amount = BigDecimal("10")), isSubtract = true)

            // Act
            val actual = confirmation(transformer.transform(state(amount = "9", fee = "1")))

            // Assert
            assertThat(
                actual.notifications.single(),
            ).isInstanceOf(StakingNotification.Info.StakeEntireBalance::class.java)
        }
    }

    @Test
    fun `GIVEN approximate fee and entire balance WHEN transform THEN reduce text uses can be lower fee format`() {
        // Arrange
        val transformer = transformer(status = currencyStatus(amount = BigDecimal("10")), isSubtract = true)
        val state = state(amount = "9", fee = "1").let { prev ->
            prev.copy(
                confirmationState = confirmation(prev).copy(
                    feeState = feeContent(ethFee(BigDecimal("1")), isFeeApproximate = true),
                ),
            )
        }

        // Act
        val actual = confirmation(transformer.transform(state))

        // Assert
        val notification = actual.notifications.single()
        val button = notification.config.buttonsState as NotificationConfig.ButtonsState.PrimaryButtonConfig
        assertThat(button.text).isEqualTo(
            resourceReference(
                R.string.send_notification_reduce_by,
                wrappedList(BigDecimal("3").format { crypto(symbol = "SOL", decimals = 9).fee(canBeLower = true) }),
            ),
        )
    }

    @Test
    fun `GIVEN exact fee and entire balance WHEN transform THEN reduce text uses exact fee format`() {
        // Arrange
        val transformer = transformer(status = currencyStatus(amount = BigDecimal("10")), isSubtract = true)

        // Act
        val actual = confirmation(transformer.transform(state(amount = "9", fee = "1")))

        // Assert
        val button = actual.notifications.single().config.buttonsState
            as NotificationConfig.ButtonsState.PrimaryButtonConfig
        assertThat(button.text).isEqualTo(
            resourceReference(
                R.string.send_notification_reduce_by,
                wrappedList(BigDecimal("3").format { crypto(symbol = "SOL", decimals = 9).fee(canBeLower = false) }),
            ),
        )
    }

    @Test
    fun `GIVEN fee still loading WHEN transform THEN notifications built without fee`() {
        // Arrange
        val state = state().let { prev ->
            prev.copy(confirmationState = confirmation(prev).copy(feeState = FeeState.Loading))
        }

        // Act
        val actual = confirmation(transformer().transform(state))

        // Assert
        assertThat(actual.notifications).isEmpty()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN only staking api error WHEN transform THEN primary button disabled`() {
        // Arrange
        val error = StakingError.StakeKitApiError(message = "m", code = 1, details = null, methodName = null)

        // Act
        val actual = confirmation(transformer(stakingError = error).transform(state()))

        // Assert
        assertThat(actual.notifications.single()).isInstanceOf(StakingNotification.Error.Common::class.java)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    /** The refactor requires side-effect-free transformers; these tests pin how many callbacks fire today. */
    @Nested
    inner class SideEffects {

        // Pins current behaviour: two independent checks report the same analytics event.
        @Test
        fun `GIVEN both exceed and fee warning WHEN applied once THEN analytics fired twice`() {
            // Act
            transformer(isSubtract = true, warning = feeWarning).transform(state(amount = "12", fee = "1"))

            // Assert
            verify(exactly = 2) { clickIntents.onNotEnoughFeeNotificationShow() }
        }

        @Test
        fun `GIVEN no warnings WHEN transformer applied twice THEN no analytics callbacks`() {
            // Arrange
            val transformer = transformer()
            val input = state()

            // Act
            transformer.transform(input)
            transformer.transform(input)

            // Assert
            verify(exactly = 0) { clickIntents.onNotEnoughFeeNotificationShow() }
            verify(exactly = 0) { clickIntents.onActivateTonAccountNotificationShow() }
        }
    }

    @Nested
    inner class FeeCoverage {

        @Test
        fun `GIVEN amount needs fee subtraction WHEN transform THEN fee coverage warning`() {
            // Act
            val actual = confirmation(
                transformer(isSubtract = true, integration = minimumIntegration("1"))
                    .transform(state(amount = "10", fee = "1")),
            )

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Warning.FeeCoverageNotification>())
                .hasSize(1)
        }

        @Test
        fun `GIVEN subtracted amount equals enter minimum WHEN transform THEN no fee coverage`() {
            // Act
            val actual = confirmation(
                transformer(isSubtract = true, integration = minimumIntegration("9"))
                    .transform(state(amount = "10", fee = "1")),
            )

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Warning.FeeCoverageNotification>())
                .isEmpty()
        }

        // Pins current behaviour: the minimum is compared with equals, so 9 and 9.0 are different.
        @Test
        fun `GIVEN subtracted amount equal to minimum in other scale WHEN transform THEN fee coverage shown`() {
            // Act
            val actual = confirmation(
                transformer(isSubtract = true, integration = minimumIntegration("9.0"))
                    .transform(state(amount = "10", fee = "1")),
            )

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Warning.FeeCoverageNotification>())
                .hasSize(1)
        }

        @Test
        fun `GIVEN exit WHEN transform THEN no fee coverage`() {
            // Act
            val actual = confirmation(
                transformer(isSubtract = true, integration = minimumIntegration("1"))
                    .transform(state(actionType = exit, amount = "10", fee = "1")),
            )

            // Assert
            assertThat(actual.notifications.filterIsInstance<NotificationUM.Warning.FeeCoverageNotification>())
                .isEmpty()
        }

        private fun minimumIntegration(minimum: String) = stakeKitIntegration(
            yield = MockStaking.yield(enter = MockStaking.enterArgs(minimum = BigDecimal(minimum))),
        )
    }

    private fun tonTransformer(amount: String, isInitialized: Boolean = true) = transformer(
        status = currencyStatus(ton, amount = BigDecimal(amount)),
        isInitialized = isInitialized,
        integration = tonIntegration,
    )

    @Suppress("LongParameterList")
    private fun transformer(
        status: CryptoCurrencyStatus = currencyStatus(),
        isInitialized: Boolean = true,
        feeStatus: CryptoCurrencyStatus? = null,
        warning: CryptoCurrencyWarning? = null,
        feeError: GetFeeError? = null,
        stakingError: StakingError? = null,
        check: CryptoCurrencyCheck = check(),
        isSubtract: Boolean = false,
        isApprovalInProgress: Boolean = false,
        integration: StakingIntegration = stakeKitIntegration(),
    ) = AddStakingNotificationsTransformer(
        cryptoCurrencyStatusProvider = Provider { status },
        appCurrencyProvider = Provider { AppCurrency.Default },
        isAccountInitializedProvider = Provider { isInitialized },
        feeCryptoCurrencyStatus = feeStatus,
        currencyWarning = warning,
        feeError = feeError,
        stakingError = stakingError,
        currencyCheck = check,
        isSubtractAvailable = isSubtract,
        isApprovalInProgress = isApprovalInProgress,
        integration = integration,
    )

    @Suppress("LongParameterList")
    private fun check(
        dust: BigDecimal? = null,
        reserve: BigDecimal? = null,
        ed: BigDecimal? = null,
        utxo: UtxoAmountLimit? = null,
        rent: CryptoCurrencyWarning.Rent? = null,
        isAccountFunded: Boolean = true,
    ) = CryptoCurrencyCheck(
        dustValue = dust,
        reserveAmount = reserve,
        minimumSendAmount = null,
        existentialDeposit = ed,
        utxoAmountLimit = utxo,
        isAccountFunded = isAccountFunded,
        rentWarning = rent,
    )

    private fun state(
        actionType: StakingActionCommonType = enter,
        amount: String = "1",
        fee: String = "0.1",
        verdict: StakingTransactionVerdict? = null,
        reduceAmountBy: BigDecimal? = null,
    ): StakingUiState = stakingUiState(
        currentStep = StakingStep.Confirmation,
        actionType = actionType,
        clickIntents = clickIntents,
        amountState = amountData(BigDecimal(amount)),
        confirmationState = confirmationData(
            feeState = feeContent(ethFee(BigDecimal(fee))),
            transactionVerdict = verdict,
            reduceAmountBy = reduceAmountBy,
        ),
    )

    private fun confirmation(state: StakingUiState) = state.confirmationState as StakingStates.ConfirmationState.Data

    internal data class OtherStakingErrorModel(val error: StakingError)
}