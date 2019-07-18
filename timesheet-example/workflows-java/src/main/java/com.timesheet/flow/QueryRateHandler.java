package com.timesheet.flow;

import com.timesheet.service.Rate;
import com.timesheet.service.RateOf;
import com.timesheet.service.SalaryRateOracle;
import kotlin.Unit;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.core.crypto.CompositeKey;
import org.intellij.lang.annotations.Flow;

import java.security.PublicKey;

@InitiatingFlow
class QueryRate extends FlowLogic<Rate> {
    private RateOf rateOf;
    private Party oracle;

    public QueryRate(RateOf rateOf, Party oracle) {
        this.rateOf = rateOf;
        this.oracle = oracle;
    }

    @Override
    public Rate call() throws FlowException {
        return initiateFlow(oracle).sendAndReceive(Rate.class, rateOf).unwrap((it ->{
            if(it.getOf() == rateOf){
                return it;
            }else{
                return null;
            }
        }));
    }
}

@InitiatingFlow
class SignRate extends FlowLogic<TransactionSignature>{
    private TransactionBuilder tx;
    private Party oracle;
    private FilteredTransaction partialMerkleTree;

    public SignRate(TransactionBuilder tx, Party oracle, FilteredTransaction partialMerkleTree) {
        this.tx = tx;
        this.oracle = oracle;
        this.partialMerkleTree = partialMerkleTree;
    }

    @Override
    public TransactionSignature call() throws FlowException {
        FlowSession oracleSession = initiateFlow(oracle);
        UntrustworthyData resp = oracleSession.sendAndReceive(TransactionSignature.class,partialMerkleTree);
        return (TransactionSignature) resp.unwrap( sig ->{
            CompositeKey k = (CompositeKey) oracleSession.getCounterparty().getOwningKey();
            if(k.isFulfilledBy((PublicKey) sig)){
                tx.toWireTransaction(getServiceHub()).checkSignature((TransactionSignature) sig);
                return sig;
            }
            return null;
        });
    }
}

@InitiatedBy(QueryRate.class)
public class QueryRateHandler extends FlowLogic<Unit>{
    private ProgressTracker.Step RECEIVING = new ProgressTracker.Step("Receiving sign request");
    private ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing filtered transaction.");
    private ProgressTracker.Step SENDING = new ProgressTracker.Step("Sending sign response.");
    private FlowSession session;

    public QueryRateHandler(FlowSession session) {
        this.session = session;
    }

    private final ProgressTracker progressTracker = new ProgressTracker(
            RECEIVING,
            SIGNING,
            SENDING
    );


    @Override
    public Unit call() throws FlowException {
        progressTracker.setCurrentStep(RECEIVING);
        FilteredTransaction request = session.receive(FilteredTransaction.class).unwrap(it -> it);
        progressTracker.setCurrentStep(SIGNING);
       try{
           TransactionSignature response = getServiceHub().cordaService(SalaryRateOracle.class).sign(request);
           progressTracker.setCurrentStep(SENDING);
           session.send(response);
       } catch(Exception e ){
           throw new FlowException(e);
       };
       return null;

    }
}