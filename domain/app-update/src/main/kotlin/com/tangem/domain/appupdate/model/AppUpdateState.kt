package com.tangem.domain.appupdate.model

/**
 * Result of checking whether the application needs an update.
 */
enum class AppUpdateState {

    /** Update is mandatory — a blocking screen with an "Update now" button must be shown. */
    ForceUpdate,

    /**
     * Update is mandatory but impossible on this device (OS too old for the critical version) —
     * a permanently blocking "brick" screen must be shown.
     */
    Brick,

    /**
     * Update is mandatory but the device OS is too old for the min-supported version — a blocking
     * "update your OS" screen must be shown.
     */
    OsTooOld,

    /** Update is available but optional — a dismissible screen may be shown. */
    OptionalUpdate,

    /** No update is required. */
    NoUpdate,
}