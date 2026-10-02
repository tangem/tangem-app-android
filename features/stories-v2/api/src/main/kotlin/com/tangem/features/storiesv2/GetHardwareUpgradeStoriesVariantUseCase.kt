package com.tangem.features.storiesv2

/**
 * Decides, on the upgrade tap, whether the hardware upgrade story comes before the Hardware wallet screen.
 *
 * A closed release gate answers [HardwareUpgradeStoriesVariant.CONTROL] without reading the experiment, so no
 * exposure is tracked for it. Otherwise it suspends only while the first experiment fetch after app start is still
 * in flight.
 */
interface GetHardwareUpgradeStoriesVariantUseCase {

    suspend operator fun invoke(): HardwareUpgradeStoriesVariant
}

enum class HardwareUpgradeStoriesVariant {

    /** Straight to the Hardware wallet screen. */
    CONTROL,

    /** The story first, then the Hardware wallet screen. */
    TREATMENT,
}