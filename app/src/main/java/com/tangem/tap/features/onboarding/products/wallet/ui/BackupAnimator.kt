package com.tangem.tap.features.onboarding.products.wallet.ui

import com.google.android.material.button.MaterialButton
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.postUiDelayBg
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupState
import com.tangem.tap.features.onboarding.products.wallet.ui.BackupCardType.FIRST_BACKUP
import com.tangem.tap.features.onboarding.products.wallet.ui.BackupCardType.ORIGIN
import com.tangem.tap.features.onboarding.products.wallet.ui.BackupCardType.SECOND_BACKUP
import com.tangem.wallet.databinding.FragmentOnboardingWalletBinding

/**
 * Created by Anton Zhilenkov on 19.10.2022.
 */
interface BackupAnimator {
    fun updateBackupState(backupState: BackupState)
    fun setupCreateWalletState()
    fun showBackupIntro(state: BackupState)
    fun showScanOriginCard()
    fun showAddBackupCards(state: BackupState, backupCardsCounts: Int)
    fun showWritePrimaryCard(state: BackupState)
    fun showWriteBackupCard(state: BackupState, backupCard: Int)
    fun showSuccess(onEnd: () -> Unit)
}

class WalletBackupAnimator(
    private val cardsWidget: WalletCardsWidget,
) : BackupAnimator {

    private val viewState: State = when (cardsWidget.leapfrogWidget.getViewsCount()) {
        2 -> State.TwoCards
        3 -> State.TreeCards
        else -> throw UnsupportedOperationException()
    }

    private var currentStep = -1

    override fun updateBackupState(backupState: BackupState) {
    }

    override fun showBackupIntro(state: BackupState) {
        currentStep = STEP_BACKUP_INTRO

        ORIGIN.show()
        FIRST_BACKUP.show()
        SECOND_BACKUP.show()

        cardsWidget.toWelcome()
    }

    override fun setupCreateWalletState() {
        currentStep = STEP_CREATE_WALLET

        cardsWidget.toFolded(false)

        ORIGIN.alpha(1f)
        FIRST_BACKUP.alpha(1f)
        SECOND_BACKUP.alpha(1f)
    }

    override fun showScanOriginCard() {
        currentStep = STEP_SCAN_ORIGIN_CARD

        cardsWidget.toFolded()
    }

    override fun showAddBackupCards(state: BackupState, backupCardsCounts: Int) {
        when (backupCardsCounts) {
            0 -> {
                when (currentStep) {
                    UNDEFINED, STEP_CREATE_WALLET -> {
                        cardsWidget.toFolded(false) { cardsWidget.toFan() }

                        FIRST_BACKUP.animateAlpha(0.6f, 0)
                        SECOND_BACKUP.animateAlpha(0.2f, 400)
                    }
                    STEP_BACKUP_INTRO -> {
                        cardsWidget.toFolded(duration = 300) { cardsWidget.toFan() }
                        FIRST_BACKUP.animateAlpha(0.6f, 400, 250)
                        SECOND_BACKUP.animateAlpha(0.2f, 400, 250)
                    }
                    STEP_SCAN_ORIGIN_CARD -> {
                        cardsWidget.toFan()
                        FIRST_BACKUP.animateAlpha(0.6f, 400, 250)
                        SECOND_BACKUP.animateAlpha(0.2f, 400, 250)
                    }
                }
            }
            1 -> {
                cardsWidget.toFan(false)
                FIRST_BACKUP.alpha(1f)
                SECOND_BACKUP.alpha(0.2f)
            }
            2 -> {
                cardsWidget.toFan(false)
                FIRST_BACKUP.alpha(1f)
                SECOND_BACKUP.alpha(1f)
            }
        }

        currentStep = STEP_ADD_BACKUP_CARDS
    }

    override fun showWritePrimaryCard(state: BackupState) {
        currentStep = STEP_WRITE_PRIMARY_CARD

        cardsWidget.toLeapfrog(
            onEndFold = {
                FIRST_BACKUP.alpha(0.4f)
                SECOND_BACKUP.alpha(0.2f)
            },
        )
    }

    private var firstBackupCardAnimated = false
    private var secondBackupCardAnimated = false

    override fun showWriteBackupCard(state: BackupState, backupCard: Int) {
        when (viewState) {
            State.TwoCards -> {
                if (firstBackupCardAnimated) return
                firstBackupCardAnimated = true

                val delay = if (currentStep == UNDEFINED) 700L else 0
                postUiDelayBg(delay) {
                    FIRST_BACKUP.alpha(1f)
                    cardsWidget.leapfrogWidget.leap {
                        ORIGIN.alpha(0.4f)
                    }
                }
            }
            State.TreeCards -> {
                when (backupCard) {
                    1 -> {
                        if (firstBackupCardAnimated) return
                        firstBackupCardAnimated = true
                        FIRST_BACKUP.alpha(1f)
                        cardsWidget.leapfrogWidget.leap {
                            SECOND_BACKUP.alpha(0.4f)
                            ORIGIN.alpha(0.2f)
                        }
                    }
                    2 -> {
                        if (secondBackupCardAnimated) return
                        secondBackupCardAnimated = true
                        SECOND_BACKUP.alpha(1f)
                        cardsWidget.leapfrogWidget.leap {
                            ORIGIN.alpha(0.4f)
                            FIRST_BACKUP.alpha(0.2f)
                        }
                    }
                }
            }
        }
        currentStep = STEP_WRITE_BACKUP_CARD
    }

    override fun showSuccess(onEnd: () -> Unit) {
        currentStep = STEP_SUCCESS
        cardsWidget.leapfrogWidget.fold(onEnd = onEnd)
    }

    private enum class State {
        TreeCards,
        TwoCards
    }

    private fun BackupCardType.animateAlpha(
        alpha: Float,
        duration: Long = 0L,
        startDelay: Long = 0L,
    ) {
        when (this) {
            ORIGIN -> {
                cardsWidget.getOriginCardView().animate().apply {
                    alpha(alpha)
                    this.startDelay = startDelay
                    this.duration = duration
                }
            }
            FIRST_BACKUP -> {
                cardsWidget.getFirstBackupCardView().animate().apply {
                    alpha(alpha)
                    this.startDelay = startDelay
                    this.duration = duration
                }
            }
            SECOND_BACKUP -> {
                if (viewState != State.TreeCards) return
                cardsWidget.getSecondBackupCardView().animate().apply {
                    alpha(alpha)
                    this.startDelay = startDelay
                    this.duration = duration
                }
            }
        }
    }

    private fun BackupCardType.alpha(alpha: Float) {
        when (this) {
            ORIGIN -> cardsWidget.getOriginCardView().alpha = alpha
            FIRST_BACKUP -> cardsWidget.getFirstBackupCardView().alpha = alpha
            SECOND_BACKUP -> {
                if (viewState != State.TreeCards) return
                cardsWidget.getSecondBackupCardView().alpha = alpha
            }
        }
    }

    private fun BackupCardType.show() {
        when (this) {
            ORIGIN -> cardsWidget.getOriginCardView().show()
            FIRST_BACKUP -> cardsWidget.getFirstBackupCardView().show()
            SECOND_BACKUP -> {
                if (viewState != State.TreeCards) return
                cardsWidget.getSecondBackupCardView().show()
            }
        }
    }

    companion object {
        private const val UNDEFINED = -1
        private const val STEP_CREATE_WALLET = 0
        private const val STEP_BACKUP_INTRO = 1
        private const val STEP_SCAN_ORIGIN_CARD = 2
        private const val STEP_ADD_BACKUP_CARDS = 3
        private const val STEP_WRITE_PRIMARY_CARD = 4
        private const val STEP_WRITE_BACKUP_CARD = 5
        private const val STEP_SUCCESS = 6
    }
}

class TestBackupAnimation(
    private val animator: BackupAnimator,
    binding: FragmentOnboardingWalletBinding,
) : BackupAnimator {

    private lateinit var state: BackupState
    private var steps = 0

    init {
        init(binding, this)
    }

    fun setStep(step: Int, onStepUpdate: (Int) -> Unit = {}) {
        steps = step
        when (steps) {
            0 -> setupCreateWalletState()
            1 -> showBackupIntro(state)
            2 -> showScanOriginCard()
            3 -> showAddBackupCards(state, 0)
            4 -> showAddBackupCards(state, 1)
            5 -> showAddBackupCards(state, 2)
            6 -> showWritePrimaryCard(state)
            7 -> showWriteBackupCard(state, 1)
            8 -> showWriteBackupCard(state, 2)
            else -> setStep(0)
        }
        onStepUpdate(steps)
    }

    fun onNext(onStepUpdate: (Int) -> Unit) {
        steps++
        setStep(steps, onStepUpdate)
    }

    override fun updateBackupState(backupState: BackupState) {
        this.state = backupState
    }

    override fun setupCreateWalletState() {
        animator.setupCreateWalletState()
    }

    override fun showBackupIntro(state: BackupState) {
        animator.showBackupIntro(state)
    }

    override fun showScanOriginCard() {
        animator.showScanOriginCard()
    }

    override fun showAddBackupCards(state: BackupState, backupCardsCounts: Int) {
        animator.showAddBackupCards(state, backupCardsCounts)
    }

    override fun showWritePrimaryCard(state: BackupState) {
        animator.showWritePrimaryCard(state)
    }

    override fun showWriteBackupCard(state: BackupState, backupCard: Int) {
        animator.showWriteBackupCard(state, backupCard)
    }

    override fun showSuccess(onEnd: () -> Unit) {
        animator.showSuccess(onEnd)
    }

    companion object {
        fun init(binding: FragmentOnboardingWalletBinding, animator: TestBackupAnimation) {
            val button = MaterialButton(binding.onboardingRoot.context).apply { text = "Next" }
            button.setOnClickListener { animator.onNext { button.text = "$it" } }
            binding.onboardingRoot.addView(button, 0)
        }
    }
}
