package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic;

import at.ac.tuwien.infosys.model.Fogdevice;
import at.ac.tuwien.infosys.model.TaskRequest;
import at.ac.tuwien.infosys.model.Utilization;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.genetics.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by olena on 12/13/17.
 */
@Slf4j
public class FogGeneticAlgorithm implements Runnable{

    private String logstarter = "--- RESPROV: GENETIC: ";
    private int chromosomeLength;
    private CrossoverPolicy crossoverPolicy;
    private SelectionPolicy selectionPolicy;
    private MutationPolicy mutationPolicy;
    private int populationSize;
    private double crossoverRate;
    private double elitismRate;
    private double mutationRate;

    private int fittestGenerationCount;
    private GeneticAlgorithm ga;
    private FogChromosome fittestChromosome;

    private List<Fogdevice> children;
    private List<Utilization> childrenUtilization;
    private List<Integer> childrenContainers;
    private Fogdevice fn;
    private int fnContainers;

    private List<TaskRequest> requests;
    private double lastDeploymentTime;
    private double averageDeploymentTime;

    public FogGeneticAlgorithm(int geneCount, CrossoverPolicy crossoverPolicy, SelectionPolicy selectionPolicy, MutationPolicy mutationPolicy, int populationSize, double crossoverRate, double elitismRate, double mutationRate, List<Fogdevice> children, List<TaskRequest> requests, double lastDeploymentTime, double averageDeploymentTime, List<Utilization> childrenUtilization, List<Integer> childrenContainers, Fogdevice fn, int fnContainers) {
        this.chromosomeLength = geneCount;
        this.crossoverPolicy = crossoverPolicy;
        this.selectionPolicy = selectionPolicy;
        this.mutationPolicy = mutationPolicy;
        this.populationSize = populationSize;
        this.crossoverRate = crossoverRate;
        this.elitismRate = elitismRate;
        this.mutationRate = mutationRate;

        this.children = new ArrayList<Fogdevice>(children);
        this.childrenUtilization =new ArrayList<Utilization>(childrenUtilization);
        this.childrenContainers=new ArrayList<Integer>(childrenContainers);
        this.fn = fn;
        this.fnContainers = fnContainers;

        this.requests = new ArrayList<TaskRequest>(requests);
        this.lastDeploymentTime=lastDeploymentTime;
        this.averageDeploymentTime=averageDeploymentTime;

        this.ga = new GeneticAlgorithm(this.crossoverPolicy, this.crossoverRate, this.mutationPolicy,
                this.mutationRate, this.selectionPolicy);
    }

    @Override
    public void run() {
        int generationCount = 0;
        Population currentPopulation = getInitialPopulation(this.chromosomeLength);
        double firstFitness = currentPopulation.getFittestChromosome().getFitness();

        double previousAverage=0;
        double fitnessIterativeAverage;
        double fitnessIterativeVariance=0;
        double nextFitness;

        int positiveSolutionCount=0;
        log.info(logstarter+"Init fitness = "+firstFitness);
        if (firstFitness>0) {
            positiveSolutionCount++;
            fitnessIterativeAverage = firstFitness;
            previousAverage = firstFitness;
        }

        Population nextPopulation;
        boolean stop = false;
        int countZeroTolerance=0;
        while (!stop) {
            generationCount++;
            nextPopulation = this.ga.nextGeneration(currentPopulation);
            FogChromosome nextFittestChromosome = (FogChromosome) nextPopulation.getFittestChromosome();
            nextFittestChromosome.setChildrenDevices(this.children, this.fn);
            nextFittestChromosome.setRequests(this.requests);
            nextFittestChromosome.setDeploymentTimes(this.lastDeploymentTime,this.averageDeploymentTime);
            nextFittestChromosome.setChildrenUtilization(this.childrenUtilization);
            nextFittestChromosome.setChildrenContainers(this.childrenContainers);
            nextFittestChromosome.setFnContainers(this.fnContainers);

            nextFitness = nextFittestChromosome.getFitness();
            //here we calculate iterative mean and variance
            if (nextFitness>0) {
                fitnessIterativeAverage = previousAverage + ((1.0 / (positiveSolutionCount + 1)) * (nextFitness - previousAverage));
                fitnessIterativeVariance = (1.0 / (positiveSolutionCount + 1)) * (fitnessIterativeVariance + (nextFitness - previousAverage) * (nextFitness - fitnessIterativeAverage));

                //calculation of tolerance value
                double tolerance = fitnessIterativeVariance / (nextFitness * nextFitness);
                log.info(logstarter + "nextfits=" + nextFitness + ", tol = " + tolerance + " Chrom: " + nextFittestChromosome.getFogChromosomeRepresentation());
                if ((tolerance - 0.01 < 0)){
                    if (tolerance!=0){

                        stop = true;
                        this.fittestChromosome = nextFittestChromosome;
                        this.fittestGenerationCount = generationCount;
                        log.info(logstarter + "Stopping condition of positive tolerance active, fitness= " + nextFitness);
                    } else {
                        countZeroTolerance++;
                        if (countZeroTolerance>2){
                            stop=true;
                            this.fittestChromosome = nextFittestChromosome;
                            this.fittestGenerationCount = generationCount;
                            log.info(logstarter + "Stopping condition of zero tolerance active, fitness= " + nextFitness);
                        }

                    }

                }
                positiveSolutionCount++;
                previousAverage=fitnessIterativeAverage;
            }
            if (generationCount == 100) {
                log.info(logstarter + "Stopping condition of number of generations active, fitness= " + nextFitness);
                stop = true;
                this.fittestChromosome = nextFittestChromosome;
                this.fittestGenerationCount = generationCount;
            }
            currentPopulation = nextPopulation;
        }
    }

    public int getNumberOfEvolvedGenerations() {
        return this.fittestGenerationCount;
    }

    public List<Integer> getFittestRepresentation() {
        return this.fittestChromosome.getFogChromosomeRepresentation();
    }

    public FogChromosome getFittestChromosome() {
        return this.fittestChromosome;
    }

    /* creates random population from 1 chromosome which reflects a current state */
    private Population getInitialPopulation(int lengh) {
        List<Chromosome> popList = new ArrayList<>();
        try {
            for (int i = 0; i < populationSize; i++) {// if mutate initial chromosome- i should set from 1
                List<Integer> newGenes = new ArrayList<>();
                for (int j = 0; j < lengh; j++) {
                    int newGene = GeneticAlgorithm.getRandomGenerator().nextInt(this.children.size()+3);//all fog cells +1 for cloud+ 1 for this fn+ 1 for neighbor colony
                    newGenes.add(newGene);
                }
                FogChromosome newFogChromosome = new FogChromosome(newGenes);
                newFogChromosome.setChildrenDevices(this.children, this.fn);
                newFogChromosome.setRequests(this.requests);
                newFogChromosome.setDeploymentTimes(this.lastDeploymentTime,this.averageDeploymentTime);
                newFogChromosome.setChildrenUtilization(this.childrenUtilization);
                newFogChromosome.setChildrenContainers(this.childrenContainers);
                newFogChromosome.setFnContainers(this.fnContainers);
                popList.add(newFogChromosome);
            }
        } catch (Exception e) {
            System.err.println("Exception in creating the initial population:" + e.getMessage());
        }
        return new ElitisticListPopulation(popList, popList.size(), elitismRate);
    }
}
