package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.genetics.*;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by olena on 12/13/17.
 */
public class FogUniformCrossover extends UniformCrossover<FogChromosome> {
    public FogUniformCrossover(double ratio) throws OutOfRangeException {
        super(ratio);
    }

    @Override
    public ChromosomePair crossover(final Chromosome first, final Chromosome second)
            throws DimensionMismatchException, MathIllegalArgumentException {

        if (!(first instanceof AbstractListChromosome<?> && second instanceof AbstractListChromosome<?>)) {
            throw new MathIllegalArgumentException(LocalizedFormats.INVALID_FIXED_LENGTH_CHROMOSOME);
        }
        return mate((FogChromosome) first, (FogChromosome) second);
    }

    /**
     * Helper for {@link #crossover(Chromosome, Chromosome)}. Performs the actual crossover.
     *
     * @param first the first chromosome
     * @param second the second chromosome
     * @return the pair of new chromosomes that resulted from the crossover
     * @throws DimensionMismatchException if the length of the two chromosomes is different
     */
    private ChromosomePair mate(final FogChromosome first,
                                final FogChromosome second) throws DimensionMismatchException {
        final int length = first.getLength();
        if (length != second.getLength()) {
            throw new DimensionMismatchException(second.getLength(), length);
        }

        // array representations of the parents
        final List<Integer> parent1Rep = first.getFogChromosomeRepresentation();
        final List<Integer> parent2Rep = second.getFogChromosomeRepresentation();
        // and of the children
        final List<Integer> child1Rep = new ArrayList<Integer>(length);
        final List<Integer> child2Rep = new ArrayList<Integer>(length);

        final RandomGenerator random = GeneticAlgorithm.getRandomGenerator();
        double ratio = super.getRatio();
        for (int index = 0; index < length; index++) {

            if (random.nextDouble() < ratio) {
                // swap the bits -> take other parent
                child1Rep.add(parent2Rep.get(index));
                child2Rep.add(parent1Rep.get(index));
            } else {
                child1Rep.add(parent1Rep.get(index));
                child2Rep.add(parent2Rep.get(index));
            }
        }

        FogChromosome firstChromosome = (FogChromosome) first.newFixedLengthChromosome(child1Rep);

        firstChromosome.setRequests(first.getRequests());
        firstChromosome.setChildrenDevices(first.getChildren(), first.getFn());
        firstChromosome.setDeploymentTimes(first.getLastDeploymentTime(),first.getAverageDeploymentTime());
        firstChromosome.setChildrenUtilization(first.getChildrenUtilization());
        firstChromosome.setFnContainers(first.getFnContainers());
        firstChromosome.setChildrenContainers(first.getChildrenContainers());

        FogChromosome secondChromosome = (FogChromosome) second.newFixedLengthChromosome(child2Rep);
        secondChromosome.setRequests(second.getRequests());
        secondChromosome.setChildrenDevices(second.getChildren(), second.getFn());
        secondChromosome.setDeploymentTimes(second.getLastDeploymentTime(),first.getAverageDeploymentTime());
        secondChromosome.setChildrenUtilization(second.getChildrenUtilization());
        secondChromosome.setChildrenContainers(secondChromosome.getChildrenContainers());
        secondChromosome.setFnContainers(secondChromosome.getFnContainers());

        return new ChromosomePair(firstChromosome,
                secondChromosome);
    }
}
