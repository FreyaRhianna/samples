package com.whistleblower.flow;


import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.whistleblower.contract.BlowWhistleContract;
import com.whistleblower.state.BlowWhistleState;
import kotlin.Pair;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.LinkedHashMap;

/**
 * Blows the whistle on a company.
 *
 * Confidential identities are used to preserve the identity of the whistle-blower and the investigator.
 *
 * @param badCompany the company the whistle is being blown on.
 * @param investigator the party handling the investigation.
 */
@InitiatingFlow
@StartableByRPC
public class BlowWhistleFlow extends FlowLogic<SignedTransaction> {
    private Party badCompany;
    private Party investigator;

    private final ProgressTracker.Step GENERATE_CONFIDENTIAL_IDS = new ProgressTracker.Step("Generating confidential identities for the transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return SwapIdentitiesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step BUILD_TRANSACTION = new ProgressTracker.Step("Building the transaction.");
    private final ProgressTracker.Step VERIFY_TRANSACTION = new ProgressTracker.Step("Verifying the transaction.");
    private final ProgressTracker.Step SIGN_TRANSACTION = new ProgressTracker.Step("I sign the transaction.");
    private final ProgressTracker.Step COLLECT_COUNTERPARTY_SIG = new ProgressTracker.Step("The counterparty signs the transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISE_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATE_CONFIDENTIAL_IDS,
            BUILD_TRANSACTION,
            VERIFY_TRANSACTION,
            SIGN_TRANSACTION,
            COLLECT_COUNTERPARTY_SIG,
            FINALISE_TRANSACTION
    );


    public BlowWhistleFlow(Party badCompany, Party investigator){
        this.badCompany = badCompany;
        this.investigator = investigator;
    }
    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(GENERATE_CONFIDENTIAL_IDS);
        FlowSession investigatorSession = initiateFlow(investigator);
        Pair<AnonymousParty, AnonymousParty> anonIds = generateConfidentialIdentities(investigatorSession);
        AnonymousParty anonMe = anonIds.getFirst();
        AnonymousParty anonInvestigator = anonIds.getSecond();

        progressTracker.setCurrentStep(BUILD_TRANSACTION);
        BlowWhistleState output = new BlowWhistleState(badCompany, anonMe,anonInvestigator);
        Command command = new Command(new BlowWhistleContract.Commands.BlowWhistleCmd(), ImmutableList.of(anonMe.getOwningKey(), anonInvestigator.getOwningKey()));
        TransactionBuilder txBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0));
        txBuilder.addOutputState(output, BlowWhistleContract.ID);
        txBuilder.addCommand(command);

        progressTracker.setCurrentStep(VERIFY_TRANSACTION);
        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGN_TRANSACTION);
        SignedTransaction stx = getServiceHub().signInitialTransaction(txBuilder,anonMe.getOwningKey());

        progressTracker.setCurrentStep(COLLECT_COUNTERPARTY_SIG);
        final ImmutableSet<FlowSession> sessions = ImmutableSet.of(investigatorSession);
        final SignedTransaction ftx = subFlow(new CollectSignaturesFlow(stx,sessions, ImmutableList.of(anonMe.getOwningKey()), COLLECT_COUNTERPARTY_SIG.childProgressTracker()));

        progressTracker.setCurrentStep(FINALISE_TRANSACTION);
        return subFlow(new FinalityFlow(ftx, ImmutableList.of(investigatorSession), FINALISE_TRANSACTION.childProgressTracker()));
    }

    @Suspendable
    private Pair<AnonymousParty, AnonymousParty> generateConfidentialIdentities(FlowSession counterpartySession) throws FlowException{
         LinkedHashMap<Party,AnonymousParty> confId = subFlow(new SwapIdentitiesFlow(counterpartySession, GENERATE_CONFIDENTIAL_IDS.childProgressTracker()));
         AnonymousParty anonMe = confId.get(getOurIdentity());
         AnonymousParty anonInvestigator = confId.get(counterpartySession.getCounterparty());
         return new Pair<>(anonMe,anonInvestigator);
    }

}

