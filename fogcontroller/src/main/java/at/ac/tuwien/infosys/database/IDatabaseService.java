package at.ac.tuwien.infosys.database;

import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.util.DeviceType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Kevin Bachmann on 11/11/2016.
 * Database service to retrieve and persist information in the local database
 */
public interface IDatabaseService {
    /**
     * GENERAL DB METHODS
     * The following three methods allow the user to delete, set, get values from the local database
     */

    void delete(String key);
    String getValue(String key);
    void setValue(String key, String value);

    /**
     * Returns all the available keys of the redis database
     * @return set with string keys
     */
    Set<String> getKeys();

    /**
     * GETTER
     * The following getter methods return the specified resources from the local running database
     */

    /**
     * Get all key/value pairs stored in the db
     * @return key/value map
     */
    Map<String, String> getAll();
    Utilization getUtilization();
    DeviceType getDeviceType();
    String getDeviceId();
    String getIp();
    int getPort();
    Fogdevice getParent();
   // Fogdevice getClosestNeighbor();
    Set<Fognode> getChildren();
    Location getLocation();
    Fogdevice getDeviceInformation();
    List<ServiceData> getServiceData();

    /**
     * SETTER
     * The following setter methods define or override the specified resources to the local running database
     */

    void setUtilization(Utilization utilization);
    void setDeviceType(DeviceType value);
    void setDeviceId(String value);
    void setPort(int value);
    void setIp(String value);
    void setParent(Fogdevice parent);
   // void setClosestNeighbor(Fogdevice neighbor);
    void setChildren(Set<Fognode> children);
    void addChild(Fognode child);
    void removeChild(Fognode child);
    void removeChildren();
    void setLocation(Location loc);
    void addParent(Fognode parent);
    void removeParent(Fognode parent);
    void removeParents();
    void setServiceData(List<ServiceData> data);
}