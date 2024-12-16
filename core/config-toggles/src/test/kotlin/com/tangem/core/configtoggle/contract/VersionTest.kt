package com.tangem.core.configtoggle.contract

import com.google.common.truth.Truth
import com.tangem.core.configtoggle.version.Version
import org.junit.Test

/**
[REDACTED_AUTHOR]
 */
internal class VersionTest {

    @Test
    fun `right versions order`() {
        val major = 1
        val minor = 2
        val fix = 3
        val versionString = "$major.$minor.$fix"

        val version = Version.create(versionString)

        Truth.assertThat(version?.getMajorVersion()).isEqualTo(major)
        Truth.assertThat(version?.getMinorVersion()).isEqualTo(minor)
        Truth.assertThat(version?.getFixVersion()).isEqualTo(fix)
    }

    @Test
    fun `major version skipped`() {
        Truth.assertThat(Version.create(".0.0")).isNull()
    }

    @Test
    fun `minor version skipped`() {
        Truth.assertThat(Version.create("0..0")).isNull()
    }

    @Test
    fun `fix version skipped`() {
        Truth.assertThat(Version.create("0.0.")).isNull()
    }

    @Test
    fun `optional fix version`() {
        val actual = Version.create("0.0")
        val expected = Version(major = 0, minor = 0)

        Truth.assertThat(actual).isEquivalentAccordingToCompareTo(expected)
    }

    @Test
    fun `full form version`() {
        val actual = Version.create("0.0.0")
        val expected = Version(major = 0, minor = 0, fix = 0)

        Truth.assertThat(actual).isEquivalentAccordingToCompareTo(expected)
    }

    @Test
    fun `compareTo if major version of one is greater than the other`() {
        val one = Version.create("1.0.0")
        val other = Version(major = 0, minor = 1, fix = 1)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_GREATER_THAN_OTHER)
    }

    @Test
    fun `compareTo if minor version of one is greater than the other`() {
        val one = Version.create("1.1.0")
        val other = Version(major = 1, minor = 0, fix = 1)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_GREATER_THAN_OTHER)
    }

    @Test
    fun `compareTo if fix version of one is greater than the other`() {
        val one = Version.create("1.1.1")
        val other = Version(major = 1, minor = 1, fix = 0)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_GREATER_THAN_OTHER)
    }

    @Test
    fun `compareTo if one and other's fix version skipped`() {
        val one = Version.create("1.1")
        val other = Version(major = 1, minor = 1)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_EQUAL_TO_OTHER)
    }

    @Test
    fun `compareTo if one's fix version skipped`() {
        val one = Version.create("1.1")
        val other = Version(major = 1, minor = 1, fix = 0)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_LESS_THAN_OTHER)
    }

    @Test
    fun `compareTo if other's fix version skipped`() {
        val one = Version.create("1.1.0")
        val other = Version(major = 1, minor = 1)

        val actual = one?.compareTo(other)

        Truth.assertThat(actual).isEqualTo(ONE_IS_GREATER_THAN_OTHER)
    }

    private companion object {
        const val ONE_IS_GREATER_THAN_OTHER = 1
        const val ONE_IS_EQUAL_TO_OTHER = 0
        const val ONE_IS_LESS_THAN_OTHER = -1
    }
}