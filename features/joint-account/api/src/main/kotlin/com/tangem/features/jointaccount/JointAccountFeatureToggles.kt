package com.tangem.features.jointaccount

/**
 * Feature toggles of the joint account feature.
 *
 * Note that the toggle gates **creation only**. A joint account received from the backend is displayed regardless of

 * invitation, or left over from a previous build), so an "account exists but the toggle is off" branch must not exist.
 */
interface JointAccountFeatureToggles {

    val isJointAccountCreationEnabled: Boolean
}