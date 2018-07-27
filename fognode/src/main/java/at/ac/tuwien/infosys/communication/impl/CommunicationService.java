package at.ac.tuwien.infosys.communication.impl;

import at.ac.tuwien.infosys.communication.ICommunicationService;
import at.ac.tuwien.infosys.communication.IRequestService;
import at.ac.tuwien.infosys.database.IDatabaseService;
import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.util.Constants;
import at.ac.tuwien.infosys.util.PropertyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Created by Kevin Bachmann on 03/11/2016.
 */
@Service
@Slf4j
public class CommunicationService implements ICommunicationService {

    @Autowired
    private PropertyService propertyService;

    @Value("${server.port}")
    private int port;

    @Autowired
    private IDatabaseService dbService;

    @Autowired
    private IRequestService requestService;

    /**
     * Returns the parent url according to the parent saved in the local database
     * @return string with a built parent url
     */
    private String getParentUrl() {
        Fogdevice parent = dbService.getParent();
        if(parent == null) log.warn("No parent set for this Fog Device");
        return "http://" + parent.getIp() + ":" + parent.getPort();
    }

    private String getClosestNeighborUrl() {
        Fogdevice closestNeighborFN = dbService.getClosestNeighborFN();
        if(closestNeighborFN == null) log.warn("No closest neighbor FCN was set for this Fog Device");
        return "http://" + closestNeighborFN.getIp() + ":" + closestNeighborFN.getPort();
    }

    /**
     * Returns the cloud url according to the cloud saved in the local database
     * @return string with a built cloud url
     */
    private String getCloudUrl(){
        return "http://" + dbService.getCloudIp() + ":" + dbService.getCloudPort();
    }

    public <T> T sendToParent(String url, HttpMethod method, HttpEntity requestEntity, ParameterizedTypeReference<T> type ) {
        String parentUrl = getParentUrl();
        return requestService.sendRequest(parentUrl + url, method, requestEntity, type);
    }

    public <T> T sendToClosestNeighborFN(String url, HttpMethod method, HttpEntity requestEntity, ParameterizedTypeReference<T> type ) {
        String closestNeighborUrl = getClosestNeighborUrl();
        return requestService.sendRequest(closestNeighborUrl + url, method, requestEntity, type);
    }

    public <T> T sendToCloud(String url, HttpMethod method, HttpEntity requestEntity, ParameterizedTypeReference<T> type ){
        String cloudUrl = getCloudUrl();
        return requestService.sendRequest(cloudUrl + url, method, requestEntity, type);
    }

    public Message pair(Fogdevice fd){
        // fog device that wants to pair requests with its own information. then the fog control node sends back a ok
        dbService.addChild(fd);
        log.info("----- PAIR FROM IP="+ fd.getIp() +":"+fd.getPort()+" -----");
        return new Message("pair");
    }

    public Message pairClosestNeighbor(Fogdevice fd){
        // fog device that wants to pair requests with its own information. then the fog control node sends back a ok
        dbService.setClosestNeighborFN(fd);
        log.info("----- ClOSEST NEIGHBOR PAIR FROM IP="+ fd.getIp() +":"+fd.getPort()+" -----");
        return new Message("pair closest neighbor");
    }

    public Message ping(){
        return new Message("ping");
    }

    public void pingChildren(){
        Set<Fogdevice> children = dbService.getChildren();
        Set<Long> childrenDelays =new HashSet<Long>();
        if (children != null && children.size() > 0) {
            for (Iterator<Fogdevice> iterator = children.iterator(); iterator.hasNext(); ) {
                Fogdevice fd = iterator.next();
                Message m = null;
                try {
                    long start = System.currentTimeMillis();
                    m = sendPing(fd);
                    long end = System.currentTimeMillis();
                    childrenDelays.add(end-start);

                } catch (Exception ex) {
                    log.warn("Fogdevice with IP=" + fd.getIp() + ":" + fd.getPort() + " cannot be reached and is removed from children.");
                    dbService.removeChild(fd);
                    continue;
                }
                if (m == null || !m.isStatus()) {
                    dbService.removeChild(fd);
                }
            }
        }
        dbService.setChildrenDelays(childrenDelays);
    }

    public void pingClosestNeighbor(){

        Fogdevice parent = dbService.getParent();
        if (parent != null && parent.getPort() == Constants.PORT_FOG_CONTROLLER) {
            Fogdevice closestNeighborFN = dbService.getClosestNeighborFN();
//        if (closestNeighborFN == null){
//            log.info("There is no closest neighbor FCN. Requesting one more time.");
//            closestNeighborFN=requestClosestNeighbor(dbService.getLocation());
//            dbService.setClosestNeighborFN(closestNeighborFN);
//            Message m = null;
//            try {
//                m = sendPing(closestNeighborFN);
//            } catch (Exception ex) {
//                log.warn("Closest neighbor FCN with IP=" + closestNeighborFN.getIp() + ":" + closestNeighborFN.getPort() + " cannot be reached.");
//            }
//            if (m == null || !m.isStatus()) {
//                log.warn("Closest neighbor FCN with IP=" + closestNeighborFN.getIp() + ":" + closestNeighborFN.getPort() + " cannot be reached.");
//            }
            if (closestNeighborFN != null) {
                Message m = null;
                try {
                    long start = System.currentTimeMillis();
                    m = sendPing(closestNeighborFN);
                    long end = System.currentTimeMillis();
                    dbService.setClosestNeighborDelay(end - start);
                } catch (Exception ex) {
                    log.warn("Closest neighbor FN with IP=" + closestNeighborFN.getIp() + ":" + closestNeighborFN.getPort() + " cannot be reached.");
                }
                if (m == null || !m.isStatus()) {
                    log.warn("Closest neighbor FN with IP=" + closestNeighborFN.getIp() + ":" + closestNeighborFN.getPort() + " cannot be reached.");
                }
            }
        }

    }

    public Message sendPing(Fogdevice fd){
        return requestService.sendRequest(fd, Constants.URL_PING, HttpMethod.GET, null,
                new ParameterizedTypeReference<Message>(){});
    }

    public Message sendPairRequest() {
        // get information about this device
        Fogdevice fc = dbService.getDeviceInformation();
        return sendToParent(Constants.URL_PAIR_REQUEST, HttpMethod.POST, new HttpEntity(fc),
                new ParameterizedTypeReference<Message>(){});
    }

    public Message sendPairRequestClosestNeighbor() {
        // get information about this device
        Fogdevice fc = dbService.getDeviceInformation();
        return sendToClosestNeighborFN(Constants.URL_PAIR_REQUEST_CLOSEST_NEIGHBOR, HttpMethod.POST, new HttpEntity(fc),
                new ParameterizedTypeReference<Message>(){});
    }

    public Message sendManualPairRequest(Fogdevice fd) {
        // get information about this device
        Fogdevice fc = dbService.getDeviceInformation();
        Message m = requestService.sendRequest(fd, Constants.URL_PAIR_REQUEST, HttpMethod.POST, new HttpEntity(fc),
                new ParameterizedTypeReference<Message>(){});
        if(m != null && m.isStatus()){
            // if the manual request was successful, save the newly paired parent
            dbService.setParent(fd);
        }
        return m;
    }


    public Message sendManualPairRequestClosestNeighbor(Fogdevice fd) {
        // get information about this device
        Fogdevice fc = dbService.getDeviceInformation();
        long start = System.currentTimeMillis();
        Message m = requestService.sendRequest(fd, Constants.URL_MANUAL_PAIR_REQUEST_CLOSEST_NEIGHBOR, HttpMethod.POST, new HttpEntity(fc),
                new ParameterizedTypeReference<Message>(){});
        long end = System.currentTimeMillis();
        if(m != null && m.isStatus()){
            // if the manual request was successful, save the newly paired parent
            dbService.setClosestNeighborFN(fd);
            dbService.setClosestNeighborDelay(end-start);
        }
        return m;
    }


    public Fogdevice requestParent(Location loc){
        return sendToCloud(Constants.URL_REQUEST_PARENT +loc.getLatitude()+"/"+loc.getLongitude(), HttpMethod.GET,
                null, new ParameterizedTypeReference<Fogdevice>(){});
    }

    public Fogdevice requestClosestNeighbor(Location loc){
        return sendToCloud(Constants.URL_REQUEST_CLOSEST_NEIGHBOR +loc.getLatitude()+"/"+loc.getLongitude(), HttpMethod.GET,
                null, new ParameterizedTypeReference<Fogdevice>(){});
    }

    public Utilization getChildUtilization(Fogdevice fd){
        return requestService.sendRequest(fd, Constants.URL_DB_UTILIZATION, HttpMethod.GET, null,
                new ParameterizedTypeReference<Utilization>(){});
    }

    public Set<Utilization> getChildrenUtilization(){
        Set<Utilization> utils = new HashSet<Utilization>();
        for(Fogdevice fd : dbService.getChildren()) {
            utils.add(requestService.sendRequest(fd, Constants.URL_DB_UTILIZATION, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Utilization>() {}));
        }
        return utils;
    }

    public Set<Fogdevice> getChildrenOfChild(Fogdevice fd) {
        return requestService.sendRequest(fd, Constants.URL_DB_CHILDREN, HttpMethod.GET, null,
                new ParameterizedTypeReference<Set<Fogdevice>>() {});
    }

    public DockerContainer sendServiceDeploymentRequest(Fogdevice fd, TaskRequest ts){
        DockerContainer container = requestService.sendRequest(fd, Constants.URL_SERVICE_DEPLOY+ts.getServiceKey(), HttpMethod.POST, null,
                new ParameterizedTypeReference<DockerContainer>() {});
//        log.info(container.toString());
        return container;
    }

    public void sendServiceStopRequest(Fogdevice fd, String dockerId){
        requestService.sendRequest(fd, Constants.URL_SERVICE_STOPBYID+"/"+dockerId, HttpMethod.POST, null,
                new ParameterizedTypeReference<Set<Object>>() {});
    }

    public Set<DockerContainer> requestDeployedContainers(Fogdevice fd){
        Set<DockerContainer> containers = requestService.sendRequest(fd, Constants.URL_SERVICE_GET_CREATED, HttpMethod.GET, null,
                new ParameterizedTypeReference<Set<DockerContainer>>() {});
        return containers;
    }

    public LocationRange getLocationRange(){
        return propertyService.getLocationRange();
    }

    public void sendCloudServiceStopRequest(Fogdevice fd, String dockerId){
        requestService.sendRequest(fd, Constants.URL_CLOUD_STOP_SERVICE+"/"+dockerId, HttpMethod.POST, null,
                new ParameterizedTypeReference<Object>() {});
    }

    public void distributeDockerImage(Fogdevice fd, DockerImage image){
        requestService.sendRequest(fd, Constants.URL_SHARED_REGISTERIMAGE, HttpMethod.POST, new HttpEntity(image),
                new ParameterizedTypeReference<Message>() {});
    }



    /**
     * ----------------------------------  SCHEDULED  ----------------------------------
     */

    @Scheduled(fixedDelayString = "${fog.ping.delay}", initialDelay = 4*1000)
    public void scheduledPing(){
        pingChildren();
        pingClosestNeighbor();

    }


}
