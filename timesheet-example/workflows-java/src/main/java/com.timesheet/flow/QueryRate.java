package com.timesheet.flow;


import co.paralleluniverse.fibers.Suspendable;
import com.timesheet.service.Rate;
import com.timesheet.service.RateOf;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.Party;

@InitiatingFlow
public class QueryRate extends FlowLogic<Rate> {
    private RateOf rateOf;
    private Party oracle;

    public QueryRate(RateOf rateOf, Party oracle) {
        this.rateOf = rateOf;
        this.oracle = oracle;
    }

    @Suspendable
    @Override
    public Rate call() throws FlowException {
        return initiateFlow(oracle).sendAndReceive(Rate.class, rateOf).unwrap((it ->{

            if(it.getOf().getCompany().equals(rateOf.getCompany()) && (it.getOf().getContractor().equals(rateOf.getContractor()))){
                return it;
            }else{
                throw new FlowException("Rate doesn't match");

            }
        }));
    }
}