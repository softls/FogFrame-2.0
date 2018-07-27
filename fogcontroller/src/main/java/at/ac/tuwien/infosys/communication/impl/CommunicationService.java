package at.ac.tuwien.infosys.communication.impl;

import at.ac.tuwien.infosys.communication.ICommunicationService;
import at.ac.tuwien.infosys.communication.IRequestService;
import at.ac.tuwien.infosys.database.impl.DatabaseService;
import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Kevin Bachmann on 20/11/2016.
 */
@Service
@Slf4j
public class CommunicationService implements ICommunicationService {

    @Autowired
    private IRequestService requestService;

    @Autowired
    private DatabaseService dbService;

    public Message pair(Fogdevice fd){
        // fog device that wants to pair requests with its own information. this device sends back a ok
        LocationRange lr = getLocationRange(fd);
        Fognode fcn = new Fognode(fd.getId(), fd.getType(), fd.getIp(), fd.getPort(), fd.getLocation(),
                                fd.getParent(), fd.getServiceTypes(), lr);
        dbService.addChild(fcn);
        log.info("----- PAIR FROM IP="+ fd.getIp() +":"+fd.getPort()+" -----");
        return new Message("pair");
    }

//    public Message pairClosestNeighbor(Fogdevice fd){
//        // fog device that wants to pair requests with its own information. this device sends back a ok
//        LocationRange lr = getLocationRange(fd);
//        Fogcontrolnode fcn = new Fogcontrolnode(fd.getId(), fd.getType(), fd.getIp(), fd.getPort(), fd.getLocation(),
//                fd.getParent(), fd.getServiceTypes(), lr);
//       // dbService.setClosestNeighbor(fcn);
//        log.info("----- CLOSEST NEIGHBOR PAIR FROM IP="+ fd.getIp() +":"+fd.getPort()+" -----");
//        return new Message("pair closest neighbor");
//    }

    public LocationRange getLocationRange(Fogdevice fd){
        return requestService.sendRequest(fd, Constants.URL_LOCATION_RANGE, HttpMethod.GET, null,
                new ParameterizedTypeReference<LocationRange>() {});
    }

    public void saveServiceData(List<ServiceData> data){
        dbService.setServiceData(data);
    }

    public Set<Fogdevice> getChildrenOfChild(Fogdevice fd) {
        return requestService.sendRequest(fd, Constants.URL_DB_CHILDREN, HttpMethod.GET, null,
                new ParameterizedTypeReference<Set<Fogdevice>>() {});
    }

    public void pingChildren(){
        Set<Fognode> children = dbService.getChildren();
        if (children != null && children.size() > 0) {
            for (Iterator<Fognode> iterator = children.iterator(); iterator.hasNext(); ) {
                Fognode fd = iterator.next();
                Message m = null;
                try {
                    m = sendPing(fd);
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
    }

    public Message sendPing(Fogdevice fd){
        return requestService.sendRequest(fd, Constants.URL_PING, HttpMethod.GET, null,
                new ParameterizedTypeReference<Message>(){});
    }

    /**
     * ----------------------------------  SCHEDULED  ----------------------------------
     */

    @Scheduled(fixedDelayString = "${fog.ping.delay}", initialDelay = 4*1000)
    public void scheduledPing(){
        pingChildren();
    }


}