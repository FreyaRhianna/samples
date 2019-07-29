package com.whistleblower.state;

import com.google.common.collect.ImmutableList;
import com.whistleblower.contract.BlowWhistleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;
import java.util.List;


/**
 * A state representing a whistle-blowing case.
 *
 * The identity of both the whistle-blower and the investigator is kept confidential through the
 * use of [AnonymousParty].
 *
 * @property badCompany the company the whistle is being blown on.
 * @property whistleBlower the [AnonymousParty] blowing the whistle.
 * @property investigator the [AnonymousParty] handling the investigation.
 */
@BelongsToContract(BlowWhistleContract.class)
public class BlowWhistleState implements LinearState {
    private Party badCompany;
    private AnonymousParty whistleBlower;
    private AnonymousParty investigator;
    private UniqueIdentifier linearId;

    public AnonymousParty getWhistleBlower(){ return this.whistleBlower;}
    public AnonymousParty getInvestigator(){ return this.investigator;}
    public Party getBadCompany(){ return this.badCompany;}

    public BlowWhistleState(Party badCompany, AnonymousParty whistleBlower, AnonymousParty investigator){
        this.badCompany = badCompany;
        this.whistleBlower = whistleBlower;
        this.investigator = investigator;
        linearId = new UniqueIdentifier();
    }
    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(whistleBlower, investigator);
    }
}


