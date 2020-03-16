package com.tangem.wallet.ltc;

import android.net.Uri;
import android.text.InputFilter;
import android.util.Log;

import com.tangem.App;
import com.tangem.data.local.PendingTransactionsStorage;
import com.tangem.data.network.Server;
import com.tangem.data.network.ServerApiBlockcypher;
import com.tangem.data.network.model.BlockcypherFee;
import com.tangem.data.network.model.BlockcypherResponse;
import com.tangem.data.network.model.BlockcypherTx;
import com.tangem.data.network.model.BlockcypherTxref;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.tangem_card.util.Util;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.util.DerEncodingUtil;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.Base58;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.Transaction;
import com.tangem.wallet.UnspentOutputInfo;
import com.tangem.wallet.btc.BtcData;
import com.tangem.wallet.btc.BtcEngine;
import com.tangem.wallet.btc.Unspents;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;

public class LtcEngine extends BtcEngine {
    private static final String TAG = LtcEngine.class.getSimpleName();
    public BtcData coinData = null;

    public LtcEngine(TangemContext context) throws Exception {
        super(context);
        if (context.getCoinData() == null) {
            coinData = new BtcData();
            context.setCoinData(coinData);
        } else if (context.getCoinData() instanceof BtcData) {
            coinData = (BtcData) context.getCoinData();
        } else {
            throw new Exception("Invalid type of Blockchain data for LtcEngine");
        }
    }

    public LtcEngine() {
        super();
    }

    private static int getDecimals() {
        return 8;
    }


    private void checkBlockchainDataExists() throws Exception {
        if (coinData == null) throw new Exception("No blockchain data");
    }

    @Override
    public boolean awaitingConfirmation() {
        if (coinData == null) return false;
        return coinData.getBalanceUnconfirmed() != 0 || App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public String getBalanceHTML() {
        Amount balance = getBalance();
        if (balance != null) {
            return balance.toDescriptionString(getDecimals());
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        return "LTC";
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null) return false;
        return coinData.getBalanceInInternalUnits().notZero();
    }

    @Override
    public boolean hasBalanceInfo() {
        if (coinData == null) return false;
        return coinData.hasBalanceInfo();
    }


    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else if (coinData.getUnspentTransactions().size() == 0) {
            ctx.setMessage(R.string.loaded_wallet_message_refresh);
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String getFeeCurrency() {
        return "LTC";
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (address.length() < 25) {
            return false;
        }

        if (address.length() > 35) {
            return false;
        }

        if (!address.startsWith("L") && !address.startsWith("M")) {
            return false;
        }

        byte[] decAddress = Base58.decodeBase58(address);

        if (decAddress == null || decAddress.length == 0) {
            return false;
        }

        byte[] rip = new byte[21];
        for (int i = 0; i < 21; ++i) {
            rip[i] = decAddress[i];
        }

        byte[] kcv = CryptoUtil.doubleSha256(rip);

        for (int i = 0; i < 4; ++i) {
            if (kcv[i] != decAddress[21 + i])
                return false;
        }

        return true;
    }


    @Override
    public boolean isNeedCheckNode() {
        return true;
    }

    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://live.blockcypher.com/ltc/address/" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse(ctx.getCoinData().getWallet() + "?amount=" + convertToAmount(convertToInternalAmount(ctx.getCard().getDenomination())).toValueString(8));
        } else {
            return Uri.parse(ctx.getCoinData().getWallet());
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        return new InputFilter[]{new DecimalDigitsInputFilter(getDecimals())};
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (coinData == null) return false;
        if (amount.compareTo(convertToAmount(coinData.getBalanceInInternalUnits())) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amountValue, Amount feeValue, Boolean isIncludeFee) {
        InternalAmount fee;
        InternalAmount amount;

        try {
            checkBlockchainDataExists();
            amount = convertToInternalAmount(amountValue);
            fee = convertToInternalAmount(feeValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (fee == null || amount == null)
            return false;

        if (fee.isZero() || amount.isZero())
            return false;

        if (isIncludeFee && (amount.compareTo(coinData.getBalanceInInternalUnits()) > 0 || amount.compareTo(fee) < 0))
            return false;

        if (!isIncludeFee && amount.add(fee).compareTo(coinData.getBalanceInInternalUnits()) > 0)
            return false;

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (((ctx.getCard().getOfflineBalance() == null) && !ctx.getCoinData().isBalanceReceived()) || (!ctx.getCoinData().isBalanceReceived() && (ctx.getCard().getRemainingSignatures() != ctx.getCard().getMaxSignatures()))) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        }

        // Workaround before new back-end
//        if (card.getRemainingSignatures() == card.getMaxSignatures()) {
//            firstLine = "Verified balance";
//            secondLine = "Balance confirmed in blockchain. ";
//            secondLine += "Verified note identity. ";
//            return;
//        }

        if (coinData.getBalanceUnconfirmed() != 0) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_transaction_in_progress);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_wait_for_confirmation);
            return false;
        }

        if (coinData.isBalanceReceived() && coinData.isBalanceEqual()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
            if (coinData.getBalanceInInternalUnits().isZero()) {
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                balanceValidator.setSecondLine(R.string.empty_string);
            }
        }

        // rule 4 TODO: need to check SignedHashed against number of outputs in blockchain
//        if((card.getRemainingSignatures() != card.getMaxSignatures()) && card.getBalance() != 0)
//        {
//            score = 80;
//            firstLine = "Unguaranteed balance";
//            secondLine = "Potential unsent transaction. Redeem immediately if accept. ";
//            return;
//        }

//        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) {
//            balanceValidator.setScore(80);
//            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
//            balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_verify_online);
//        }

//            if(card.getFailedBalanceRequestCounter()!=0) {
//                score -= 5 * card.getFailedBalanceRequestCounter();
//                secondLine += "Not all nodes have returned balance. Swipe down or tap again. ";
//                if(score <= 0)
//                    return;
//            }

        //
//            if(card.isBalanceReceived() && !card.isBalanceEqual()) {
//                score = 0;
//                firstLine = "Disputed balance";
//                secondLine += " Cannot obtain trusted balance at the moment. Try to tap and check this banknote later.";
//                return;
//            }

        return true;
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) return null;
        return convertToAmount(coinData.getBalanceInInternalUnits());
    }

    @Override
    public String evaluateFeeEquivalent(String fee) {
        if (!coinData.getAmountEquivalentDescriptionAvailable()) return "";
        try {
            Amount feeAmount = new Amount(fee, getFeeCurrency());
            return feeAmount.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public String getBalanceEquivalent() {
        if (coinData == null || !coinData.getAmountEquivalentDescriptionAvailable()) return "";
        Amount balance = getBalance();
        if (balance == null) return "";
        return balance.toEquivalentString(coinData.getRate());
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException {
        byte netSelectionByte = (byte) 0x30;

        byte hash1[] = Util.calculateSHA256(pkUncompressed);
        byte hash2[] = Util.calculateRIPEMD160(hash1);

        ByteBuffer BB = ByteBuffer.allocate(hash2.length + 1);

        BB.put(netSelectionByte);
        BB.put(hash2);

        byte hash3[] = Util.calculateSHA256(BB.array());
        byte hash4[] = Util.calculateSHA256(hash3);

        BB = ByteBuffer.allocate(hash2.length + 5);
        BB.put(netSelectionByte); //BB.put((byte) 0x6f);
        BB.put(hash2);
        BB.put(hash4[0]);
        BB.put(hash4[1]);
        BB.put(hash4[2]);
        BB.put(hash4[3]);

        return org.bitcoinj.core.Base58.encode(BB.array());
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) {
        BigDecimal d = internalAmount.divide(new BigDecimal("100000000"));
        return new Amount(d, getBalanceCurrency());
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) {
        BigDecimal d = amount.multiply(new BigDecimal("100000000"));
        return new InternalAmount(d, "Satoshi");
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) {
        if (bytes == null) return null;
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return new InternalAmount(Util.byteArrayToLong(reversed), "Satoshi");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount internalAmount) {
        byte[] bytes = Util.longToByteArray(internalAmount.longValueExact());
        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) reversed[i] = bytes[bytes.length - i - 1];
        return reversed;
    }

    @Override
    public CoinData createCoinData() {
        return new BtcData();
    }

    @Override
    public String getUnspentInputsDescription() {
        Unspents unspents = coinData.getUnspentInputsDescription();
        if (unspents == null) {
            return "";
        } else {
            return String.format(
                    ctx.getContext().getString(R.string.details_unspents_number),
                    unspents.getUnspetns(),
                    unspents.getGatheredUnspents());
        }
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        ArrayList<UnspentOutputInfo> unspentOutputs = new ArrayList<>();
        checkBlockchainDataExists();

        String myAddress = ctx.getCoinData().getWallet();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        for (BtcData.UnspentTransaction utxo : coinData.getUnspentTransactions()) {
            unspentOutputs.add(new UnspentOutputInfo(BTCUtils.fromHex(utxo.txID), new Transaction.Script(BTCUtils.fromHex(utxo.script)), utxo.amount, utxo.outputN, -1, utxo.txID, null));
        }

        long fullAmount = 0;
        for (int i = 0; i < unspentOutputs.size(); ++i) {
            fullAmount += unspentOutputs.get(i).value;
        }

        long fees = convertToInternalAmount(feeValue).longValueExact();
        long amount = convertToInternalAmount(amountValue).longValueExact();
        long change = fullAmount - amount;
        if (IncFee) {
            amount = amount - fees;
        } else {
            change = change - fees;
        }

        final long amountFinal = amount;
        final long changeFinal = change;

        if (amount + fees > fullAmount) {
            throw new CardProtocol.TangemException_WrongAmount(String.format("Balance (%d) < change (%d) + amount (%d)", fullAmount, change, amount));
        }

        final byte[][] txForSign = new byte[unspentOutputs.size()][];
        final byte[][] bodyDoubleHash = new byte[unspentOutputs.size()][];
        final byte[][] bodyHash = new byte[unspentOutputs.size()][];

        for (int i = 0; i < unspentOutputs.size(); ++i) {
            txForSign[i] = BTCUtils.buildTXForSign(myAddress, targetAddress, myAddress, unspentOutputs, i, amount, change);
            bodyHash[i] = Util.calculateSHA256(txForSign[i]);
            bodyDoubleHash[i] = Util.calculateSHA256(bodyHash[i]);
        }

        return new SignTask.TransactionToSign() {

            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw;
            }

            @Override
            public byte[][] getHashesToSign() throws Exception {
                byte[][] dataForSign = new byte[unspentOutputs.size()][];
                if (txForSign.length > 10)
                    throw new Exception("To much hashes in one transaction!");
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    dataForSign[i] = bodyDoubleHash[i];
                }
                return dataForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                for (int i = 0; i < txForSign.length; i++) {
                    if (i != 0 && txForSign[0].length != txForSign[i].length)
                        throw new Exception("Hashes length must be identical!");
                    bs.write(txForSign[i]);
                }

                return bs.toByteArray();
            }

            @Override
            public String getHashAlgToSign() {
                return "sha-256x2";
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Issuer validation not supported!");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                for (int i = 0; i < unspentOutputs.size(); ++i) {
                    BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, i * 64, 32 + i * 64));
                    BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64));
                    s = CryptoUtil.toCanonicalised(s);

                    unspentOutputs.get(i).scriptForBuild = DerEncodingUtil.packSignDer(r, s, pbKey);
                }

                byte[] txForSend = BTCUtils.buildTXForSend(targetAddress, myAddress, unspentOutputs, amountFinal, changeFinal);
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) throws Exception {
        ctx.setError(null);

        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();

        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
            @Override
            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
                Log.i(TAG, "onSuccess: " + method);
                try {
                    String walletAddress = blockcypherResponse.getAddress();
                    if (!walletAddress.equals(coinData.getWallet())) {
                        // todo - check
                        throw new Exception("Invalid wallet address in answer!");
                    }
                    Long confBalance = blockcypherResponse.getBalance();
                    Long unconfirmedBalance = blockcypherResponse.getUnconfirmed_balance();
                    coinData.setBalanceReceived(true);
                    coinData.setBalanceConfirmed(confBalance);
                    coinData.setBalanceUnconfirmed(unconfirmedBalance);
                    coinData.setValidationNodeDescription(Server.ApiBlockcypher.URL_BLOCKCYPHER);

                    coinData.getUnspentTransactions().clear();
                    //TODO: change request logic, 2000 tx max
                    if (blockcypherResponse.getTxrefs() != null) {
                        for (BlockcypherTxref txref : blockcypherResponse.getTxrefs()) {
                            if (txref.getTx_input_n() == -1) { //recieved only
                                if (!txref.getSpent()) {
                                    BtcData.UnspentTransaction trUnspent = new BtcData.UnspentTransaction();
                                    trUnspent.txID = txref.getTx_hash();
                                    trUnspent.amount = txref.getValue();
                                    trUnspent.outputN = txref.getTx_output_n();
                                    trUnspent.script = txref.getScript();
                                    coinData.getUnspentTransactions().add(trUnspent);
                                }
                            } else { //sent only
                                coinData.incSentTransactionsCount();
                            }
                        }
                    }

                    if (blockcypherResponse.getUnconfirmed_txrefs() != null) {
                        for (BlockcypherTxref unconfirmedTxref : blockcypherResponse.getUnconfirmed_txrefs()) {
                            if (unconfirmedTxref.getTx_input_n() != -1) {
                                coinData.incSentTransactionsCount();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "FAIL BLOCKCYPHER_ADDRESS Exception");
                    e.printStackTrace();
                    ctx.setError(e.getMessage());
                    blockchainRequestsCallbacks.onComplete(false);
                }

                checkPending(blockchainRequestsCallbacks);
            }

            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
                Log.e(TAG, "Wrong response type for requestBalanceAndUnspentTransactions");
                ctx.setError("Wrong response type for requestBalanceAndUnspentTransactions");
                blockchainRequestsCallbacks.onComplete(false);
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };

        serverApiBlockcypher.setResponseListener(blockcypherListener);

        serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_ADDRESS, ctx.getCoinData().getWallet(), "");
    }

    private void checkPending(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        if (App.pendingTransactionsStorage.hasTransactions(ctx.getCard())) {
            ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();

            ServerApiBlockcypher.TxResponseListener listener = new ServerApiBlockcypher.TxResponseListener() {
                @Override
                public void onSuccess(BlockcypherTx response) {
                    Log.i(TAG, "onSuccess: BlockcypherTx");
                    try {
                        if (response.getHex() != null) {
                            App.pendingTransactionsStorage.removeTransaction(ctx.getCard(), response.getHex());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onFail: BlockcypherTx" + response);
                    }
                    if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }

                @Override
                public void onFail(String message) {
                    Log.i(TAG, "onFail: BlockcypherTx " + message);
                    if (serverApiBlockcypher.isRequestsSequenceCompleted()) {
                        blockchainRequestsCallbacks.onComplete(!ctx.hasError());//serverApiElectrum.isErrorOccurred(), serverApiElectrum.getError());
                    } else {
                        blockchainRequestsCallbacks.onProgress();
                    }
                }
            };
            serverApiBlockcypher.setTxResponseListener(listener);

            for (PendingTransactionsStorage.TransactionInfo pendingTx : App.pendingTransactionsStorage.getTransactions(ctx.getCard()).getTransactions()) {
                String txId = BTCUtils.toHex(BTCUtils.reverse(CryptoUtil.doubleSha256(BTCUtils.fromHex(pendingTx.getTx()))));
                try {
                    serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_TXS, "", txId);
                } catch (Exception e) {
                    e.printStackTrace();
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                }
            }
        } else {
            blockchainRequestsCallbacks.onComplete(!ctx.hasError());
        }
    }

    private final static BigDecimal relayFee = new BigDecimal(0.00001).setScale(8, RoundingMode.DOWN);

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) throws Exception {
        coinData.minFee = coinData.normalFee = coinData.maxFee = new Amount(relayFee, "LTC");
        blockchainRequestsCallbacks.onComplete(true);
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) throws Exception {
        final String txStr = BTCUtils.toHex(txForSend);
        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();

        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
            @Override
            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
                String resultString = blockcypherResponse.toString();
                try {
                    if (resultString.isEmpty()) {
                        ctx.setError("No response from node");
                        blockchainRequestsCallbacks.onComplete(false);
                    } else { // TODO: Make check for a valid send response
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        ctx.setError(e.getMessage());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        ctx.setError(e.getClass().getName());
                        blockchainRequestsCallbacks.onComplete(false);
                        Log.e(TAG, resultString);
                    }
                }
            }

            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
                Log.e(TAG, "Wrong response type for requestSendTransaction");
                ctx.setError("Wrong response type for requestSendTransaction");
                blockchainRequestsCallbacks.onComplete(false);
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiBlockcypher.setResponseListener(blockcypherListener);

        serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_SEND, "", txStr);
    }

    @Override
    public boolean allowSelectFeeLevel() {
        return false;
    }
}