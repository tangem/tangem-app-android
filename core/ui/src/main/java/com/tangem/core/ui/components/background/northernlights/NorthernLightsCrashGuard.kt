package com.tangem.core.ui.components.background.northernlights

object NorthernLightsCrashGuard {

    private const val CRASH_THRESHOLD = 1
    private const val NO_VERSION = -1

    private var store: Store? = null
    private var appVersion: Int = NO_VERSION
    private var state: State = State()

    @Volatile
    private var isArmed: Boolean = false

    @Volatile
    var isDisabled: Boolean = false
        private set

    @Volatile
    var hasDetectedCrash: Boolean = false
        private set

    @Volatile
    var crashCount: Int = 0
        private set

    interface Store {
        fun load(): State
        fun save(state: State)
    }

    data class State(
        val armedVersion: Int = NO_VERSION,
        val crashCount: Int = 0,
        val crashVersion: Int = NO_VERSION,
    )

    fun init(store: Store, appVersion: Int) {
        val persisted = store.load()
        val hasDiedWhileLive = persisted.armedVersion == appVersion
        val countForVersion = if (persisted.crashVersion == appVersion) persisted.crashCount else 0
        val crashCount = if (hasDiedWhileLive) countForVersion + 1 else countForVersion

        val next = State(armedVersion = NO_VERSION, crashCount = crashCount, crashVersion = appVersion)
        store.save(next)

        this.store = store
        this.appVersion = appVersion
        this.state = next
        this.isArmed = false
        this.isDisabled = crashCount >= CRASH_THRESHOLD
        this.hasDetectedCrash = hasDiedWhileLive
        this.crashCount = crashCount
    }

    fun arm() {
        val store = store ?: return
        if (isArmed) return

        isArmed = true
        state = state.copy(armedVersion = appVersion)
        store.save(state)
    }

    fun markSurvived() {
        val store = store ?: return
        if (!isArmed) return

        isArmed = false
        state = state.copy(armedVersion = NO_VERSION)
        store.save(state)
    }
}