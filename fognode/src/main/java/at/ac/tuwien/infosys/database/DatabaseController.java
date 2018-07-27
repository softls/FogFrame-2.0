package at.ac.tuwien.infosys.database;


import at.ac.tuwien.infosys.communication.ICommunicationService;
import at.ac.tuwien.infosys.model.Fogdevice;
import at.ac.tuwien.infosys.model.Utilization;
import at.ac.tuwien.infosys.util.Constants;
import at.ac.tuwien.infosys.util.DeviceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Kevin Bachmann on 27/10/2016.
 * Controller to retrieve and persist information in the local database according to selected REST endpoints
 */
@RestController
@CrossOrigin(origins = "*")
public class DatabaseController {

    @Autowired
    private IDatabaseService dbService;

    @Autowired
    private ICommunicationService commService;

    @PostConstruct
    public void init(){    }


    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_GETALL)
    public Map<String, String> getAll(){
        return dbService.getAll();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_DEVICETYPE)
    public DeviceType getDeviceType(){
        return dbService.getDeviceType();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_DEVICEID)
    public String getDeviceId(){
        return dbService.getDeviceId();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_IP)
    public String getIp(){
        return dbService.getIp();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_PARENT)
    public Fogdevice getParent(){
        return dbService.getParent();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_CHILDREN)
    public Set<Fogdevice> getChildren(){
        Set<Fogdevice> dbchildren = dbService.getChildren();
        Set<Fogdevice> children = new HashSet<Fogdevice>();
        for(Fogdevice child : dbchildren){
            children.add(child); // HINT: only if fog control nodes should deploy stuff as well
            if(child.getType().equals(DeviceType.FOG_NODE)){
                // send get children request to child
                Set<Fogdevice> childChildren = commService.getChildrenOfChild(child);
                children.addAll(childChildren);
            } else {
                children.add(child);
            }
        }
        return children;
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_UTILIZATION)
    public Utilization getUtilization() {
        return dbService.getUtilization();
    }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_AVERAGE_DEPLOYMENT_TIME)
    public long getAverageDeploymentTime() { return dbService.getAverageDeploymentTime(); }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_LAST_DEPLOYMENT_TIME)
    public long getLastDeploymentTime() { return dbService.getLastDeploymentTime(); }

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_CHILDREN_DELAYS)
    public Set<Long> getChildrenDelays() { return dbService.getChildrenDelays();}

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_CLOSEST_NEIGHBOR_DELAY)
    public long getClosestNeighborDelay() { return dbService.getClosestNeighborDelay();}

    @RequestMapping(method = RequestMethod.GET, value=Constants.URL_DB_CLOUD_DELAY)
    public long getCloudDelay() { return dbService.getCloudDelay();}



    /**
     * SETTERS
     */

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_UTILIZATION+"{value}")
    public void setUtilization(@PathVariable Utilization value){
        dbService.setUtilization(value);
    }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_IP+"{value}")
    public void setIp(@PathVariable String value){
        dbService.setIp(value);
    }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_PARENT)
    public void setParent(@RequestBody Fogdevice parent){
        dbService.setParent(parent);
    }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_CHILDREN)
    public void setChildren(@RequestBody Set<Fogdevice> children){
        dbService.setChildren(children);
    }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_CHILD)
    public void addChild(@RequestBody Fogdevice child){
        dbService.addChild(child);
    }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_AVERAGE_DEPLOYMENT_TIME+"{value}")
    public void setAverageDeploymentTime(@PathVariable String value){ dbService.setAverageDeploymentTime(Long.valueOf(value)); }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_LAST_DEPLOYMENT_TIME+"{value}")
    public void setLastDeploymentTime(@PathVariable String value){ dbService.setLastDeploymentTime(Long.valueOf(value)); }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_CLOSEST_NEIGHBOR_DELAY)
    public void setChildrenDelays(@RequestBody Set<Long> delays){ dbService.setChildrenDelays(delays); }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_CLOSEST_NEIGHBOR_DELAY+"{value}")
    public void setClosestNeighborDelay(@PathVariable String value){ dbService.setClosestNeighborDelay(Long.valueOf(value)); }

    @RequestMapping(method = RequestMethod.POST, value=Constants.URL_DB_CLOUD_DELAY+"{value}")
    public void setCloudDelay(@PathVariable String value){ dbService.setCloudDelay(Long.valueOf(value)); }

    @RequestMapping(method = RequestMethod.DELETE, value=Constants.URL_DB_CHILD)
    public void removeChild(@RequestBody Fogdevice child){ dbService.removeChild(child); }


}