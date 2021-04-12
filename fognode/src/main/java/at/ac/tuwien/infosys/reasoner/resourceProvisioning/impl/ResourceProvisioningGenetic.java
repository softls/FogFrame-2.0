package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl;

import at.ac.tuwien.infosys.communication.impl.CommunicationService;
import at.ac.tuwien.infosys.database.IDatabaseService;
import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.model.exception.ResourceProvisioningException;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.IResourceProvisioning;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic.FogChromosome;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic.FogGeneticAlgorithm;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic.FogRandomGeneMutation;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl.genetic.FogUniformCrossover;
import at.ac.tuwien.infosys.util.Constants;
import at.ac.tuwien.infosys.watchdog.WatchdogService;
import lombok.extern.slf4j.Slf4j;
import net.sf.javailp.Problem;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by olena on 12/13/17.
 */

@Service
@Slf4j
public class ResourceProvisioningGenetic implements IResourceProvisioning {
    @Autowired
    private CommunicationService commService;

    @Autowired
    private IDatabaseService dbService;

    @Autowired
    private WatchdogService watchdogService;

    private String logstarter = "--- RESPROV: ";

    private long appDeadline; //taken from any request
    private long appDeploymentTime;//taken from any request


    @Override
    public ApplicationAssignment handleTaskRequests(Set<Fogdevice> children, Set<TaskRequest> requests) throws ResourceProvisioningException {

        ApplicationAssignment ass = new ApplicationAssignment();
        List<TaskAssignment> taskAssignments = new ArrayList<TaskAssignment>();
        List<TaskRequest> openRequests = new ArrayList<TaskRequest>();

        List<TaskRequest> requestsList = new ArrayList<>();
        requestsList.addAll(requests);
        log.info(logstarter + "task requests: " + requestsList);

        long startTime = System.currentTimeMillis();

        for (TaskRequest taskRequest : requestsList) {
            this.appDeadline = taskRequest.getDeadlineOnDeployment() * 60 * 1000;
            this.appDeploymentTime = taskRequest.getPreviousDeploymentTime() * 60 * 1000;
            log.info(logstarter + "application deadline: " + this.appDeadline);
            break;
        }

        List<Fogdevice> childrenList = new ArrayList<>();
        childrenList.addAll(children);
        log.info(logstarter + "getting children device data");

        List<Utilization> childrenUtilization = new ArrayList<>();
        List<Integer> childrenContainers =new ArrayList<>();
        for (Fogdevice fd:childrenList) {
            try {
                Utilization u = null;
                do {
                    u = commService.getChildUtilization(fd);
                    if (u == null || u.getStorage() == 0 || u.getCpu() == 0 || u.getRam() == 0)
                        Thread.sleep(10);
                } while (u == null || u.getStorage() == 0 || u.getCpu() == 0 || u.getRam() == 0);
                childrenUtilization.add(u);
                int numberofDeployedContainers = commService.requestDeployedContainers(fd).size();
                childrenContainers.add(numberofDeployedContainers);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info(logstarter + "children containers: " + childrenContainers.toString());
        int numberOfContainersInFN = commService.requestDeployedContainers(dbService.getDeviceInformation()).size();
        log.info(logstarter + "children: " + children.toString());

        int geneCount = requestsList.size();
        try {
            int populationSize = 100;
            double crossoverRate = 0.8;
            double elitismRate = 0.2;
            double mutationRate = 0.02;
            log.info(logstarter + "Instantiating Genetic Algorithm... ");
            FogGeneticAlgorithm fogGeneticAlgorithm = new FogGeneticAlgorithm(
                    geneCount,
                    new FogUniformCrossover(0.5),
                    new TournamentSelection(2),
                    new FogRandomGeneMutation(),
                    populationSize,
                    crossoverRate,
                    elitismRate,
                    mutationRate,
                    childrenList,
                    requestsList,
                    dbService.getLastDeploymentTime(),
                    dbService.getAverageDeploymentTime(),
                    childrenUtilization,
                    childrenContainers,
                    dbService.getDeviceInformation(),
                    numberOfContainersInFN
                    );

            long time1 = System.currentTimeMillis();
            fogGeneticAlgorithm.run();
            log.info(logstarter + "Calculations finished... ");
            long time2 = System.currentTimeMillis();
            long calcTime = time2 - time1;
            System.out.println("Computation time = " + calcTime);

            FogChromosome fittestChromosome = fogGeneticAlgorithm.getFittestChromosome();

            long endTime = System.currentTimeMillis();
            long computTime = endTime-startTime;
            log.info(logstarter + "Computational Time, millis = "+computTime);

            log.info(logstarter + "Fittest chromosome:"+fittestChromosome.toString());

            List<Integer> fittestRepresentation = fittestChromosome.getFogChromosomeRepresentation();

            int count = 0;
            for (TaskRequest req : requestsList) {
                int gene = fittestRepresentation.get(count);
                if (gene == childrenList.size() + 1){
                    log.info(logstarter + "send all deployment requests to neighbor colony ");
                    long time3 = System.currentTimeMillis();
                    calcTime=time3-time1;
                    Iterator<TaskRequest> requestIt = requests.iterator();
                    while (requestIt.hasNext()) {
                        TaskRequest request = requestIt.next();
                        request.setPreviousDeploymentTime(calcTime);
                        openRequests.add(request);
                    }
                    ass.setAssignedTasks(taskAssignments);
                    ass.setOpenRequests(openRequests);
                    return ass;
                }
                count++;
            }
            count = 0;
            for (TaskRequest req : requestsList) {

                int gene = fittestRepresentation.get(count);
                if (gene < childrenList.size()) { // if gene is either fog cell, or fn
                    Fogdevice fd = childrenList.get(gene);
                    Set<DockerContainer> deployedContainers = commService.requestDeployedContainers(fd);
                    int containerCount = 0;
                    if (deployedContainers != null) {
                        containerCount = deployedContainers.size();
                    }
                    log.info(logstarter + containerCount + " already deployed containers");
                    if (containerCount >= Constants.MAX_CONTAINERS) {
                        log.info(logstarter + "send all deployment requests to neighbor colony ");
                        long time3 = System.currentTimeMillis();
                        calcTime=time3-time1;
                        Iterator<TaskRequest> requestIt = requests.iterator();
                        while (requestIt.hasNext()) {
                            TaskRequest request = requestIt.next();
                            request.setPreviousDeploymentTime(calcTime);
                            openRequests.add(request);
                        }
                        break;
                    }
                    log.info(logstarter + "send deployment request to " + fd.getIp() + ": " + req);
                    DockerContainer container = commService.sendServiceDeploymentRequest(fd, req);

                    TaskAssignment taskAssignment = new TaskAssignment(fd, req, container, false);
                    taskAssignments.add(taskAssignment);

                }
                if (gene == childrenList.size()) { // if gene is fn
                    log.info(logstarter + "send deployment request to myself " + req);
                    Fogdevice fd = dbService.getDeviceInformation();
                    Set<DockerContainer> deployedContainers = commService.requestDeployedContainers(fd);
                    int containerCount = 0;
                    if (deployedContainers != null) {
                        containerCount = deployedContainers.size();
                    }
                    log.info(logstarter + containerCount + " already deployed containers");
                    if (containerCount >= Constants.MAX_CONTAINERS) {
                        log.info(logstarter + "send all deployment requests to neighbor colony ");
                        long time3 = System.currentTimeMillis();
                        calcTime=time3-time1;
                        Iterator<TaskRequest> requestIt = requests.iterator();
                        while (requestIt.hasNext()) {
                            TaskRequest request = requestIt.next();
                            request.setPreviousDeploymentTime(calcTime);
                            openRequests.add(request);
                        }
                        break;
                    }
                    log.info(logstarter + "send deployment request to " + fd.getIp() + ": " + req);
                    DockerContainer container = commService.sendServiceDeploymentRequest(fd, req);

                    TaskAssignment taskAssignment = new TaskAssignment(fd, req, container, false);
                    taskAssignments.add(taskAssignment);

                }
                if (gene == childrenList.size() + 2) { // if gene is cloud
                    log.info(logstarter + "send deployment request to cloud " + req);
                    req.setCloudTask(true);
                    openRequests.add(req);

                }
                if (gene == childrenList.size() + 1) { //if gene is neighbor
                    log.info(logstarter + "send all deployment requests to neighbor colony ");
                    long time3 = System.currentTimeMillis();
                    calcTime=time3-time1;
                    Iterator<TaskRequest> requestIt = requests.iterator();
                    while (requestIt.hasNext()) {
                        TaskRequest request = requestIt.next();
                        request.setPreviousDeploymentTime(calcTime);
                        openRequests.add(request);
                    }
                    break;
                }
                count++;
            }
            log.info(logstarter + "finished the resource provisioning of the fog tasks");

            ass.setAssignedTasks(taskAssignments);
            ass.setOpenRequests(openRequests);

        } catch (Exception e) {

            e.printStackTrace();

            throw new ResourceProvisioningException("", ass, e);
        }
        return ass;
    }
}
