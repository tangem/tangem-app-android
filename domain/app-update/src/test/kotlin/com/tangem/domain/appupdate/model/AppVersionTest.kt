package com.tangem.domain.appupdate.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class AppVersionTest {

    @Test
    fun `GIVEN single segment WHEN parse THEN parsed`() {
        assertThat(AppVersion.parseOrNull("14")).isNotNull()
    }

    @Test
    fun `GIVEN major and minor WHEN parse THEN parsed`() {
        assertThat(AppVersion.parseOrNull("5.30")).isNotNull()
    }

    @Test
    fun `GIVEN major minor fix WHEN parse THEN parsed`() {
        assertThat(AppVersion.parseOrNull("5.40.1")).isNotNull()
    }

    @Test
    fun `GIVEN empty minor part WHEN parse THEN null`() {
        assertThat(AppVersion.parseOrNull("5..1")).isNull()
    }

    @Test
    fun `GIVEN non-numeric part WHEN parse THEN null`() {
        assertThat(AppVersion.parseOrNull("5.x")).isNull()
    }

    @Test
    fun `GIVEN blank WHEN parse THEN null`() {
        assertThat(AppVersion.parseOrNull("")).isNull()
    }

    @Test
    fun `GIVEN trailing dot WHEN parse THEN null`() {
        assertThat(AppVersion.parseOrNull("5.")).isNull()
    }

    @Test
    fun `GIVEN older version WHEN compare THEN less than newer`() {
        assertThat(AppVersion.parseOrNull("14")!! < AppVersion.parseOrNull("15.0")!!).isTrue()
    }

    @Test
    fun `GIVEN fix difference WHEN compare THEN ordered`() {
        assertThat(AppVersion.parseOrNull("5.40.1")!! > AppVersion.parseOrNull("5.40.0")!!).isTrue()
    }

    @Test
    fun `GIVEN missing minor WHEN compare to explicit zero THEN equal`() {
        val implicit = AppVersion.parseOrNull("14")!!
        val explicit = AppVersion.parseOrNull("14.0")!!

        assertThat(implicit.compareTo(explicit)).isEqualTo(0)
    }

    @Test
    fun `GIVEN build-type suffix WHEN parse THEN parsed without suffix`() {
        assertThat(AppVersion.parseOrNull("6.1-internal")).isNotNull()
    }

    @Test
    fun `GIVEN snapshot fallback WHEN parse THEN parsed`() {
        assertThat(AppVersion.parseOrNull("1.0.0-SNAPSHOT")).isNotNull()
    }

    @Test
    fun `GIVEN suffixed version WHEN compare to clean THEN equal`() {
        val suffixed = AppVersion.parseOrNull("6.1-internal")!!
        val clean = AppVersion.parseOrNull("6.1")!!

        assertThat(suffixed.compareTo(clean)).isEqualTo(0)
    }
}