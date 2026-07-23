package com.tangem.feature.wallet.presentation.wallet.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.usecase.GetAppUpdateStateUseCase
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import com.tangem.domain.assetsdiscovery.usecase.ObserveAssetsDiscoveryUseCase
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.hotwallet.GetAccessCodeSkippedUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.crypto.BlockchainUtils
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetWalletNotificationsFactoryTest {

    private val isDemoCardUseCase: IsDemoCardUseCase = mockk()
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase = mockk()
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()
    private val accountDependencies: AccountDependencies = mockk()
    private val getAccessCodeSkippedUseCase: GetAccessCodeSkippedUseCase = mockk()
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase = mockk()
    private val observeAssetsDiscoveryUseCase: ObserveAssetsDiscoveryUseCase = mockk()
    private val getAppUpdateStateUseCase: GetAppUpdateStateUseCase = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val clickIntents: WalletClickIntents = mockk(relaxed = true)

    private val currencyFactory = MockCryptoCurrencyFactory()

    private val coldResolver: CardTypesResolver = mockk(relaxed = true)
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true)
    private val hotWallet: UserWallet.Hot = mockk(relaxed = true)

    private val factory = GetWalletNotificationsFactory(
        isDemoCardUseCase = isDemoCardUseCase,
        isNeedToBackupUseCase = isNeedToBackupUseCase,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
        accountDependencies = accountDependencies,
        getAccessCodeSkippedUseCase = getAccessCodeSkippedUseCase,
        hasSingleWalletSignedHashesUseCase = hasSingleWalletSignedHashesUseCase,
        observeAssetsDiscoveryUseCase = observeAssetsDiscoveryUseCase,
        getAppUpdateStateUseCase = getAppUpdateStateUseCase,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            isDemoCardUseCase,
            isNeedToBackupUseCase,
            isWalletBackupProblematicUseCase,
            accountDependencies,
            getAccessCodeSkippedUseCase,
            hasSingleWalletSignedHashesUseCase,
            observeAssetsDiscoveryUseCase,
            getAppUpdateStateUseCase,
            singleAccountStatusListSupplier,
            clickIntents,
            coldResolver,
            coldWallet,
            hotWallet,
        )
        mockkStatic(ScanResponse::cardTypesResolver)

        // Defaults: every notification is suppressed. Each test flips exactly one gate on.
        every { accountDependencies.singleAccountStatusListSupplier } returns singleAccountStatusListSupplier
        every { isDemoCardUseCase(any()) } returns false
        every { isWalletBackupProblematicUseCase(any()) } returns false
        every { isNeedToBackupUseCase(any()) } returns flowOf(false)
        every { getAccessCodeSkippedUseCase(any()) } returns flowOf(true)
        every { observeAssetsDiscoveryUseCase(any()) } returns flowOf(AssetsDiscoveryProgress.Idle)
        every { getAppUpdateStateUseCase.getBannerStateFlow() } returns flowOf(AppUpdateState.NoUpdate)
        every { hasSingleWalletSignedHashesUseCase(any(), any()) } returns flowOf(false)
        // Balance is loaded and non-zero, so both the outdated-data and add-funds banners stay hidden.
        stubAccountStatusList(balance = LOADED_NON_ZERO)

        // Cold wallet: release firmware, attestation ok, no low signatures, not a test/demo card.
        val scanResponse = mockk<ScanResponse>()
        every { scanResponse.cardTypesResolver } returns coldResolver
        every { coldWallet.walletId } returns WALLET_ID
        every { coldWallet.scanResponse } returns scanResponse
        every { coldWallet.isMultiCurrency } returns true
        every { coldResolver.isReleaseFirmwareType() } returns true
        every { coldResolver.isAttestationFailed() } returns false
        every { coldResolver.getRemainingSignatures() } returns null
        every { coldResolver.isTestCard() } returns false
        every { coldResolver.getCardId() } returns CARD_ID

        // Hot wallet: backed up and access-code protected, so the finish-activation banner stays hidden.
        val hotWalletId = mockk<HotWalletId> { every { authType } returns HotWalletId.AuthType.Password }
        every { hotWallet.walletId } returns WALLET_ID
        every { hotWallet.hotWalletId } returns hotWalletId
        every { hotWallet.backedUp } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ScanResponse::cardTypesResolver)
    }

    @Test
    fun `GIVEN default gates WHEN create THEN no notification is shown for cold wallet`() = runTest {
        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN default gates WHEN create THEN no notification is shown for hot wallet`() = runTest {
        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result).isEmpty()
    }

    // region UsedOutdatedData
    @ParameterizedTest
    @MethodSource("provideBalanceModels")
    fun `GIVEN balance source WHEN create THEN outdated-data banner visibility matches`(
        model: OutdatedDataModel,
    ) = runTest {
        // Arrange
        stubAccountStatusList(balance = model.balance)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.UsedOutdatedData }).isEqualTo(model.expectedShown)
    }

    internal data class OutdatedDataModel(val balance: TotalFiatBalance, val expectedShown: Boolean)

    private fun provideBalanceModels() = listOf(
        // Only a loaded balance sourced from cache-only marks the data as outdated.
        OutdatedDataModel(TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ONLY_CACHE), expectedShown = true),
        OutdatedDataModel(TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL), expectedShown = false),
        OutdatedDataModel(TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.CACHE), expectedShown = false),
        OutdatedDataModel(TotalFiatBalance.Loading, expectedShown = false),
        OutdatedDataModel(TotalFiatBalance.Failed, expectedShown = false),
    )
    // endregion

    // region AddFunds
    @ParameterizedTest
    @MethodSource("provideAddFundsModels")
    fun `GIVEN balance state WHEN create THEN add-funds banner visibility matches`(
        model: AddFundsModel,
    ) = runTest {
        // Arrange
        stubAccountStatusList(balance = model.balance)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.AddFunds }).isEqualTo(model.expectedShown)
    }

    internal data class AddFundsModel(val balance: TotalFiatBalance, val expectedShown: Boolean)

    private fun provideAddFundsModels() = listOf(
        // Shown only when the balance is loaded and zero.
        AddFundsModel(TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL), expectedShown = true),
        AddFundsModel(TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL), expectedShown = false),
        AddFundsModel(TotalFiatBalance.Loading, expectedShown = false),
        AddFundsModel(TotalFiatBalance.Failed, expectedShown = false),
    )
    // endregion

    // region Critical notifications (cold wallet only)
    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN backup problematic flag WHEN create THEN backup-error banner visibility matches`(
        problematic: Boolean,
    ) = runTest {
        // Arrange
        every { isWalletBackupProblematicUseCase(coldWallet) } returns problematic

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.BackupError }).isEqualTo(problematic)
    }

    @Test
    fun `GIVEN backup problematic WHEN create on hot wallet THEN backup-error banner is hidden`() = runTest {
        // Arrange
        every { isWalletBackupProblematicUseCase(any()) } returns true

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.BackupError }).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN release firmware flag WHEN create THEN dev-card banner visibility matches`(
        isRelease: Boolean,
    ) = runTest {
        // Arrange
        every { coldResolver.isReleaseFirmwareType() } returns isRelease

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.DevCard }).isEqualTo(!isRelease)
    }

    @ParameterizedTest
    @MethodSource("provideFailedValidationModels")
    fun `GIVEN release and attestation flags WHEN create THEN failed-validation banner visibility matches`(
        model: FailedValidationModel,
    ) = runTest {
        // Arrange
        every { coldResolver.isReleaseFirmwareType() } returns model.isRelease
        every { coldResolver.isAttestationFailed() } returns model.isAttestationFailed

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.FailedCardValidation }).isEqualTo(model.expectedShown)
    }

    internal data class FailedValidationModel(
        val isRelease: Boolean,
        val isAttestationFailed: Boolean,
        val expectedShown: Boolean,
    )

    private fun provideFailedValidationModels() = listOf(
        // Only release firmware with a failed attestation triggers the banner.
        FailedValidationModel(isRelease = true, isAttestationFailed = true, expectedShown = true),
        FailedValidationModel(isRelease = true, isAttestationFailed = false, expectedShown = false),
        FailedValidationModel(isRelease = false, isAttestationFailed = true, expectedShown = false),
    )

    @ParameterizedTest
    @MethodSource("provideLowSignaturesModels")
    fun `GIVEN remaining signatures WHEN create THEN low-signatures banner visibility matches`(
        model: LowSignaturesModel,
    ) = runTest {
        // Arrange
        every { coldResolver.getRemainingSignatures() } returns model.remainingSignatures

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.LowSignatures }).isEqualTo(model.expectedShown)
    }

    internal data class LowSignaturesModel(val remainingSignatures: Int?, val expectedShown: Boolean)

    private fun provideLowSignaturesModels() = listOf(
        // Shown only when the count is known and at/below the threshold (10).
        LowSignaturesModel(remainingSignatures = 10, expectedShown = true),
        LowSignaturesModel(remainingSignatures = 1, expectedShown = true),
        LowSignaturesModel(remainingSignatures = 11, expectedShown = false),
        LowSignaturesModel(remainingSignatures = null, expectedShown = false),
    )
    // endregion

    // region DemoCard
    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN demo card flag WHEN create THEN demo-card banner visibility matches`(isDemo: Boolean) = runTest {
        // Arrange
        every { isDemoCardUseCase(CARD_ID) } returns isDemo

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.DemoCard }).isEqualTo(isDemo)
    }

    @Test
    fun `GIVEN hot wallet WHEN create THEN demo-card banner is hidden`() = runTest {
        // Arrange
        every { isDemoCardUseCase(any()) } returns true

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.DemoCard }).isTrue()
    }
    // endregion

    // region MissingAddresses
    @Test
    fun `GIVEN missed derivation currencies WHEN create THEN missing-addresses banner is shown`() = runTest {
        // Arrange
        stubAccountStatusList(
            balance = LOADED_NON_ZERO,
            currencies = listOf(
                missedDerivationStatus(currencyFactory.ethereum),
                missedDerivationStatus(currencyFactory.stellar),
            ),
        )

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        val banner = result.filterIsInstance<WalletNotificationUM.MissingAddresses>().single()
        assertThat(banner.missingAddressesCount).isEqualTo(2)
        assertThat(banner.isHotWallet).isFalse()
    }

    @Test
    fun `GIVEN no missed derivation currencies WHEN create THEN missing-addresses banner is hidden`() = runTest {
        // Arrange
        stubAccountStatusList(balance = LOADED_NON_ZERO, currencies = emptyList())

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.MissingAddresses }).isTrue()
    }

    @Test
    fun `GIVEN hot wallet with missed derivation WHEN create THEN missing-addresses banner marks hot wallet`() =
        runTest {
            // Arrange
            stubAccountStatusList(
                balance = LOADED_NON_ZERO,
                currencies = listOf(missedDerivationStatus(currencyFactory.ethereum)),
            )

            // Act
            val result = factory.create(hotWallet, clickIntents).first()

            // Assert
            val banner = result.filterIsInstance<WalletNotificationUM.MissingAddresses>().single()
            assertThat(banner.isHotWallet).isTrue()
            assertThat(banner.missingAddressesCount).isEqualTo(1)
        }
    // endregion

    // region MissingBackup
    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN need-to-backup flag WHEN create THEN missing-backup banner visibility matches`(
        needToBackup: Boolean,
    ) = runTest {
        // Arrange
        every { isNeedToBackupUseCase(WALLET_ID) } returns flowOf(needToBackup)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.MissingBackup }).isEqualTo(needToBackup)
    }
    // endregion

    // region TestnetCard
    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN test card flag WHEN create THEN testnet-card banner visibility matches`(isTestCard: Boolean) = runTest {
        // Arrange
        every { coldResolver.isTestCard() } returns isTestCard

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.TestnetCard }).isEqualTo(isTestCard)
    }
    // endregion

    // region SomeNetworksUnreachable
    @Test
    fun `GIVEN unreachable currency WHEN create THEN unreachable-networks banner is shown`() = runTest {
        // Arrange
        stubAccountStatusList(
            balance = LOADED_NON_ZERO,
            currencies = listOf(unreachableStatus(currencyFactory.ethereum)),
        )

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.SomeNetworksUnreachable }).isTrue()
    }

    @Test
    fun `GIVEN no unreachable currency WHEN create THEN unreachable-networks banner is hidden`() = runTest {
        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.SomeNetworksUnreachable }).isTrue()
    }
    // endregion

    // region CloreMigration
    @ParameterizedTest
    @MethodSource("provideCloreMigrationModels")
    fun `GIVEN clore currency and multi-currency flag WHEN create THEN clore-migration banner visibility matches`(
        model: CloreMigrationModel,
    ) = runTest {
        // Arrange
        mockkObject(BlockchainUtils)
        try {
            every { BlockchainUtils.isClore(any()) } returns model.hasCloreCurrency
            every { coldWallet.isMultiCurrency } returns model.isMultiCurrency
            stubAccountStatusList(
                balance = LOADED_NON_ZERO,
                currencies = listOf(plainStatus(currencyFactory.ethereum)),
            )

            // Act
            val result = factory.create(coldWallet, clickIntents).first()

            // Assert
            assertThat(result.any { it is WalletNotificationUM.CloreMigration }).isEqualTo(model.expectedShown)
        } finally {
            unmockkObject(BlockchainUtils)
        }
    }

    internal data class CloreMigrationModel(
        val hasCloreCurrency: Boolean,
        val isMultiCurrency: Boolean,
        val expectedShown: Boolean,
    )

    private fun provideCloreMigrationModels() = listOf(
        // Shown only for a multi-currency wallet that holds a Clore currency.
        CloreMigrationModel(hasCloreCurrency = true, isMultiCurrency = true, expectedShown = true),
        CloreMigrationModel(hasCloreCurrency = true, isMultiCurrency = false, expectedShown = false),
        CloreMigrationModel(hasCloreCurrency = false, isMultiCurrency = true, expectedShown = false),
    )
    // endregion

    // region NumberOfSignedHashesIncorrect
    @ParameterizedTest
    @MethodSource("provideBooleans")
    fun `GIVEN signed hashes flag WHEN create THEN signed-hashes banner visibility matches`(
        hasSignedHashes: Boolean,
    ) = runTest {
        // Arrange
        every { hasSingleWalletSignedHashesUseCase(any(), any()) } returns flowOf(hasSignedHashes)
        stubAccountStatusList(
            balance = LOADED_NON_ZERO,
            currencies = listOf(plainStatus(currencyFactory.ethereum)),
        )

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.NumberOfSignedHashesIncorrect }).isEqualTo(hasSignedHashes)
    }

    @Test
    fun `GIVEN hot wallet WHEN create THEN signed-hashes banner is hidden`() = runTest {
        // Arrange
        every { hasSingleWalletSignedHashesUseCase(any(), any()) } returns flowOf(true)
        stubAccountStatusList(
            balance = LOADED_NON_ZERO,
            currencies = listOf(plainStatus(currencyFactory.ethereum)),
        )

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.NumberOfSignedHashesIncorrect }).isTrue()
    }
    // endregion

    // region FinishWalletActivation (hot wallet only)
    @ParameterizedTest
    @MethodSource("provideFinishActivationModels")
    fun `GIVEN hot wallet state WHEN create THEN finish-activation banner visibility matches`(
        model: FinishActivationModel,
    ) = runTest {
        // Arrange
        val hotWalletId = mockk<HotWalletId> { every { authType } returns model.authType }
        every { hotWallet.hotWalletId } returns hotWalletId
        every { hotWallet.backedUp } returns model.backedUp
        every { getAccessCodeSkippedUseCase(WALLET_ID) } returns flowOf(model.accessCodeSkipped)
        stubAccountStatusList(balance = model.balance)

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.FinishWalletActivation }).isEqualTo(model.expectedShown)
    }

    internal data class FinishActivationModel(
        val backedUp: Boolean,
        val authType: HotWalletId.AuthType,
        val accessCodeSkipped: Boolean,
        val balance: TotalFiatBalance,
        val expectedShown: Boolean,
    )

    private fun provideFinishActivationModels() = listOf(
        // Backed up and access-code protected — nothing to finish.
        FinishActivationModel(
            backedUp = true,
            authType = HotWalletId.AuthType.Password,
            accessCodeSkipped = false,
            balance = LOADED_NON_ZERO,
            expectedShown = false,
        ),
        // Not backed up — must finish activation.
        FinishActivationModel(
            backedUp = false,
            authType = HotWalletId.AuthType.Password,
            accessCodeSkipped = false,
            balance = LOADED_NON_ZERO,
            expectedShown = true,
        ),
        // No access code set and not skipped — must finish activation.
        FinishActivationModel(
            backedUp = true,
            authType = HotWalletId.AuthType.NoPassword,
            accessCodeSkipped = false,
            balance = LOADED_NON_ZERO,
            expectedShown = true,
        ),
        // No access code but skipped — nothing to finish.
        FinishActivationModel(
            backedUp = true,
            authType = HotWalletId.AuthType.NoPassword,
            accessCodeSkipped = true,
            balance = LOADED_NON_ZERO,
            expectedShown = false,
        ),
        // Balance still loading — banner suppressed even though activation is pending.
        FinishActivationModel(
            backedUp = false,
            authType = HotWalletId.AuthType.Password,
            accessCodeSkipped = false,
            balance = TotalFiatBalance.Loading,
            expectedShown = false,
        ),
        // Zero balance shows the add-funds banner instead, so finish-activation is suppressed.
        FinishActivationModel(
            backedUp = false,
            authType = HotWalletId.AuthType.Password,
            accessCodeSkipped = false,
            balance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            expectedShown = false,
        ),
    )

    @Test
    fun `GIVEN cold wallet WHEN create THEN finish-activation banner is hidden`() = runTest {
        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.FinishWalletActivation }).isTrue()
    }
    // endregion

    // region AssetsDiscoveryCompleted (hot wallet only)
    @Test
    fun `GIVEN completed assets discovery WHEN create THEN assets-discovery banner is shown`() = runTest {
        // Arrange
        every { observeAssetsDiscoveryUseCase(WALLET_ID) } returns flowOf(AssetsDiscoveryProgress.Completed)

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.AssetsDiscoveryCompleted }).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideNonCompletedProgress")
    fun `GIVEN non-completed assets discovery WHEN create THEN assets-discovery banner is hidden`(
        progress: AssetsDiscoveryProgress,
    ) = runTest {
        // Arrange
        every { observeAssetsDiscoveryUseCase(WALLET_ID) } returns flowOf(progress)

        // Act
        val result = factory.create(hotWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.AssetsDiscoveryCompleted }).isTrue()
    }

    private fun provideNonCompletedProgress() = listOf(
        AssetsDiscoveryProgress.Idle,
        AssetsDiscoveryProgress.InProgress(completedNetworks = 1, totalNetworks = 3),
    )

    @Test
    fun `GIVEN cold wallet WHEN create THEN assets-discovery banner is hidden`() = runTest {
        // Arrange — the use case is only observed for hot wallets; cold wallets use Idle.
        every { observeAssetsDiscoveryUseCase(any()) } returns flowOf(AssetsDiscoveryProgress.Completed)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.AssetsDiscoveryCompleted }).isTrue()
    }
    // endregion

    // region SoftUpdate
    @Test
    fun `GIVEN optional update WHEN create THEN soft-update banner is shown`() = runTest {
        // Arrange
        every { getAppUpdateStateUseCase.getBannerStateFlow() } returns flowOf(AppUpdateState.OptionalUpdate)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.SoftUpdateAvailable }).isTrue()
    }

    @ParameterizedTest
    @MethodSource("provideNonOptionalUpdateStates")
    fun `GIVEN non-optional update state WHEN create THEN soft-update banner is hidden`(
        state: AppUpdateState,
    ) = runTest {
        // Arrange
        every { getAppUpdateStateUseCase.getBannerStateFlow() } returns flowOf(state)

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.none { it is WalletNotificationUM.SoftUpdateAvailable }).isTrue()
    }

    private fun provideNonOptionalUpdateStates() = listOf(
        AppUpdateState.NoUpdate,
        AppUpdateState.ForceUpdate,
        AppUpdateState.Brick,
        AppUpdateState.OsTooOld,
    )
    // endregion

    // region TangemPay warnings
    @ParameterizedTest
    @MethodSource("provideTangemPayModels")
    fun `GIVEN payment account status WHEN create THEN tangem-pay banner matches`(
        model: TangemPayModel,
    ) = runTest {
        // Arrange
        val paymentStatus = AccountStatus.Payment(
            account = mockk<Account.Payment>(relaxed = true),
            value = model.value,
        )
        stubAccountStatusList(balance = LOADED_NON_ZERO, accountStatuses = listOf(paymentStatus))

        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(result.any { it is WalletNotificationUM.TangemPayRefreshNeeded })
            .isEqualTo(model.expectRefreshNeeded)
        assertThat(result.any { it is WalletNotificationUM.TangemPayPromo }).isEqualTo(model.expectPromo)
        assertThat(result.any { it is WalletNotificationUM.TangemPayUnreachable }).isEqualTo(model.expectUnreachable)
    }

    internal data class TangemPayModel(
        val value: PaymentAccountStatusValue,
        val expectRefreshNeeded: Boolean = false,
        val expectPromo: Boolean = false,
        val expectUnreachable: Boolean = false,
    )

    private fun provideTangemPayModels() = listOf(
        TangemPayModel(value = PaymentAccountStatusValue.Error.NotSynced, expectRefreshNeeded = true),
        TangemPayModel(value = PaymentAccountStatusValue.NotCreated, expectPromo = true),
        TangemPayModel(value = PaymentAccountStatusValue.Error.Unavailable, expectUnreachable = true),
        // Non-actionable statuses produce no TangemPay notification.
        TangemPayModel(value = PaymentAccountStatusValue.Loading),
    )

    @Test
    fun `GIVEN no payment account status WHEN create THEN no tangem-pay banner is shown`() = runTest {
        // Act
        val result = factory.create(coldWallet, clickIntents).first()

        // Assert
        assertThat(
            result.none {
                it is WalletNotificationUM.TangemPayRefreshNeeded ||
                    it is WalletNotificationUM.TangemPayPromo ||
                    it is WalletNotificationUM.TangemPayUnreachable
            },
        ).isTrue()
    }
    // endregion

    private fun stubAccountStatusList(
        balance: TotalFiatBalance,
        currencies: List<CryptoCurrencyStatus> = emptyList(),
        accountStatuses: List<AccountStatus> = emptyList(),
    ) {
        val list = mockk<AccountStatusList> {
            every { totalFiatBalance } returns balance
            every { flattenCurrencies() } returns currencies
            every { this@mockk.accountStatuses } returns accountStatuses
        }
        every {
            singleAccountStatusListSupplier(any<SingleAccountStatusListProducer.Params>())
        } returns flowOf(list)
    }

    private fun missedDerivationStatus(currency: CryptoCurrency) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
    )

    private fun unreachableStatus(currency: CryptoCurrency) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
    )

    private fun plainStatus(currency: CryptoCurrency) = CryptoCurrencyStatus(
        currency = currency,
        value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
    )

    private fun provideBooleans() = listOf(true, false)

    private companion object {
        val WALLET_ID = UserWalletId("01")
        const val CARD_ID = "cardId"
        val LOADED_NON_ZERO = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL)
    }
}