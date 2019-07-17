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
    private UniqueIdentifier linearID = new UniqueIdentifier();

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
