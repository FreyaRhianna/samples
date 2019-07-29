package com.whistleblower.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;

@InitiatedBy(BlowWhistleFlow.class)
public class BlowWhistleFlowResponder extends FlowLogic<ObjectUtils.Null> {
    private FlowSession counterpartySession;

    public BlowWhistleFlowResponder(FlowSession counterpartySession){
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public ObjectUtils.Null call() throws FlowException {



        subFlow(new SwapIdentitiesFlow(counterpartySession));
        SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession){

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                // TODO: Checking.
            }
        };

        SecureHash txId = subFlow(signTransactionFlow).getId();
        subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
        return null;
    }
}



/*
@InitiatedBy(BlowWhistleFlow::class)
class BlowWhistleFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
@Suspendable
    override fun call() {
            subFlow(SwapIdentitiesFlow(counterpartySession))

            val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
            // TODO: Checking.
            }
            }

            val txId = subFlow(signTransactionFlow).id

            subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
            }
            }
*/