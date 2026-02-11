package com.tangem.domain.wallets.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.models.ApplicationId
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.models.UserWalletRemoteInfo
import com.tangem.domain.wallets.repository.WalletsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UpdateRemoteWalletsInfoUseCaseTest {

    private lateinit var useCase: UpdateRemoteWalletsInfoUseCase
    private lateinit var walletsRepository: WalletsRepository
    private lateinit var userWalletsSyncDelegate: UserWalletsSyncDelegate
    private lateinit var userWalletListRepository: UserWalletsListRepository
    private lateinit var generateWalletNameUseCase: GenerateWalletNameUseCase

    @Before
    fun setup() {
        walletsRepository = mockk()
        userWalletsSyncDelegate = mockk()
        userWalletListRepository = mockk {
            every { userWallets } returns MutableStateFlow<List<UserWallet>?>(emptyList())
        }
        generateWalletNameUseCase = mockk()
        useCase = UpdateRemoteWalletsInfoUseCase(
            walletsRepository = walletsRepository,
            userWalletsSyncDelegate = userWalletsSyncDelegate,
            userWalletsListRepository = userWalletListRepository,
            generateWalletNameUseCase = generateWalletNameUseCase,
        )
    }

    @Test
    fun `GIVEN wallets with non-empty names WHEN invoke THEN return Right with Unit`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletsInfo = listOf(
            UserWalletRemoteInfo(UserWalletId("0A0B0C0D"), "Wallet 1", true),
            UserWalletRemoteInfo(UserWalletId("0E0F1011"), "Wallet 2", true),
        )
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns walletsInfo
        coEvery { userWalletsSyncDelegate.syncWallets(walletsInfo) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { walletsRepository.getWalletsInfo(applicationId.value) }
        coVerify { userWalletsSyncDelegate.syncWallets(walletsInfo) }
    }

    @Test
    fun `GIVEN wallets with empty names WHEN invoke THEN filter out wallets with empty names`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletWithName = UserWalletRemoteInfo(UserWalletId("0A0B0C0D"), "Wallet 1", true)
        val walletWithEmptyName = UserWalletRemoteInfo(UserWalletId("0E0F1011"), "", true)
        val walletsInfo = listOf(walletWithName, walletWithEmptyName)
        val filteredWallets = listOf(walletWithName)
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns walletsInfo
        coEvery { userWalletsSyncDelegate.syncWallets(filteredWallets) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { userWalletsSyncDelegate.syncWallets(filteredWallets) }
    }

    @Test
    fun `GIVEN empty wallets list WHEN invoke THEN sync with empty list`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletsInfo = emptyList<UserWalletRemoteInfo>()
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns walletsInfo
        coEvery { userWalletsSyncDelegate.syncWallets(walletsInfo) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { userWalletsSyncDelegate.syncWallets(emptyList()) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke THEN return Left with Throwable`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val exception = RuntimeException("Network error")
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } throws exception

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isLeft()).isTrue()
        result.onLeft { throwable ->
            assertThat(throwable).isEqualTo(exception)
        }
    }

    @Test
    fun `GIVEN syncWallets throws exception WHEN invoke THEN return Left with Throwable`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletsInfo = listOf(
            UserWalletRemoteInfo(UserWalletId("0A0B0C0D"), "Wallet 1", true),
        )
        val exception = RuntimeException("Sync error")
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns walletsInfo
        coEvery { userWalletsSyncDelegate.syncWallets(walletsInfo) } throws exception

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isLeft()).isTrue()
        result.onLeft { throwable ->
            assertThat(throwable).isEqualTo(exception)
        }
    }

    @Test
    fun `GIVEN all wallets have empty names WHEN invoke THEN sync with empty list`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletsInfo = listOf(
            UserWalletRemoteInfo(UserWalletId("0A0B0C0D"), "", true),
            UserWalletRemoteInfo(UserWalletId("0E0F1011"), "", true),
        )
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns walletsInfo
        coEvery { userWalletsSyncDelegate.syncWallets(emptyList()) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { userWalletsSyncDelegate.syncWallets(emptyList()) }
    }

    @Test
    fun `GIVEN hot wallet with blank name WHEN invoke THEN generate name using invokeForHot`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletId = UserWalletId("0A0B0C0D")
        val hotWallet = mockk<UserWallet.Hot> {
            every { this@mockk.walletId } returns walletId
        }
        every { userWalletListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet))

        val remoteWalletWithBlankName = UserWalletRemoteInfo(walletId, "", true)
        val generatedName = "Wallet"
        every { generateWalletNameUseCase.invokeForHot() } returns generatedName

        val expectedWallet = UserWalletRemoteInfo(walletId, generatedName, true)
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns listOf(remoteWalletWithBlankName)
        coEvery { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { generateWalletNameUseCase.invokeForHot() }
        coVerify { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) }
    }

    @Test
    fun `GIVEN cold wallet with blank name WHEN invoke THEN generate name using productType`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val walletId = UserWalletId("0A0B0C0D")
        val mockScanResponse = mockk<ScanResponse>(relaxed = true) {
            every { productType } returns ProductType.Wallet2
            every { card } returns mockk(relaxed = true)
        }
        val coldWallet = mockk<UserWallet.Cold> {
            every { this@mockk.walletId } returns walletId
            every { scanResponse } returns mockScanResponse
        }
        every { userWalletListRepository.userWallets } returns MutableStateFlow(listOf(coldWallet))

        val remoteWalletWithBlankName = UserWalletRemoteInfo(walletId, "", true)
        val generatedName = "Wallet"
        every { generateWalletNameUseCase.invoke(any(), any(), any()) } returns generatedName

        val expectedWallet = UserWalletRemoteInfo(walletId, generatedName, true)
        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns listOf(remoteWalletWithBlankName)
        coEvery { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { generateWalletNameUseCase.invoke(any(), any(), any()) }
        coVerify { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) }
    }

    @Test
    fun `GIVEN wallet with blank name not in repository WHEN invoke THEN filter it out`() = runTest {
        // GIVEN
        val applicationId = ApplicationId("test-app-id")
        val knownWalletId = UserWalletId("0A0B0C0D")
        val unknownWalletId = UserWalletId("0E0F1011")

        val hotWallet = mockk<UserWallet.Hot> {
            every { this@mockk.walletId } returns knownWalletId
        }
        every { userWalletListRepository.userWallets } returns MutableStateFlow(listOf(hotWallet))
        every { generateWalletNameUseCase.invokeForHot() } returns "Wallet"

        val knownWalletRemote = UserWalletRemoteInfo(knownWalletId, "", true)
        val unknownWalletRemote = UserWalletRemoteInfo(unknownWalletId, "", true)
        val expectedWallet = UserWalletRemoteInfo(knownWalletId, "Wallet", true)

        coEvery { walletsRepository.getWalletsInfo(applicationId.value) } returns listOf(knownWalletRemote, unknownWalletRemote)
        coEvery { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) } returns Unit.right()

        // WHEN
        val result = useCase(applicationId)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify { userWalletsSyncDelegate.syncWallets(listOf(expectedWallet)) }
    }
}