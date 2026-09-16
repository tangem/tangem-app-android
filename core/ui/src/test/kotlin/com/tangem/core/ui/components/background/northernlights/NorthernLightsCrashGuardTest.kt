package com.tangem.core.ui.components.background.northernlights

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class NorthernLightsCrashGuardTest {

    private class FakeStore(private var state: NorthernLightsCrashGuard.State) : NorthernLightsCrashGuard.Store {

        var saved: NorthernLightsCrashGuard.State = state
            private set

        override fun load(): NorthernLightsCrashGuard.State = state

        override fun save(state: NorthernLightsCrashGuard.State) {
            this.state = state
            saved = state
        }
    }

    @Test
    fun `GIVEN clean storage WHEN init THEN shader stays enabled`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isFalse()
        assertThat(store.saved).isEqualTo(
            NorthernLightsCrashGuard.State(armedVersion = NO_VERSION, crashCount = 0, crashVersion = APP_VERSION),
        )
    }

    @Test
    fun `GIVEN clean storage WHEN init THEN no crash reported`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.hasDetectedCrash).isFalse()
        assertThat(NorthernLightsCrashGuard.crashCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN armed for current version WHEN init THEN crash reported once`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State(armedVersion = APP_VERSION))

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.hasDetectedCrash).isTrue()
        assertThat(NorthernLightsCrashGuard.crashCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN crash recorded but not armed WHEN init THEN disabled without new report`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State(crashCount = 1, crashVersion = APP_VERSION))

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isTrue()
        assertThat(NorthernLightsCrashGuard.hasDetectedCrash).isFalse()
    }

    @Test
    fun `GIVEN armed for current version WHEN init THEN shader disabled and crash counted`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State(armedVersion = APP_VERSION))

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isTrue()
        assertThat(store.saved).isEqualTo(
            NorthernLightsCrashGuard.State(armedVersion = NO_VERSION, crashCount = 1, crashVersion = APP_VERSION),
        )
    }

    @Test
    fun `GIVEN armed for previous version WHEN init THEN shader enabled again`() {
        // Arrange
        val store = FakeStore(
            NorthernLightsCrashGuard.State(
                armedVersion = APP_VERSION - 1,
                crashCount = 1,
                crashVersion = APP_VERSION - 1,
            ),
        )

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isFalse()
        assertThat(store.saved.crashCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN crash already recorded for current version WHEN init THEN shader stays disabled`() {
        // Arrange
        val store = FakeStore(
            NorthernLightsCrashGuard.State(crashCount = 1, crashVersion = APP_VERSION),
        )

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isTrue()
        assertThat(store.saved.crashCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN initialized guard WHEN arm THEN armed version persisted`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Act
        NorthernLightsCrashGuard.arm()

        // Assert
        assertThat(store.saved.armedVersion).isEqualTo(APP_VERSION)
    }

    @Test
    fun `GIVEN armed guard WHEN markSurvived THEN armed version cleared`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)
        NorthernLightsCrashGuard.arm()

        // Act
        NorthernLightsCrashGuard.markSurvived()

        // Assert
        assertThat(store.saved.armedVersion).isEqualTo(NO_VERSION)
    }

    @Test
    fun `GIVEN arm and survive cycle WHEN next init THEN no crash counted`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)
        NorthernLightsCrashGuard.arm()
        NorthernLightsCrashGuard.markSurvived()

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isFalse()
        assertThat(store.saved.crashCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN armed without survive WHEN next init THEN crash counted`() {
        // Arrange
        val store = FakeStore(NorthernLightsCrashGuard.State())
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)
        NorthernLightsCrashGuard.arm()

        // Act
        NorthernLightsCrashGuard.init(store = store, appVersion = APP_VERSION)

        // Assert
        assertThat(NorthernLightsCrashGuard.isDisabled).isTrue()
        assertThat(store.saved.crashCount).isEqualTo(1)
    }

    private companion object {
        const val APP_VERSION = 1825
        const val NO_VERSION = -1
    }
}