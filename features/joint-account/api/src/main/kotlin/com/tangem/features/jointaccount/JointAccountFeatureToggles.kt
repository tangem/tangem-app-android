package com.tangem.features.jointaccount

/**
 * Feature toggles of the joint account feature.
 *
 * The toggle gates creation and whether the app knows about joint accounts at all: with it off, joint records are

 */
interface JointAccountFeatureToggles {

    val isJointAccountCreationEnabled: Boolean
}