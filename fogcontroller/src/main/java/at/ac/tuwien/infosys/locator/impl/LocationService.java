package at.ac.tuwien.infosys.locator.impl;

import at.ac.tuwien.infosys.communication.ICommunicationService;
import at.ac.tuwien.infosys.database.IDatabaseService;
import at.ac.tuwien.infosys.locator.ILocationService;
import at.ac.tuwien.infosys.model.Fognode;
import at.ac.tuwien.infosys.model.Fogdevice;
import at.ac.tuwien.infosys.model.Location;
import at.ac.tuwien.infosys.model.LocationRange;
import at.ac.tuwien.infosys.util.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Kevin Bachmann on 14/11/2016.
 */
@Service
@Slf4j
public class LocationService implements ILocationService {

    @Autowired
    private IDatabaseService dbService;

    @Autowired
    private ICommunicationService commService;

    public Fogdevice getResponsibleParent(long latitude, long longitude){
        Set<Fognode> possibleParents = new HashSet<Fognode>();

//        Set<Fogcontrolnode> parents = dbService.getParents(); // static parents
        // not just get the children of the cloud but as well all the fog control nodes
        Set<Fognode> parents = new HashSet<Fognode>();
        Set<Fognode> directChildren = dbService.getChildren();
        for(Fognode directChild : directChildren){
            if(directChild.getType().equals(DeviceType.FOG_NODE)){
                parents.add(directChild);
                // send get children request
                Set<Fogdevice> childChildren = commService.getChildrenOfChild(directChild);
                for(Fogdevice fd: childChildren){
                    if(fd.getType().equals(DeviceType.FOG_NODE)){
                        // convert fd to fcn and get location of them
                        LocationRange lr = commService.getLocationRange(fd);
                        Fognode fn = new Fognode(fd.getId(), fd.getType(), fd.getIp(), fd.getPort(), fd.getLocation(),
                                fd.getParent(), fd.getServiceTypes(), lr);
                        parents.add(fn);
                    }
                }
            }
        }

        for(Fognode f : parents){
            if((f.getLocation().getLatitude() != latitude || f.getLocation().getLongitude() != longitude) &&
                    f.getLocationRange().isInside(new Location(latitude, longitude))){
                // check that it is not the caller itself and it fits into the location range
                possibleParents.add(f);
            }
        }
        if(possibleParents.size()==1) return (Fogdevice) possibleParents.toArray()[0];
        if(possibleParents.size()>1){
            return findClosestDevice(latitude, longitude, possibleParents);
        }
        // if no matching parent is found the cloud-fog middleware is returned
        Fogdevice fogcontroller = new Fogdevice(dbService.getDeviceId(), DeviceType.FOG_CONTROLLER, dbService.getIp(),
                dbService.getPort(), dbService.getLocation(), null);
        return fogcontroller;
    }

    public Fogdevice getClosestNeighborFN(long latitude, long longitude){
        Set<Fognode> possibleNeighborFNs = new HashSet<Fognode>();

        Set<Fognode> allDirectFNs = dbService.getChildren();
        if (allDirectFNs.size()==1) return null;
        if (allDirectFNs.size()>1)
            for(Fognode directFCN : allDirectFNs){
                if(directFCN.getType().equals(DeviceType.FOG_NODE)){
                    if ((directFCN.getLocation().getLatitude()!=latitude)&&(directFCN.getLocation().getLongitude()!=longitude)){
                        possibleNeighborFNs.add(directFCN);}
            }
            return findClosestDevice(latitude,longitude,possibleNeighborFNs);
        }
        return null;

    }

    public Fogdevice findClosestDevice(long latitude, long longitude, Set<Fognode> parents){
        double distance = Integer.MAX_VALUE;
        Fognode parent = null;

        for(Fognode f : parents){
            long tempLat = f.getLocation().getLatitude();
            long tempLong = f.getLocation().getLongitude();
            // calculate the distance between the locations by calculating the sqrt(x^2+y^2)
            double tempDistance = Math.hypot((double)(tempLong-longitude), (double)(tempLat-latitude));
            if(tempDistance < distance) {
                distance = tempDistance;
                parent = f;
            }
        }
        return parent;
    }
}
