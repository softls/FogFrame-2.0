package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl;

import at.ac.tuwien.infosys.communication.impl.CommunicationService;
import at.ac.tuwien.infosys.database.impl.DatabaseService;
import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.model.exception.ResourceProvisioningException;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.IResourceProvisioning;
import at.ac.tuwien.infosys.util.Constants;
import at.ac.tuwien.infosys.watchdog.WatchdogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Kevin Bachmann on 02/11/2016.
 * Modified for multiple colonies Olena Skarlat
 */
@Primary
@Service
@Slf4j
public class ResourceProvisioningFirstFit implements IResourceProvisioning {

    @Autowired
    private CommunicationService commService;

    @Autowired
    private DatabaseService dbService;

    @Autowired
    private WatchdogService watchdogService;

    private String logstarter = "--- RESPROV: ";


    // sort incoming services according to service-types and then deploy them until the threshold is nearly exceeded
    public ApplicationAssignment handleTaskRequests(Set<Fogdevice> children, Set<TaskRequest> requests)
            throws ResourceProvisioningException {

        ApplicationAssignment ass = new ApplicationAssignment();
        try {
            List<TaskAssignment> taskAssignments = new ArrayList<TaskAssignment>();
            Fogdevice thisFN = dbService.getDeviceInformation();

            // 1. sort requests according to service-type (comparable interface)

            ArrayList<TaskRequest> sortedRequests = new ArrayList<TaskRequest>(requests);
            Collections.sort(sortedRequests);
            Iterator<TaskRequest> requestIt = sortedRequests.iterator();
            log.info(logstarter + "sorted task requests: " + sortedRequests);

            // 2. sort children according to service-type (comparable interface)
            ArrayList<Fogdevice> sortedDevices = new ArrayList<Fogdevice>(children);
            if (thisFN.getServiceTypes().size()>0) {
            sortedDevices.add(thisFN);}
            Collections.sort(sortedDevices);
            Iterator<Fogdevice> deviceIt = sortedDevices.iterator();

            log.info(logstarter + "sorted devices by service type: " + sortedDevices.toString());
            long startTime = System.currentTimeMillis();
            long computTime = 0;

            int round = 0;
            // 3. assign requests to children
            while (deviceIt.hasNext()) {
                Fogdevice fd = deviceIt.next();
                ArrayList<String> deviceServiceTypes = fd.getServiceTypes();

                    log.info(logstarter + "------- device: " + fd.getIp() + " with types:" + fd.getServiceTypes() + " -------");

                    for (String deviceServiceType : fd.getServiceTypes()) {

                        requestIt = sortedRequests.iterator();
                        while (requestIt.hasNext()) {
                            TaskRequest req = requestIt.next();
                            String reqType = req.getServiceType();

                            if (deviceServiceType.equals(reqType)) {
                                // check utilization
                                Utilization u = null;
                                do {
                                    u = commService.getChildUtilization(fd);
                                    if (u == null || u.getStorage() == 0 || u.getCpu() == 0 || u.getRam() == 0)
                                        Thread.sleep(10);
                                } while (u == null || u.getStorage() == 0 || u.getCpu() == 0 || u.getRam() == 0);
                                log.info(logstarter + "util of " + fd.getIp() + ": " + u.toString());

                                Set<DockerContainer> deployedContainers = commService.requestDeployedContainers(fd);
                                int containerCount = 0;
                                if (deployedContainers != null) {
                                    containerCount = deployedContainers.size();
                                }
                                log.info(logstarter + containerCount + " already deployed containers");
                                if (containerCount >= Constants.MAX_CONTAINERS) {

                                    break;
                                }

                                if (watchdogService.checkRules(u)) {
                                    // assign it to the child
                                    log.info(logstarter + "send deployment request to " + fd.getIp() + ": " + req);

                                    long intermediateTime=System.currentTimeMillis();
                                    computTime=computTime+intermediateTime-startTime;
                                    startTime=intermediateTime;

                                    DockerContainer container = commService.sendServiceDeploymentRequest(fd, req);

                                    TaskAssignment taskAssignment = new TaskAssignment(fd, req, container, false);
                                    taskAssignments.add(taskAssignment);

                                    // remove request from list that it does not get assigned anymore
                                    requestIt.remove();
                                }

                            } else {
                                // do nothing
                            }
                        }

                }

                // retry if the task requests are not all deployed and the iterator is empty
                if (!deviceIt.hasNext() && sortedRequests.size() > 0 && round < Constants.PROVISIONING_ROUNDS) {
                    round++;
                    deviceIt = sortedDevices.iterator();
                    log.info(logstarter + "-------------- ROUND " + (round) + " --------------");
                }
            }
            log.info(logstarter+"Computational time, millis = "+computTime);
            if (sortedRequests.size() > 0) {
                log.info(logstarter + "The following task requests could not be deployed\n" + sortedRequests + "\n----------------------------------------");
            }
            log.info(logstarter + "finished the resource provisioning of the fog tasks");

            ass.setAssignedTasks(taskAssignments);
            ass.setOpenRequests(sortedRequests);
        } catch(Exception e){
            throw new ResourceProvisioningException("", ass, e);
        }
        return ass;
    }
}
