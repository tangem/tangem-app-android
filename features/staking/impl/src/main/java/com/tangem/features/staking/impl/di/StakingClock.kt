package com.tangem.features.staking.impl.di

import javax.inject.Qualifier

/**
 * Qualifies the staking feature's [kotlinx.datetime.Clock] binding so it stays local in intent and
 * cannot collide with an unqualified app-wide `Clock` provided by another module.
 */
@Qualifier
annotation class StakingClock