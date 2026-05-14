package com.tangem.features.onboarding.v2.multiwallet.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class UtilsTest {

    @ParameterizedTest
    @MethodSource("provideScreenTitleByStep")
    fun `GIVEN step WHEN screenTitleByStep THEN expected text reference is returned`(
        step: OnboardingMultiWalletState.Step,
        expected: TextReference,
    ) {
        val actual = screenTitleByStep(step)

        Assertions.assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        fun provideScreenTitleByStep(): List<Arguments> = listOf(
            Arguments.of(
                OnboardingMultiWalletState.Step.UpgradeWallet,
                resourceReference(R.string.common_tangem),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.CreateWallet,
                resourceReference(R.string.onboarding_create_wallet_header),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.SeedPhrase,
                resourceReference(R.string.onboarding_create_wallet_header),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.ChooseBackupOption,
                resourceReference(R.string.onboarding_getting_started),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.ScanPrimary,
                resourceReference(R.string.onboarding_navbar_title_creating_backup),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.AddBackupDevice,
                resourceReference(R.string.onboarding_navbar_title_creating_backup),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.AddressSync,
                resourceReference(R.string.onboarding_navbar_title_biometrics),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.Finalize,
                resourceReference(R.string.onboarding_button_finalize_backup),
            ),
            Arguments.of(
                OnboardingMultiWalletState.Step.Done,
                resourceReference(R.string.common_done),
            ),
        )
    }
}