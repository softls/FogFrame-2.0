package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.MutationPolicy;

/**
 * Created by olena on 12/13/17.
 */
public class FogRandomGeneMutation implements MutationPolicy {
    @Override
    public Chromosome mutate(Chromosome chromosome) throws MathIllegalArgumentException {
        FogChromosome originalChromosome = (FogChromosome) chromosome;
        return originalChromosome.mutate();
    }
}
