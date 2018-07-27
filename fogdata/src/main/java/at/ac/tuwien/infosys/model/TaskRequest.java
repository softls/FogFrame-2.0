package at.ac.tuwien.infosys.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Kevin Bachmann on 11/11/2016.
 */
@Data
@EqualsAndHashCode
public class TaskRequest implements Comparable<TaskRequest>, Serializable {

    private String id;
    private String serviceKey;
    private String serviceType;
    private boolean fogTask;
    private boolean cloudTask;
    private double cpuDemand;
    private double ramDemand;
    private double storageDemand;

    private long previousDeploymentTime; //if one of services was propagated from another colony
    private int deadlineOnDeployment; //deadline on deployment of the application
    private int duration;

    public TaskRequest() {
        this.id = UUID.randomUUID().toString();
        fogTask = false;
        cloudTask = false;
    }

    public TaskRequest(String serviceKey, String serviceType, boolean fogTask, boolean cloudTask){
        this.id = UUID.randomUUID().toString();
        this.serviceKey = serviceKey;
        this.serviceType = serviceType;
        this.fogTask = fogTask;
        this.cloudTask = cloudTask;
    }

    public TaskRequest(String serviceKey, String serviceType, boolean fogTask, boolean cloudTask, double cpuDemand, double ramDemand, double storageDemand){
        this.id = UUID.randomUUID().toString();
        this.serviceKey = serviceKey;
        this.serviceType = serviceType;
        this.fogTask = fogTask;
        this.cloudTask = cloudTask;
        this.cpuDemand = cpuDemand;
        this.ramDemand=ramDemand;
        this.storageDemand=storageDemand;
    }

    public TaskRequest(String serviceKey, String serviceType, boolean fogTask, boolean cloudTask, int previousDeploymentTime, int deadlineOnDeployment, double cpuDemand, double ramDemand, double storageDemand){
        this.id = UUID.randomUUID().toString();
        this.serviceKey = serviceKey;
        this.serviceType = serviceType;
        this.fogTask = fogTask;
        this.cloudTask = cloudTask;
        this.previousDeploymentTime=previousDeploymentTime;
        this.deadlineOnDeployment=deadlineOnDeployment;
        this.cpuDemand = cpuDemand;
        this.ramDemand=ramDemand;
        this.storageDemand=storageDemand;
    }

    public TaskRequest(String serviceKey, String serviceType, boolean fogTask, boolean cloudTask, long previousDeploymentTime, int deadlineOnDeployment, int duration){
        this.id = UUID.randomUUID().toString();
        this.serviceKey = serviceKey;
        this.serviceType = serviceType;
        this.fogTask = fogTask;
        this.cloudTask = cloudTask;
        this.previousDeploymentTime=previousDeploymentTime;
        this.deadlineOnDeployment=deadlineOnDeployment;
        this.duration=duration;
    }

    public void setPreviousDeploymentTime(long previousDeploymentTime) {
        this.previousDeploymentTime = previousDeploymentTime;
    }

    public long getPreviousDeploymentTime() {
        return previousDeploymentTime;
    }

    @Override
    public int compareTo(TaskRequest o) {
        return this.getServiceType().compareTo(o.getServiceType());
    }

    @Override
    public String toString() {
        return "TaskRequest{" +
                "serviceKey='" + serviceKey + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", fogTask=" + fogTask +
                ", cloudTask=" + cloudTask +
                '}';
    }
}
