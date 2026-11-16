package com.tangem.data.common.account

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SupportedAccountTypesTest {

    private val jointAccountFeatureToggles: JointAccountFeatureToggles = mockk()
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles = mockk()

    private val supportedAccountTypes = SupportedAccountTypes(
        jointAccountFeatureToggles = jointAccountFeatureToggles,
        collectiblesFeatureToggles = collectiblesFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountFeatureToggles, collectiblesFeatureToggles)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN toggles WHEN types and isSupported THEN only enabled record types are supported`(model: Model) {
        // Arrange
        every { jointAccountFeatureToggles.isJointAccountEnabled } returns model.isJointEnabled
        every { collectiblesFeatureToggles.isCollectiblesEnabled } returns model.isCollectiblesEnabled

        // Act
        val types = supportedAccountTypes.types
        val supportedTypes = WalletAccountDTO.Type.entries.filter(supportedAccountTypes::isSupported)
        val supported = recordTypes.filter { type -> supportedAccountTypes.isSupported(createRecord(type)) }

        // Assert
        assertThat(types).containsExactlyElementsIn(model.expectedTypes)
        assertThat(supportedTypes).containsExactlyElementsIn(model.expectedTypes)
        assertThat(supported).containsExactlyElementsIn(model.expectedSupported)
    }

    private fun createRecord(type: WalletAccountDTO.Type?) = WalletAccountDTO(
        id = "id",
        name = null,
        derivationIndex = 0,
        icon = "Letter",
        iconColor = "Azure",
        type = type,
    )

    data class Model(
        val isJointEnabled: Boolean,
        val isCollectiblesEnabled: Boolean,
        val expectedTypes: Set<WalletAccountDTO.Type>,
        val expectedSupported: List<WalletAccountDTO.Type?>,
    )

    private val recordTypes = listOf(null) + WalletAccountDTO.Type.entries

    private fun provideTestModels() = listOf(
        Model(
            isJointEnabled = false,
            isCollectiblesEnabled = false,
            expectedTypes = setOf(WalletAccountDTO.Type.CRYPTO),
            expectedSupported = listOf(null, WalletAccountDTO.Type.CRYPTO),
        ),
        Model(
            isJointEnabled = true,
            isCollectiblesEnabled = false,
            expectedTypes = setOf(WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.JOINT),
            expectedSupported = listOf(null, WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.JOINT),
        ),
        Model(
            isJointEnabled = false,
            isCollectiblesEnabled = true,
            expectedTypes = setOf(WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.COLLECTIBLES),
            expectedSupported = listOf(null, WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.COLLECTIBLES),
        ),
        Model(
            isJointEnabled = true,
            isCollectiblesEnabled = true,
            expectedTypes = WalletAccountDTO.Type.entries.toSet(),
            expectedSupported = listOf(null) + WalletAccountDTO.Type.entries,
        ),
    )
}