package com.example.state

import com.example.contract.SanctionedEntitiesContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

/**
 * The state object recording list of untrusted parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param badPeople list of untrusted parties.
 * @param issuer the party issuing the sanctioned list.
 */
@BelongsToContract(SanctionedEntitiesContract::class)
data class SanctionedEntities(
    val badPeople: List<Party>,
    val issuer: Party,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) :
    LinearState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(issuer)
}
