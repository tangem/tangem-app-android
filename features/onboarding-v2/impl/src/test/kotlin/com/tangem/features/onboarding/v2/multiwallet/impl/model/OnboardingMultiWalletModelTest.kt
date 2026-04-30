package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion as SdkFirmwareVersion
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.converter.ArtworkUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.ArtworkModel
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onboarding.repository.OnboardingRepository
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.title.OnboardingTitle
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnboardingMultiWalletModelTest {

    private val analyticsHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val router: Router = mockk(relaxUnitFun = true)
    private val backupServiceHolder: BackupServiceHolder = mockk()
    private val backupServiceWeakRef: WeakReference<BackupService> = WeakReference(null)
    private val onboardingRepository: OnboardingRepository = mockk(relaxUnitFun = true)
    private val getCardImageUseCase: GetCardImageUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val artworkUMConverter: ArtworkUMConverter = mockk()
    private val paramsContainer: ParamsContainer = mockk()
    private val titleProvider: TitleProvider = mockk(relaxUnitFun = true)

    private val card1Id = "card-id-1"
    private val card1PublicKey = byteArrayOf(1, 2, 3)
    private val card1ManufacturerName = "Tangem"
    private val card1Manufacturer = CardDTO.Manufacturer(
        name = card1ManufacturerName,
        manufactureDate = Date(0),
        signature = null,
    )
    private val card1FirmwareVersionDto = CardDTO.FirmwareVersion(
        major = 6,
        minor = 33,
        patch = 0,
        type = SdkFirmwareVersion.FirmwareType.Release,
    )
    private val card1SdkFirmwareVersion = SdkFirmwareVersion(major = 6, minor = 33)
    private val cardDto: CardDTO = mockk()
    private val scanResponse: ScanResponse = mockk()
    private val artwork1Model = ArtworkModel(verifiedArtwork = null, defaultUrl = "default-url-1")
    private val artwork1Um = ArtworkUM(verifiedArtwork = null, defaultUrl = "default-url-1")

    private val params: OnboardingMultiWalletComponent.Params = mockk {
        every { titleProvider } returns this@OnboardingMultiWalletModelTest.titleProvider
        every { scanResponse } returns this@OnboardingMultiWalletModelTest.scanResponse
    }

    @BeforeEach
    fun setUp() {
        every { paramsContainer.require<OnboardingMultiWalletComponent.Params>() } returns params
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.Onboarding

        every { cardDto.cardId } returns card1Id
        every { cardDto.cardPublicKey } returns card1PublicKey
        every { cardDto.manufacturer } returns card1Manufacturer
        every { cardDto.firmwareVersion } returns card1FirmwareVersionDto
        every { cardDto.wallets } returns emptyList()
        every { cardDto.backupStatus } returns null

        every { scanResponse.card } returns cardDto
        every { scanResponse.productType } returns ProductType.Wallet
        every { scanResponse.primaryCard } returns null

        every { backupServiceHolder.backupService } returns backupServiceWeakRef

        coEvery {
            getCardImageUseCase.invoke(
                cardId = card1Id,
                cardPublicKey = card1PublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card1ManufacturerName,
                firmwareVersion = card1SdkFirmwareVersion,
            )
        } returns artwork1Model
        every { artworkUMConverter.convert(artwork1Model) } returns artwork1Um
    }

    @Test
    fun `WHEN model is created THEN OnboardingEvent Started is sent`() = runTest {
        createModel(this)
        advanceUntilIdle()

        verify { analyticsHandler.send(match<OnboardingEvent.Started> { true }) }
    }

    @Test
    fun `GIVEN UpgradeHotWallet mode WHEN model is created THEN title is common_tangem`() = runTest {
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.UpgradeHotWallet(
            userWalletId = UserWalletId("011"),
        )

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.common_tangem)),
            )
        }
    }

    @Test
    fun `GIVEN ContinueFinalize mode WHEN model is created THEN title is finalize_backup`() = runTest {
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.ContinueFinalize

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.onboarding_button_finalize_backup)),
            )
        }
    }

    @Test
    fun `GIVEN AddressSync mode WHEN model is created THEN title is biometrics`() = runTest {
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.AddressSync(
            userWalletId = UserWalletId("011"),
            isWalletStarted = false,
        )

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.onboarding_navbar_title_biometrics)),
            )
        }
    }

    @Test
    fun `GIVEN wallets present AND NoBackup AND no primary card WHEN created THEN title is creating_backup`() = runTest {
        every { cardDto.wallets } returns listOf(mockk())
        every { cardDto.backupStatus } returns CardDTO.BackupStatus.NoBackup
        every { scanResponse.primaryCard } returns null

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.onboarding_navbar_title_creating_backup)),
            )
        }
    }

    @Test
    fun `GIVEN wallets present AND NoBackup AND Wallet productType WHEN created THEN title is getting_started`() =
        runTest {
            every { cardDto.wallets } returns listOf(mockk())
            every { cardDto.backupStatus } returns CardDTO.BackupStatus.NoBackup
            every { scanResponse.primaryCard } returns mockk()
            every { scanResponse.productType } returns ProductType.Wallet

            createModel(this)
            advanceUntilIdle()

            verify {
                titleProvider.changeTitle(
                    title = OnboardingTitle(text = resourceReference(R.string.onboarding_getting_started)),
                )
            }
        }

    @Test
    fun `GIVEN wallets present AND NoBackup AND non-Wallet productType WHEN created THEN title is creating_backup`() =
        runTest {
            every { cardDto.wallets } returns listOf(mockk())
            every { cardDto.backupStatus } returns CardDTO.BackupStatus.NoBackup
            every { scanResponse.primaryCard } returns mockk()
            every { scanResponse.productType } returns ProductType.Wallet2

            createModel(this)
            advanceUntilIdle()

            verify {
                titleProvider.changeTitle(
                    title = OnboardingTitle(text = resourceReference(R.string.onboarding_navbar_title_creating_backup)),
                )
            }
        }

    @Test
    fun `GIVEN wallets present AND active backup WHEN created THEN title is finalize_backup`() = runTest {
        every { cardDto.wallets } returns listOf(mockk())
        every { cardDto.backupStatus } returns CardDTO.BackupStatus.Active(cardCount = 2)

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.onboarding_button_finalize_backup)),
            )
        }
    }

    @Test
    fun `GIVEN no wallets WHEN model is created THEN title is create_wallet_header`() = runTest {
        every { cardDto.wallets } returns emptyList()

        createModel(this)
        advanceUntilIdle()

        verify {
            titleProvider.changeTitle(
                title = OnboardingTitle(text = resourceReference(R.string.onboarding_create_wallet_header)),
            )
        }
    }

    @Test
    fun `WHEN model is created THEN loadCardArtwork updates artwork1 in uiState`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        coVerify {
            getCardImageUseCase.invoke(
                cardId = card1Id,
                cardPublicKey = card1PublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card1ManufacturerName,
                firmwareVersion = card1SdkFirmwareVersion,
            )
        }
        verify { artworkUMConverter.convert(artwork1Model) }
        Assertions.assertEquals(artwork1Um, model.uiState.value.artwork1)
    }

    @Test
    fun `GIVEN backups emit card2 WHEN subscribeToBackups THEN artwork2 is loaded and updated`() = runTest {
        val card2Info = card2BackupInfo()
        val artwork2Model = ArtworkModel(verifiedArtwork = null, defaultUrl = "default-url-2")
        val artwork2Um = ArtworkUM(verifiedArtwork = null, defaultUrl = "default-url-2")
        coEvery {
            getCardImageUseCase.invoke(
                cardId = card2Info.cardId,
                cardPublicKey = card2Info.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card2Info.manufacturer.name,
                firmwareVersion = card2Info.firmwareVersion,
            )
        } returns artwork2Model
        every { artworkUMConverter.convert(artwork2Model) } returns artwork2Um

        val model = createModel(this)
        advanceUntilIdle()

        model.backups.value = MultiWalletChildParams.Backup(card2 = card2Info)
        advanceUntilIdle()

        coVerify {
            getCardImageUseCase.invoke(
                cardId = card2Info.cardId,
                cardPublicKey = card2Info.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card2Info.manufacturer.name,
                firmwareVersion = card2Info.firmwareVersion,
            )
        }
        Assertions.assertEquals(artwork2Um, model.uiState.value.artwork2)
    }

    @Test
    fun `GIVEN backups emit card3 after card2 WHEN subscribeToBackups THEN artwork3 is loaded and updated`() = runTest {
        val card2Info = card2BackupInfo()
        val card3Info = card3BackupInfo()
        val artwork2Model = ArtworkModel(verifiedArtwork = null, defaultUrl = "default-url-2")
        val artwork2Um = ArtworkUM(verifiedArtwork = null, defaultUrl = "default-url-2")
        val artwork3Model = ArtworkModel(verifiedArtwork = null, defaultUrl = "default-url-3")
        val artwork3Um = ArtworkUM(verifiedArtwork = null, defaultUrl = "default-url-3")
        coEvery {
            getCardImageUseCase.invoke(
                cardId = card2Info.cardId,
                cardPublicKey = card2Info.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card2Info.manufacturer.name,
                firmwareVersion = card2Info.firmwareVersion,
            )
        } returns artwork2Model
        every { artworkUMConverter.convert(artwork2Model) } returns artwork2Um
        coEvery {
            getCardImageUseCase.invoke(
                cardId = card3Info.cardId,
                cardPublicKey = card3Info.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card3Info.manufacturer.name,
                firmwareVersion = card3Info.firmwareVersion,
            )
        } returns artwork3Model
        every { artworkUMConverter.convert(artwork3Model) } returns artwork3Um

        val model = createModel(this)
        advanceUntilIdle()

        model.backups.value = MultiWalletChildParams.Backup(card2 = card2Info)
        advanceUntilIdle()
        model.backups.value = MultiWalletChildParams.Backup(card2 = card2Info, card3 = card3Info)
        advanceUntilIdle()

        coVerify {
            getCardImageUseCase.invoke(
                cardId = card3Info.cardId,
                cardPublicKey = card3Info.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = card3Info.manufacturer.name,
                firmwareVersion = card3Info.firmwareVersion,
            )
        }
        Assertions.assertEquals(artwork3Um, model.uiState.value.artwork3)
    }

    @Test
    fun `GIVEN non-AddressSync mode WHEN onBack confirmed THEN router pop is called`() = runTest {
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.Onboarding
        coEvery { onboardingRepository.clearUnfinishedFinalizeOnboarding() } just Runs
        val dialogSlot = slot<DialogMessage>()
        every { uiMessageSender.send(capture(dialogSlot)) } just Runs

        val model = createModel(this)
        advanceUntilIdle()

        model.onBack()
        dialogSlot.captured.firstAction.onClick.invoke()
        advanceUntilIdle()

        coVerify { onboardingRepository.clearUnfinishedFinalizeOnboarding() }
        verify { router.pop(onComplete = any()) }
        verify(exactly = 0) { router.popTo(route = any(), onComplete = any()) }
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN AddressSync mode AND wallet started WHEN onBack confirmed THEN popTo Wallet is called`() = runTest {
        every { params.mode } returns OnboardingMultiWalletComponent.Mode.AddressSync(
            userWalletId = UserWalletId("011"),
            isWalletStarted = true,
        )
        coEvery { onboardingRepository.clearUnfinishedFinalizeOnboarding() } just Runs
        val dialogSlot = slot<DialogMessage>()
        every { uiMessageSender.send(capture(dialogSlot)) } just Runs

        val model = createModel(this)
        advanceUntilIdle()

        model.onBack()
        dialogSlot.captured.firstAction.onClick.invoke()
        advanceUntilIdle()

        coVerify { onboardingRepository.clearUnfinishedFinalizeOnboarding() }
        verify { router.popTo(route = AppRoute.Wallet, onComplete = any()) }
        verify(exactly = 0) { router.pop(onComplete = any()) }
        verify(exactly = 0) { router.replaceAll(routes = anyVararg(), onComplete = any()) }
    }

    @Test
    fun `GIVEN AddressSync mode AND wallet not started WHEN onBack confirmed THEN replaceAll Wallet is called`() =
        runTest {
            every { params.mode } returns OnboardingMultiWalletComponent.Mode.AddressSync(
                userWalletId = UserWalletId("011"),
                isWalletStarted = false,
            )
            coEvery { onboardingRepository.clearUnfinishedFinalizeOnboarding() } just Runs
            val dialogSlot = slot<DialogMessage>()
            every { uiMessageSender.send(capture(dialogSlot)) } just Runs

            val model = createModel(this)
            advanceUntilIdle()

            model.onBack()
            dialogSlot.captured.firstAction.onClick.invoke()
            advanceUntilIdle()

            coVerify { onboardingRepository.clearUnfinishedFinalizeOnboarding() }
            verify { router.replaceAll(routes = arrayOf(AppRoute.Wallet), onComplete = any()) }
            verify(exactly = 0) { router.pop(onComplete = any()) }
            verify(exactly = 0) { router.popTo(route = any(), onComplete = any()) }
        }

    private fun card2BackupInfo() = MultiWalletChildParams.Backup.BackupCardInfo(
        cardId = "card-id-2",
        cardPublicKey = byteArrayOf(4, 5, 6),
        manufacturer = Card.Manufacturer(name = "Tangem2", manufactureDate = Date(0), signature = null),
        firmwareVersion = SdkFirmwareVersion(major = 6, minor = 34),
    )

    private fun card3BackupInfo() = MultiWalletChildParams.Backup.BackupCardInfo(
        cardId = "card-id-3",
        cardPublicKey = byteArrayOf(7, 8, 9),
        manufacturer = Card.Manufacturer(name = "Tangem3", manufactureDate = Date(0), signature = null),
        firmwareVersion = SdkFirmwareVersion(major = 6, minor = 35),
    )

    private fun createModel(testScope: TestScope): OnboardingMultiWalletModel {
        return OnboardingMultiWalletModel(
            paramsContainer = paramsContainer,
            analyticsHandler = analyticsHandler,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            backupServiceHolder = backupServiceHolder,
            onboardingRepository = onboardingRepository,
            getCardImageUseCase = getCardImageUseCase,
            uiMessageSender = uiMessageSender,
            artworkUMConverter = artworkUMConverter,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}