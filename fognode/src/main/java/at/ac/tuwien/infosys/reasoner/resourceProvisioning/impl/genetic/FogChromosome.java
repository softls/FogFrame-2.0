package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic;

/**
 * Created by olena on 12/13/17.
 */

import at.ac.tuwien.infosys.model.Fogdevice;
import at.ac.tuwien.infosys.model.TaskRequest;
import at.ac.tuwien.infosys.model.Utilization;
import at.ac.tuwien.infosys.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.InvalidRepresentationException;

import java.util.*;

@Slf4j
public class FogChromosome extends AbstractListChromosome<Integer> {


    private double chromosomeFitness=0;

    private List<TaskRequest> requests = new ArrayList<>();
    private List<Fogdevice> children=new ArrayList<>();
    private List<Fogdevice> fogdevices = new ArrayList<>();
    private List<Utilization> childrenUtilization =new ArrayList<>();
    private Fogdevice fn;
    private List<Integer> childrenContainers=new ArrayList<>();
    private int fnContainers;

    private long appDeadline; //taken from any request
    private long appDeploymentTime;//taken from any request
    private long appDuration;
    private double lastDeploymentTime;
    private double averageDeploymentTime;

    private long MINUTE = 60000; //number of miliseconds in a minute

    private List<Double> responseTimes;


    public FogChromosome(List<Integer> representation) throws InvalidRepresentationException {
        super(representation);
    }

    public void setRequests(List<TaskRequest> requests){
        this.requests = new ArrayList<TaskRequest>(requests);
        for(TaskRequest taskRequest:requests){
            this.appDeadline = taskRequest.getDeadlineOnDeployment()*MINUTE;
            this.appDeploymentTime = taskRequest.getPreviousDeploymentTime(); //already in millis
            this.appDuration = taskRequest.getDuration()*MINUTE;
            break;
        }
    }

    public void setChildrenUtilization(List<Utilization> childrenUtilization){
        this.childrenUtilization=new ArrayList<Utilization>(childrenUtilization);
    }
    public void setDeploymentTimes(double lastDeploymentTime, double averageDeploymentTime){
        this.lastDeploymentTime=lastDeploymentTime;
        this.averageDeploymentTime=averageDeploymentTime;
    }

    public void setChildrenDevices(List<Fogdevice> children, Fogdevice fn)
    {
        this.children = new ArrayList<Fogdevice>(children);
        this.fogdevices= new ArrayList<Fogdevice>(children);
        this.fn =fn;
    }

    public void setChildrenContainers(List<Integer> childrenContainers) {
        this.childrenContainers = new ArrayList<Integer>(childrenContainers);
    }

    public void setFnContainers(int fnContainers) {
        this.fnContainers = fnContainers;
    }

    public List<TaskRequest> getRequests() {
        return requests;
    }

    public List<Fogdevice> getChildren() {
        return children;
    }

    public double getLastDeploymentTime() {
        return lastDeploymentTime;
    }

    public double getAverageDeploymentTime() {
        return averageDeploymentTime;
    }

    public List<Utilization> getChildrenUtilization() {
        return childrenUtilization;
    }

    public List<Integer> getChildrenContainers() {
        return childrenContainers;
    }

    public int getFnContainers() {
        return fnContainers;
    }

    public Fogdevice getFn() {
        return fn;
    }

    @Override
    protected void checkValidity(List<Integer> list) throws InvalidRepresentationException {

    }

    @Override
    public AbstractListChromosome<Integer> newFixedLengthChromosome(List<Integer> chromosomeRepresentation) {
        return new FogChromosome(chromosomeRepresentation);
    }

    @Override
    public double fitness() {
        //assignment each module one time is guaranteed by chromosome representation
        List<Integer> genes = this.getRepresentation();

        double fitness=0.0;
        double deploymentTime = 0;
        double DEATH_PENATLTY = 100000.0;
        double weight=1;

        boolean typeViolation = false;
        boolean assignToNeighbour = false;
        boolean assignToCloud=false;

        int count = 0; //to iterate over genes
        for(TaskRequest taskRequest:requests){
            boolean fogtask = taskRequest.isFogTask();
            int gene = genes.get(count);
            String serviceType = taskRequest.getServiceType();
            //if assigned to child device
            if (gene<children.size()) {
                Fogdevice fd = children.get(gene);
                fitness=fitness+weight;        //encourage assignment to fog colony
                //constraint on utilization of child devices
                Utilization u = this.childrenUtilization.get(gene);
                fitness=fitness+checkUtilization(u,fitness);
                //constraint on service type of child devices
                if (!fd.getServiceTypes().contains(serviceType)) {
                    typeViolation=true;
                } else {
                    fitness = fitness + weight;
                }
            //if assigned to fog node
            } else if (gene==children.size()) {
                //constraint on service types
                if (!this.fn.getServiceTypes().contains(serviceType)) {
                    typeViolation=true;
                } else {
                    fitness = fitness +weight;
                }
                //if assigned- encourage fitness
                fitness=fitness+weight;
                //if needs sensor equipment and assigned- death penalty
                if (fogtask) fitness=fitness-DEATH_PENATLTY;
            //if has to be assigned to neighbor
            } else if (gene==children.size()+1) {
                assignToNeighbour=true;
                fitness=fitness+weight;
            //if assigned to cloud
            } else if (gene == children.size()+2) { //penalty on using cloud
                assignToCloud=true;
                if (fogtask) fitness=fitness-DEATH_PENATLTY;
            }
            //to iterate over genes in chromosome
            count++;
        }
        //calculate number of containers on each child and total number of containers
        int totalContainers=0;
        for (int i=0;i<childrenContainers.size();i++) {
            int containerCount = 0;
            for (int gene : genes) {
                if (gene == i) {
                    containerCount++;
                }
            }
            int usedContainers=containerCount+childrenContainers.get(i);
            totalContainers=totalContainers+usedContainers;
            if (usedContainers<= Constants.MAX_CONTAINERS){
                fitness=fitness+weight;
            } else fitness=fitness-DEATH_PENATLTY;

        }
        //calculate number of containers in fog node
        int fcnContainerCount = 0;
        for (int gene : genes) {
            if (gene == children.size()) {
                fcnContainerCount++;
            }
        }
        int usedContainers=fcnContainerCount+ fnContainers;
        if (usedContainers>=Constants.MAX_CONTAINERS)
            fitness=fitness-DEATH_PENATLTY;
        //constraint on total number of containers running in fog colony ( enhances fitness function)

//        totalContainers=totalContainers+usedContainers;
//        if ((totalContainers<20) && assignToNeighbour) {
//            fitness=fitness-1000;
//        }

        //assess deploymnt time if assigned to neighbor
        if (assignToNeighbour) {
           double alpha = 0.5;
           double movingAverage = alpha*this.lastDeploymentTime+(1-alpha)*this.averageDeploymentTime;
           deploymentTime += this.appDeploymentTime + movingAverage;
        }
        double responseTime = appDuration + deploymentTime;
        //constraint on on deadline
        if (this.appDeadline-responseTime>0) {
            //constraint on assigning to the cloud, but there is enough time to send to neighbor fog colony
//            if (assignToCloud) {
//                fitness=fitness-1000;
//            }
            fitness = fitness + weight;
        } else {
          //encourage assigning to the cloud when there is no time to wait
          //  if (assignToCloud){fitness=fitness+100;}
          //  else{
            //if deadline is violated- death
                fitness = fitness - DEATH_PENATLTY;
          //  }
        }
        //if service type is violated- death
        if (typeViolation) {
           fitness = fitness - DEATH_PENATLTY;
        }
        this.chromosomeFitness=fitness;
        return fitness;
    }

    public FogChromosome mutate() {
        List<Integer> genes = this.getRepresentation();
        int mutationIndex = GeneticAlgorithm.getRandomGenerator().nextInt(genes.size() + 1);
        List<Integer> mutated = new ArrayList<>();
        //get random gene, mutate assignment
        for (int i = 0; i < genes.size(); i++) {
            Integer gene = genes.get(i);
            if (i == mutationIndex) {
                mutated.add(GeneticAlgorithm.getRandomGenerator().nextInt(children.size()+3));
            } else
                mutated.add(gene);
        }
        //create new chromosome
        FogChromosome newChromosome = (FogChromosome) newFixedLengthChromosome(mutated);
        newChromosome.setChildrenDevices(this.children, this.fn);
        newChromosome.setRequests(this.requests);
        newChromosome.setDeploymentTimes(this.lastDeploymentTime,this.averageDeploymentTime);
        newChromosome.setChildrenUtilization(this.childrenUtilization);
        newChromosome.setChildrenContainers(this.childrenContainers);
        newChromosome.setFnContainers(this.fnContainers);
        return newChromosome;

    }

    public double calculateGoal() {
        List<Integer> genes = this.getRepresentation();
        int countGene = 0;
        int countService = 0;
        double goal = 0;
        boolean assignedToNeighbor = false;
        for(TaskRequest taskRequest:requests){
            Integer gene = genes.get(countGene);
            if (gene<children.size()+1){
                countService++;
            }
            if (gene==children.size()+1){
                assignedToNeighbor=true;
            }
            countGene++;
        }
        double coefficient = calculateProximityToWaitingTime();
        if (!assignedToNeighbor) {
            goal = coefficient * (countService);
        }else{
            goal = coefficient * requests.size();
        }

        return goal;
    }



    public List<Integer> getFogChromosomeRepresentation() {
        return this.getRepresentation();
    }

    //tbd
    private double calculateExecutionDelayInFogDevice(TaskRequest request, Fogdevice fogdevice) {
        return 0;
    }
    private double calculateExecutionDelayInFogNode(TaskRequest request) {
        return 0;
    }
    private double calculateExecutionDelayInCloud(TaskRequest request) {
        return 0;
    }

    private double calculateProximityToWaitingTime() {
        double coefficient;
        double divisor =this.appDeadline-this.appDeploymentTime;
        try {
            if (divisor == 0) {
                throw new IllegalArgumentException("Argument 'D_A - w_A' is 0");
            }
            if (divisor < 0) {
                coefficient=-1;
                return coefficient;
            }
            if (divisor > 0) {
                coefficient = 1 / divisor;
                System.out.println("Coefficient= " + coefficient);
                return coefficient;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return Double.POSITIVE_INFINITY;
    }

    private double checkUtilization(Utilization u, double fitness){
        if (u.getCpu()<70) {
            fitness=fitness + 10;
        } else {
            fitness=fitness - 10;
        }

        if (u.getRam()<89.5) {
            fitness=fitness + 10;
        } else {
            fitness=fitness - 10;
        }

        if (u.getStorage()>5) {
            fitness=fitness + 10;
        } else {
            fitness=fitness - 10;
        }
        return fitness;

    }


}
