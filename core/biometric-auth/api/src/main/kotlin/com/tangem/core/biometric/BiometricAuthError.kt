package com.tangem.core.biometric

/**
 * Reason the authentication prompt did not end with a successful authentication.
 *
 * The manager only reports what happened — whether a particular reason should block the guarded
 * action is up to the caller. For example a screen may choose to proceed on [NoDeviceCredential],
 * since a device without a lock screen cannot be gated at all.
 */
enum class BiometricAuthError {

    /**
     * The system never showed or force-dismissed the prompt: the sensor was busy, the user was
     * switched, the device got locked. Distinct from [BiometricAuthManager.Result.Cancelled] —
     * the user made no choice here.
     */
    SystemCancelled,

    /** No lock screen is set up on the device, so there is no credential to authenticate against. */
    NoDeviceCredential,

    /** The device supports biometrics but the user has not enrolled any. */
    NoBiometricEnrolled,

    /** The device has no biometric sensor, or it is temporarily unavailable. */
    HardwareUnavailable,

    /** Too many failed attempts — biometrics are disabled for a short period. */
    LockedOut,

    /** Too many failed attempts — biometrics stay disabled until the user unlocks with a credential. */
    LockedOutPermanently,

    /** The user did not respond in time. */
    Timeout,

    /** Biometrics cannot be trusted until the required security update is installed. */
    SecurityUpdateRequired,

    /** There was no foreground activity to attach the prompt to, so it was never shown. */
    NoForegroundActivity,

    /** A platform error without a dedicated mapping. */
    Unknown,
}