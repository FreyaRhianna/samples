package com.timesheet.service;

import net.corda.core.identity.Party;

public class RateOf {
    private Party contractor;
    private Party company;

    public RateOf(Party contractor, Party company) {
        this.contractor = contractor;
        this.company = company;
    }

    public Party getContractor() {
        return contractor;
    }

    public Party getCompany() {
        return company;
    }
}
