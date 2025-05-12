package com.tangem.domain.walletconnect

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.reown.walletkit.client.Wallet
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.walletconnect.pair.AssociateNetworksDelegate
import com.tangem.data.walletconnect.pair.CaipNamespaceDelegate
import com.tangem.data.walletconnect.pair.DefaultWcPairUseCase
import com.tangem.data.walletconnect.pair.WcPairSdkDelegate
import com.tangem.data.walletconnect.utils.WcSdkSessionConverter
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.walletconnect.model.WcPairError
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.pair.WcPairState
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class DefaultWcPairUseCaseTest {

    private val sessionsManager: WcSessionsManager = mockk<WcSessionsManager>()
    private val associateNetworksDelegate: AssociateNetworksDelegate = mockk<AssociateNetworksDelegate>()
    private val caipNamespaceDelegate: CaipNamespaceDelegate = mockk<CaipNamespaceDelegate>()
    private val sdkDelegate: WcPairSdkDelegate = mockk<WcPairSdkDelegate>()
    private val blockAidVerifier: BlockAidVerifier = mockk<BlockAidVerifier>()

    private val url = "testUrl"
    private val source = WcPairRequest.Source.QR
    private val loading = WcPairState.Loading
    private val sdkProposal: Wallet.Model.SessionProposal
        get() = Wallet.Model.SessionProposal(
            pairingTopic = "",
            name = "",
            description = "",
            url = "",
            icons = listOf(),
            redirect = "",
            requiredNamespaces = mapOf(),
            optionalNamespaces = mapOf(),
            properties = mapOf(),
            proposerPublicKey = "",
            relayProtocol = "",
            relayData = "",
        )

    private val unsupportedDApp = "Apex Pro"
    private val unsupportedSdkProposal get() = sdkProposal.copy(name = unsupportedDApp)

    private val sessionForApprove: WcSessionApprove
        get() = WcSessionApprove(
            wallet = MockUserWalletFactory.create(),
            network = listOf(),
        )

    private val sdkApprove: Wallet.Params.SessionApprove
        get() = Wallet.Params.SessionApprove(
            proposerPublicKey = "",
            namespaces = mapOf(),
        )

    private val sdkApproveSuccess: Wallet.Model.SettledSessionResponse.Result
        get() = Wallet.Model.SettledSessionResponse.Result(
            session = sdkSession,
        )

    private val sdkSession: Wallet.Model.Session
        get() = Wallet.Model.Session(
            pairingTopic = "",
            topic = "",
            expiry = 0L,
            requiredNamespaces = mapOf(),
            optionalNamespaces = mapOf(),
            namespaces = mapOf(),
            metaData = null,
        )

    private val Wallet.Model.Session.sessionForSave: WcSession
        get() = WcSession(
            wallet = sessionForApprove.wallet,
            sdkModel = WcSdkSessionConverter.convert(this),
            securityStatus = CheckDAppResult.SAFE,
            networks = setOf(),
        )

    private fun useCaseFactory() = DefaultWcPairUseCase(
        sessionsManager = sessionsManager,
        associateNetworksDelegate = associateNetworksDelegate,
        caipNamespaceDelegate = caipNamespaceDelegate,
        sdkDelegate = sdkDelegate,
        blockAidVerifier = blockAidVerifier,
        pairRequest = WcPairRequest(userWalletId = UserWalletId(""), uri = url, source = source),
    )

    @Before
    fun setup() {
        coEvery { associateNetworksDelegate.associate(sdkProposal) } returns mapOf()
        coEvery {
            caipNamespaceDelegate.associate(
                sessionProposal = sdkProposal,
                userWallet = sessionForApprove.wallet,
                networks = sessionForApprove.network,
            )
        } returns mapOf()
    }

    @Test
    fun `pair, emmit proposal state and wait actions`() = runTest {
        coEvery { sdkDelegate.pair(url) } returns sdkProposal.right()
        coEvery { blockAidVerifier.verifyDApp(any()) } returns Either.catch { CheckDAppResult.SAFE }

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
                blockAidVerifier.verifyDApp(DAppData(sdkProposal.url))
            }
            assert(awaitItem() is WcPairState.Proposal)
            expectNoEvents()
        }
    }

    @Test
    fun `success pair and approve flow`() = runTest {
        val approveLoading = WcPairState.Approving.Loading(sessionForApprove)
        val sessionForSave = sdkSession.sessionForSave
        val result = WcPairState.Approving.Result(sessionForApprove, sessionForSave.right())

        coEvery { sdkDelegate.pair(url) } returns sdkProposal.right()
        coEvery { sdkDelegate.approve(sdkApprove) } returns sdkApproveSuccess.right()
        coEvery { sessionsManager.saveSession(sessionForSave) } returns Unit
        coEvery { blockAidVerifier.verifyDApp(any()) } returns Either.catch { CheckDAppResult.SAFE }

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
                blockAidVerifier.verifyDApp(DAppData(sdkProposal.url))
            }
            assert(awaitItem() is WcPairState.Proposal)
            useCase.approve(sessionForApprove)
            // ignore
            useCase.reject()

            assertEquals(approveLoading, awaitItem())
            coVerifyOrder {
                sdkDelegate.approve(sdkApprove)
                sessionsManager.saveSession(sessionForSave)
            }
            assertEquals(result, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `success pair and reject approving`() = runTest {
        val proposerPublicKey = sdkProposal.proposerPublicKey

        coEvery { sdkDelegate.pair(url) } returns sdkProposal.right()
        coEvery { sdkDelegate.rejectSession(proposerPublicKey) } returns Unit
        coEvery { blockAidVerifier.verifyDApp(any()) } returns Either.catch { CheckDAppResult.SAFE }

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
                blockAidVerifier.verifyDApp(DAppData(sdkProposal.url))
            }
            assert(awaitItem() is WcPairState.Proposal)
            useCase.reject()
            coVerifyOrder {
                sdkDelegate.rejectSession(sdkProposal.proposerPublicKey)
            }
            awaitComplete()
        }
    }

    @Test
    fun `success pair and reject unsupported dApp`() = runTest {
        coEvery { sdkDelegate.pair(url) } returns unsupportedSdkProposal.right()
        val unsupportedDAppError = WcPairState.Error(WcPairError.UnsupportedDApp)

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
            }
            assertEquals(unsupportedDAppError, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `complete on pair error`() = runTest {
        val error = WcPairError.ExternalApprovalError("error")
        coEvery { sdkDelegate.pair(url) } returns error.left()
        val errorState = WcPairState.Error(error)

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
            }
            assertEquals(errorState, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `complete on approve error`() = runTest {
        val approveLoading = WcPairState.Approving.Loading(sessionForApprove)
        val error = WcPairError.ExternalApprovalError("error").left()
        coEvery { sdkDelegate.pair(url) } returns sdkProposal.right()
        coEvery { sdkDelegate.approve(sdkApprove) } returns error
        coEvery { blockAidVerifier.verifyDApp(any()) } returns Either.catch { CheckDAppResult.SAFE }

        val errorResult = WcPairState.Approving.Result(sessionForApprove, error)

        val useCase = useCaseFactory()
        useCase.invoke().test {
            assertEquals(loading, awaitItem())
            coVerifyOrder {
                sdkDelegate.pair(url)
                associateNetworksDelegate.associate(sdkProposal)
                blockAidVerifier.verifyDApp(DAppData(sdkProposal.url))
            }
            assert(awaitItem() is WcPairState.Proposal)
            useCase.approve(sessionForApprove)

            assertEquals(approveLoading, awaitItem())
            coVerifyOrder {
                sdkDelegate.approve(sdkApprove)
            }

            assertEquals(errorResult, awaitItem())
            awaitComplete()
        }
    }
}