package com.tangem.wallet;

import androidx.annotation.StringRes;

import com.tangem.App;
import com.tangem.tangem_card.data.TangemCard;

public class BalanceValidator {
    private @StringRes int firstLine;
    private @StringRes int secondLine;
    private int score;
    private boolean hasPending;

    public @StringRes int getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(@StringRes int value) {
        firstLine = value;
    }

    public @StringRes int getSecondLine(Boolean recommend) {
//        if (!recommend)
            return secondLine;

//        if (score > 89) {
//            return "Safe to accept. " + secondLine;
//        } else if (score > 74) {
//            return "Not fully safe to accept. " + secondLine;
//        } else if (score > 30) {
//            return "Not safe to accept. " + secondLine;
//        } else {
//            return "Do not accept! " + secondLine;
//        }
    }

    public void setSecondLine(@StringRes int value) {
        secondLine = value;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getColor() {
        if( hasPending )
        {
            return R.color.primary_dark;
        }
        if (score > 89) {
            return R.color.confirmed;
        } else if (score > 74) {
            return android.R.color.holo_orange_light;
        } else if (score > 0) {
            return android.R.color.holo_orange_dark;
        } else {
            return android.R.color.holo_red_light;
        }
    }

    public void check(TangemContext ctx, Boolean attest) {
        firstLine = R.string.balance_validator_first_line_verification_failed;
        secondLine = R.string.empty_string;
        TangemCard card = ctx.getCard();
        CoinEngine engine = CoinEngineFactory.INSTANCE.create(ctx);

        if (!engine.validateBalance(this)) return;

        hasPending=App.pendingTransactionsStorage.hasTransactions(card);

        if( hasPending )
        {
            firstLine = R.string.balance_validator_first_line_pending_transaction;
            secondLine = R.string.balance_validator_second_line_swipe_to_refresh;
            return;
        }

        // Verify card?
        if (attest) {

            if (!card.isWalletPublicKeyValid()) {
                score = 0;
                firstLine = R.string.balance_validator_first_line_verification_failed;
                secondLine = R.string.balance_validator_second_line_verification_failed;
                return;
            }

            if (card.isOnlineVerified() != null && !card.isOnlineVerified()) {
                score = 0;
                firstLine = R.string.balance_validator_first_line_not_genuine;
                secondLine = R.string.balance_validator_second_line_failed_attestation;
                return;
            }

            if (card.isCodeConfirmed() != null && !card.isCodeConfirmed()) {
                score = 0;
                firstLine = R.string.balance_validator_first_line_not_genuine;
                secondLine = R.string.balance_validator_second_line_failed_binary_code_verification;
                return;
            }

            if (card.PIN2 == TangemCard.PIN2_Mode.CustomPIN2) {
                score = 0;
                firstLine = R.string.balance_validator_first_line_locked_pin2;
                secondLine = R.string.balance_validator_second_line_disable_pin_2;
                return;
            }

            // rule 2.b
            if (card.isOnlineVerified()) {
                secondLine += R.string.balance_validator_first_line_verify_identity;
            } else {
                score = 80;
                secondLine += R.string.balance_validator_second_line_identity_not_verified;
            }
        }
    }

}