package com.timesheet.state;

import com.google.common.collect.ImmutableList;
import com.timesheet.contract.InvoiceContract;
import com.timesheet.schema.InvoiceSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

@BelongsToContract(InvoiceContract.class)
public class InvoiceState implements QueryableState, LinearState {

    private LocalDate date;
    private int hoursWorked;
    private Double rate;
    private Party contractor;
    private Party company;
    private Party oracle;
    private Boolean paid = false;
    private UniqueIdentifier linearID;

    public LocalDate getDate() {
        return date;
    }

    public int getHoursWorked() {
        return hoursWorked;
    }

    public Double getRate() {
        return rate;
    }

    public Party getContractor() {
        return contractor;
    }

    public Party getCompany() {
        return company;
    }

    public Party getOracle() {
        return oracle;
    }

    public Boolean getPaid() {
        return paid;
    }

    public UniqueIdentifier getLinearID() {
        return linearID;
    }

    @ConstructorForDeserialization
    public InvoiceState(LocalDate date, int hoursWorked, Double rate, Party contractor, Party company, Party oracle, Boolean paid, UniqueIdentifier linearID ){
        this.date = date;
        this.hoursWorked = hoursWorked;
        this.rate = rate;
        this.contractor = contractor;
        this.company = company;
        this.oracle = oracle;
        this.paid = paid;
        this.linearID = linearID;

    }

    public InvoiceState(LocalDate date, int hoursWorked, Double rate, Party contractor, Party company, Party oracle, Boolean paid ){
        this(date, hoursWorked, rate, contractor, company, oracle, paid, new UniqueIdentifier());

    }

    public InvoiceState(LocalDate date, int hoursWorked, Double rate, Party contractor, Party company, Party oracle ){
        this(date, hoursWorked, rate, contractor, company, oracle, false);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearID;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof  InvoiceSchemaV1){
            return new InvoiceSchemaV1.PersistentInvoice(
                    this.contractor.getName().toString(),
                    this.company.getName().toString(),
                    this.date,
                    this.hoursWorked,
                    this.rate,
                    this.linearID.getId()
            );
        }else{
            throw new IllegalArgumentException("Unrecognised schema " + schema.toString());
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new InvoiceSchemaV1());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(contractor,company);
    }
}
