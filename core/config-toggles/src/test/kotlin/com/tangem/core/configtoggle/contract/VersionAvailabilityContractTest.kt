package com.tangem.core.configtoggle.contract

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.version.VersionAvailabilityContract
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class VersionAvailabilityContractTest {

    @Test
    fun `local version is undefined`() {
        val currentVersion = "0.0.0"
        val localVersion = "undefined"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isFalse()
    }

    @Test
    fun `invalid current version`() {
        val currentVersion = ".0.0"
        val localVersion = "0.0.0"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isFalse()
    }

    @Test
    fun `invalid local version`() {
        val currentVersion = "0.0.0"
        val localVersion = ".0.0"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isFalse()
    }

    @Test
    fun `current version is greater than local version`() {
        val currentVersion = "1.0.0"
        val localVersion = "0.0.0"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `current version is equal to local version`() {
        val currentVersion = "0.0.1"
        val localVersion = "0.0.1"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isTrue()
    }

    @Test
    fun `current version is less than local version`() {
        val currentVersion = "0.0.0"
        val localVersion = "0.1.0"

        val actual = VersionAvailabilityContract.invoke(currentVersion, localVersion)

        Truth.assertThat(actual).isFalse()
    }
}