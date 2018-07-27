package at.ac.tuwien.infosys.locator;

import at.ac.tuwien.infosys.locator.impl.LocationService;
import at.ac.tuwien.infosys.model.Fogdevice;
import at.ac.tuwien.infosys.model.exception.NoClosestNeighborFNException;
import at.ac.tuwien.infosys.model.exception.NoParentException;
import at.ac.tuwien.infosys.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by Kevin Bachmann on 14/11/2016.
 */
@RestController
@CrossOrigin(origins = "*")
@Slf4j
public class LocationController {

    @Autowired
    private LocationService locationService;

    /**
     * Returns the responsible parent according to the passed location and the location ranges of the fog control nodes
     * in the whole topology.
     * @param latitude location latitude
     * @param longitude location longitude
     * @return responsible parent
     */
    @RequestMapping(method = RequestMethod.GET, value= Constants.URL_REQUEST_PARENT+"{latitude}/{longitude}")
    public Fogdevice getResponsibleParent(@PathVariable long latitude, @PathVariable long longitude) throws NoParentException {
        log.info("--- Parent request from location: ("+latitude+"/"+longitude+") ---");
        return locationService.getResponsibleParent(latitude, longitude);
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_REQUEST_CLOSEST_NEIGHBOR+"{latitude}/{longitude}")
    public  Fogdevice getClosestNeighbor(@PathVariable long latitude, @PathVariable long longitude) throws NoClosestNeighborFNException {
        log.info("--- Closest Neighbor FN request from location: ("+latitude+"/"+longitude+") ---");
        Fogdevice fd= locationService.getClosestNeighborFN(latitude, longitude);
        if (fd!=null) {
            log.info("----- The closest neighbor is " + fd.getIp() +" ("+fd.getLocation().getLatitude() + "/" +fd.getLocation().getLongitude()+")");
        } else log.info("----- The closest neighbor cannot be found, no other FN is connected to network. Returning null. -----");
        return fd;
    }
}
