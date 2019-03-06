package com.tangem.domain.wallet;

import com.tangem.App;
import com.tangem.card_common.data.TangemCard;
import com.tangem.wallet.R;

public class BalanceValidator {
    private String firstLine;
    private String secondLine;
    private int score;
    private boolean hasPending;

    public String getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(String value) {
        firstLine = value;
    }

    public String getSecondLine(Boolean recommend) {
        if (!recommend) return secondLine;
        if (score > 89) {
            return "Safe to accept. " + secondLine;
        } else if (score > 74) {
            return "Not fully safe to accept. " + secondLine;
        } else if (score > 30) {
            return "Not safe to accept. " + secondLine;
        } else {
            return "Do not accept! " + secondLine;
        }
    }

    public void setSecondLine(String value) {
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
        firstLine = "Verification failed";
        secondLine = "";
        TangemCard card = ctx.getCard();
        CoinEngine engine = CoinEngineFactory.INSTANCE.create(ctx);

        if (!engine.validateBalance(this)) return;

        hasPending=App.pendingTransactionsStorage.hasTransactions(card);

        if( hasPending )
        {
            firstLine = "Pending transaction...";
            secondLine = "Swipe down to refresh";
            return;
        }

        // Verify card?
        if (attest) {

            if (!card.isWalletPublicKeyValid()) {
                score = 0;
                firstLine = "Verification failed";
                secondLine = "Wallet verification failed. Tap again.";
                return;
            }

            if (card.isOnlineVerified() != null && !card.isOnlineVerified()) {
                score = 0;
                firstLine = "Not genuine banknote";
                secondLine = "Tangem Attestation service says the banknote is not genuine.";
                return;
            }

            if (card.isCodeConfirmed() != null && !card.isCodeConfirmed()) {
                score = 0;
                firstLine = "Not genuine banknote";
                secondLine = "Firmware binary code verification failed";
                return;
            }

            if (card.PIN2 == TangemCard.PIN2_Mode.CustomPIN2) {
                score = 0;
                firstLine = "Locked with PIN2";
                secondLine = "Ask the holder to disable PIN2 before accepting";
                return;
            }

            // rule 2.b
            if (card.isOnlineVerified()) {
                secondLine += "Verified note identity. ";
            } else {
                score = 80;
                secondLine += "Card identity was not verified. Cannot reach Tangem attestation service. ";
            }
        }
    }

}