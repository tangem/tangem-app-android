package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.scenarios.*
import com.tangem.tap.domain.sdk.mocks.content.ShibaNoBackupMockContent
import com.tangem.tap.domain.sdk.mocks.content.ShibaNoBackupNoWalletsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2NoBackupMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2NoBackupNoWalletsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet3NoBackupMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet3NoBackupNoWalletsMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class OnboardingTest : BaseTestCase() {

    @AllureId("248")
    @DisplayName("Onboarding: 'Shiba' no wallets backup screen test")
    @Test
    fun shibaNoWalletsBackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = ShibaNoBackupNoWalletsMockContent)
            checkCreateWalletScreenForWalletNoWallets()
            openAndCheckBackupScreenForWalletNoWallets()
        }
    }

    @AllureId("3989")
    @DisplayName("Onboarding: 'Shiba' with wallets backup screen test")
    @Test
    fun shibaBackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = ShibaNoBackupMockContent)
            checkBackupScreen()
        }
    }

    @AllureId("246")
    @DisplayName("Onboarding: 'Wallet 2' no wallets backup screen test")
    @Test
    fun wallet2NoWalletsBackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = Wallet2NoBackupNoWalletsMockContent)
            checkCreateWalletScreenForWallet2NoWallets()
            openAndCheckBackupScreenForWallet2NoWallets()
        }
    }

    @AllureId("3990")
    @DisplayName("Onboarding: 'Wallet 2' with wallets backup screen test")
    @Test
    fun wallet2BackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = Wallet2NoBackupMockContent)
            checkBackupScreen()
        }
    }

    @AllureId("10816")
    @DisplayName("Onboarding: 'Wallet 3' no wallets backup screen test")
    @Test
    fun wallet3NoWalletsBackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = Wallet3NoBackupNoWalletsMockContent)
            // Wallet 3 shows the same Create-wallet/backup onboarding screens as Wallet 2 — reuse its helpers.
            checkCreateWalletScreenForWallet2NoWallets()
            openAndCheckBackupScreenForWallet2NoWallets()
        }
    }

    @AllureId("10820")
    @DisplayName("Onboarding: 'Wallet 3' with wallets backup screen test")
    @Test
    fun wallet3BackupScreenTest() {
        setupHooks().run {
            scanCard(mockContent = Wallet3NoBackupMockContent)
            checkBackupScreen()
        }
    }
}