package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.scenarios.*
import com.tangem.tap.domain.sdk.mocks.content.ShibaNoBackupMockContent
import com.tangem.tap.domain.sdk.mocks.content.ShibaNoBackupNoWalletsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2NoBackupMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2NoBackupNoWalletsMockContent
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
            scanCard(
                mockContent = ShibaNoBackupMockContent,
                alreadyActivatedDialogIsShown = true
            )
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
            scanCard(
                mockContent = Wallet2NoBackupMockContent,
                alreadyActivatedDialogIsShown = true
            )
            checkBackupScreen()
        }
    }
}