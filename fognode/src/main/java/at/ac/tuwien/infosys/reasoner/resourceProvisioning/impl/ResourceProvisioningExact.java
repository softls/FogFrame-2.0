package at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl;

import at.ac.tuwien.infosys.communication.impl.CommunicationService;
import at.ac.tuwien.infosys.database.IDatabaseService;
import at.ac.tuwien.infosys.model.*;
import at.ac.tuwien.infosys.model.exception.ResourceProvisioningException;
import at.ac.tuwien.infosys.reasoner.resourceProvisioning.IResourceProvisioning;
import at.ac.tuwien.infosys.watchdog.WatchdogService;

import com.google.common.util.concurrent.UncheckedTimeoutException;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import lombok.extern.slf4j.Slf4j;
import net.sf.javailp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by olena on 11/22/17.
 * IBM CPLEX DOES NOT WORK ON RASPBERRY PI!!! BECAUSE THEY DON"T HAVE ARM BINARIES, only for INTEX x86-64!!!
 */

@Service
@Slf4j

public class ResourceProvisioningExact implements IResourceProvisioning{

    @Autowired
    private CommunicationService commService;

    @Autowired
    private IDatabaseService dbService;

    @Autowired
    private WatchdogService watchdogService;

    private String logstarter = "--- RESPROV: ";

    private static final int M =1000;     // big M for constraints
    private static final double MU = 0.9; // allowed util of resources

    private Problem problem;
    private Set<TaskRequest> requests;
    private Set<Fogdevice> children;
    private long appDeadline; //taken from any request
    private long appDeploymentTime;//taken from any request


    @Override
    public ApplicationAssignment handleTaskRequests(Set<Fogdevice> children, Set<TaskRequest> requests) throws ResourceProvisioningException {
        ApplicationAssignment ass = new ApplicationAssignment();
        List<TaskAssignment> taskAssignments = new ArrayList<TaskAssignment>();
        List<TaskRequest> openRequests = new ArrayList<TaskRequest>();

        this.requests = requests;
        log.info(logstarter + "task requests: " + requests);

        Iterator<TaskRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            TaskRequest taskRequest = iterator.next();

            this.appDeadline = taskRequest.getDeadlineOnDeployment()*60*1000;
            this.appDeploymentTime = taskRequest.getPreviousDeploymentTime()*60*1000;
            log.info(logstarter + "application deadline: " + this.appDeadline);
            break;
        }

        this.children = children;
        log.info(logstarter + "children: " + children.toString());

        try {
            log.info(logstarter + "Trying to instantiate CPLEX solver");
            Solver solver = new FogSolverCplex();
            ((SolverCPLEX) solver).addHook(new SolverCPLEX.Hook() {
                @Override
                public void call(IloCplex cplex, Map<Object, IloNumVar> varToNum) {
                    try {
                        cplex.setParam(IloCplex.DoubleParam.TiLim, 60);
                        log.info(logstarter + "CPLEX solver initiated");
                    } catch (IloException e) {
                        e.printStackTrace();
                    }
                }
            });
            this.problem = new Problem();
            log.info(logstarter + "Problem instantiated");
            addObjective(this.problem);
            log.info(logstarter + "Objective added");
            addConstraint_1(this.problem);
            log.info(logstarter + "Constraint 1 added");
            addConstraint_2(this.problem);
            log.info(logstarter + "Constraint 2 added");
            addConstraint_3(this.problem);
            log.info(logstarter + "Constraint 3 added");
            addConstraint_4(this.problem);
            log.info(logstarter + "Constraint 4 added");
            addConstraint_5(this.problem);
            log.info(logstarter + "Constraint 5 added");
            addConstraint_6(this.problem);
            log.info(logstarter + "Constraint 6 added");
            addConstraint_7(this.problem);
            log.info(logstarter + "Constraint 7 added");


            long time1 = System.currentTimeMillis();
            Result solve = solver.solve(this.problem);
            long time2 = System.currentTimeMillis();
            long calcTime = time2 - time1;
            log.info(logstarter + "Calculation time = " + calcTime);
            printResult(solve, children, requests);


            if (solve!=null){
                String decisionVariableN = "n";
                Number n = solve.get(decisionVariableN);
                if (n.equals(0)){
                    Iterator<TaskRequest> requestIterator = requests.iterator();
                    Iterator<Fogdevice> fogdeviceIterator = children.iterator();
                    while (requestIterator.hasNext()) {
                        TaskRequest taskRequest =requestIterator.next();
                        String appModuleName = taskRequest.getId();
                        String moduleType = taskRequest.getServiceType();
                        while (fogdeviceIterator.hasNext()) {
                            Fogdevice fogdevice = fogdeviceIterator.next();
                            if (fogdevice.getServiceTypes().contains(moduleType)) {
                                String fogDeviceName = fogdevice.getId();
                                String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                                Number x = solve.get(decisionVariableX);
                                if (x.equals(1)) {
                                    log.info(logstarter + "send deployment request to " + fogdevice.getIp() + ": " + taskRequest);
                                    DockerContainer container = commService.sendServiceDeploymentRequest(fogdevice, taskRequest);
                                    TaskAssignment taskAssignment = new TaskAssignment(fogdevice, taskRequest, container, false);
                                    taskAssignments.add(taskAssignment);

                                }
                            }
                        }
                        if (dbService.getServiceTypes().contains(moduleType)) {
                            String decisionVariableY = "y_" + appModuleName;
                            Number y = solve.get(decisionVariableY);
                            if (y.equals(1)) {
                                log.info(logstarter + "send deployment request to " + dbService.getIp() + ": " + taskRequest);
                                Fogdevice fogcontrolnode = dbService.getDeviceInformation();
                                DockerContainer container = commService.sendServiceDeploymentRequest(fogcontrolnode, taskRequest);
                                TaskAssignment taskAssignment = new TaskAssignment(fogcontrolnode, taskRequest, container, false);
                                taskAssignments.add(taskAssignment);

                            }
                        }
                        String decisionVariableZ = "z_" + "_" + appModuleName;
                        Number z = solve.get(decisionVariableZ);
                        log.info(logstarter +decisionVariableZ + " = " + z);
                        if (z.equals(1)) {
                            log.info(logstarter + "send deployment request to cloud " + taskRequest);
                            taskRequest.setCloudTask(true);
                        }
                        // remove request from list that it does not get assigned anymore TODO: check if necessary
                        requestIterator.remove();
                        break;
                    }
                }else {
                    log.info(logstarter + "send all deployment requests to neighbor ");
                    openRequests.addAll(requests);
                }
            }
            ass.setAssignedTasks(taskAssignments);
            ass.setOpenRequests(openRequests);
        } catch(Exception e){

            e.printStackTrace();

            throw new ResourceProvisioningException("", ass, e);
        }
        return ass;
    }


    private void printResult(Result solve, Set<Fogdevice> children, Set<TaskRequest> requests) {
        log.info(logstarter + "\n-------------------------\nSolved:   \n" + solve + "\n" +
                "-------------------------\n");
        log.info(logstarter + "Variables:");
        Collection<Object> variables = this.problem.getVariables();
        int i = 0;
        for (Object variable : variables) {
            log.info(logstarter + i + " " + variable);
            i++;
        }
        log.info(logstarter + "\n-------------------------\n");
        log.info(logstarter + "Constraints:");
        i = 0;
        for (Object obj : this.problem.getConstraints()) {
            log.info(logstarter + "Constraint " + i + ": " + obj.toString());
            i++;
        }
        log.info(logstarter + "\n-------------------------\n");
        log.info(logstarter + "Objective:");
        log.info(logstarter + this.problem.getObjective().toString());
        log.info(logstarter + "\n-------------------------\n");

        if (solve != null) {
            log.info(logstarter + "Objective = " + solve.getObjective());

            Iterator<TaskRequest> requestIterator = requests.iterator();
            Iterator<Fogdevice> fogdeviceIterator = children.iterator();

            while (requestIterator.hasNext()) {
                TaskRequest taskRequest = requestIterator.next();
                String appModuleName = taskRequest.getId();
                String moduleType = taskRequest.getServiceType();
                while (fogdeviceIterator.hasNext()) {
                    Fogdevice fogdevice = fogdeviceIterator.next();
                    if (fogdevice.getServiceTypes().contains(moduleType)) {
                        String fogDeviceName = fogdevice.getId();
                        String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                        Number x = solve.get(decisionVariableX);
                        log.info(logstarter + decisionVariableX + " = " + x);
                    }
                }
                if (dbService.getServiceTypes().contains(moduleType)) {
                    String decisionVariableY = "y_" + appModuleName;
                    Number y = solve.get(decisionVariableY);
                    log.info(logstarter +decisionVariableY + " = " + y);
                }

                String decisionVariableZ = "z_" + "_" + appModuleName;
                Number z = solve.get(decisionVariableZ);
                log.info(logstarter +decisionVariableZ + " = " + z);

            }
            String decisionVariableN = "n";
            Number n = solve.get(decisionVariableN);
            log.info(logstarter +decisionVariableN + " = " + n);
        }
        log.info(logstarter +"\n-------------------------\n");
    }

    private void addObjective(Problem problem) {
        Linear linear = new Linear();

        double coefficient = calculateProximityToWaitingTime();

        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();
        int count=0;
        while (requestIterator.hasNext()) {
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();

            //one time thing, because all requests have similar data abut deadline and previous deployment time of the application

            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice=fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    linear.add(coefficient, decisionVariableX);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                linear.add(coefficient, decisionVariableY);
            }
            count++;
        }
        String decisionVariableN = "n";
        linear.add(coefficient*count, decisionVariableN);
        problem.setObjective(linear, OptType.MAX);
    }

    // n=number of assigned services from an application, is at least one cannot be assigned, then the app goes to another colony, works together with addConstraint_2
    // if n-|A|>=0 then y<=0, else y>=1
    // n-|A|<= M(1-y)-1
    private void addConstraint_2(Problem problem){
        Linear linear =new Linear();

        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();
        int count=0;
        while (requestIterator.hasNext()) {
            count++;
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();
            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice=fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    linear.add(1, decisionVariableX);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                linear.add(1, decisionVariableY);
            }
            String decisionVariableZ = "z_" + "_" + appModuleName;
            linear.add(1, decisionVariableZ);
        }
        String decisionVariableN = "n";
        linear.add(M, decisionVariableN);//M=1000
        problem.add(linear, Operator.LE, M+count-1);
    }

    // n=number of assigned services from an application, is at least one cannot be assigned, then the app goes to another colony, works together with addConstraint_3
    // if n-|A|>=0 then y<=0, else y>=1
    // |A|-n<= My
    private void addConstraint_3(Problem problem){
        Linear linear =new Linear();

        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();
int count=0;
        while (requestIterator.hasNext()) {
            count++;
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();
            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice=fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    linear.add(1, decisionVariableX);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                linear.add(1, decisionVariableY);
            }

            String decisionVariableZ = "z_" + "_" + appModuleName;
            linear.add(1, decisionVariableZ);
        }
        String decisionVariableN = "n";
        linear.add(M, decisionVariableN);//M=1000
        problem.add(linear, Operator.GE, count);
    }

//utilization of fog cells
    private void addConstraint_4(Problem problem){
        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();

        while (fogdeviceIterator.hasNext()){
            Fogdevice fogdevice = fogdeviceIterator.next();
            Linear linearCpu = new Linear();
            Linear linearRam = new Linear();
            Linear linearStorage = new Linear();
            while(requestIterator.hasNext()){
                TaskRequest request=requestIterator.next();
                String appModuleName = request.getId();
                double cpuDemand = request.getCpuDemand();
                double ramDemand = request.getRamDemand();
                double storageDemand = request.getRamDemand();
                String moduleType = request.getServiceType();
                if (fogdevice.getServiceTypes().contains(moduleType)){
                    String decisionVariableX = "x_" + appModuleName + "_" + fogdevice.getId();
                    linearCpu.add(cpuDemand, decisionVariableX);
                    linearRam.add(ramDemand, decisionVariableX);
                    linearStorage.add(storageDemand,decisionVariableX);
                }
            }
            Utilization utilization = commService.getChildUtilization(fogdevice);
            problem.add (linearCpu, Operator.LE, MU*utilization.getCpu());
            problem.add(linearRam, Operator.LE, MU*utilization.getRam());
            problem.add(linearStorage, Operator.LE, MU*utilization.getStorage());
        }
    }

    //utilization of fog control node
    private void addConstraint_5(Problem problem){
        Linear linearCpu = new Linear();
        Linear linearRam = new Linear();
        Linear linearStorage = new Linear();

        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();

        while(requestIterator.hasNext()){
            TaskRequest request=requestIterator.next();
            String appModuleName = request.getId();
            double cpuDemand = request.getCpuDemand();
            double ramDemand = request.getRamDemand();
            double storageDemand = request.getRamDemand();
            String moduleType = request.getServiceType();
            if (dbService.getServiceTypes().contains(moduleType)){
                String decisionVariableX = "x_" + appModuleName;
                linearCpu.add(cpuDemand, decisionVariableX);
                linearRam.add(ramDemand, decisionVariableX);
                linearStorage.add(storageDemand,decisionVariableX);
            }
        }
        Utilization utilization = dbService.getUtilization();
        problem.add (linearCpu, Operator.LE, MU*utilization.getCpu());
        problem.add(linearRam, Operator.LE, MU*utilization.getRam());
        problem.add(linearStorage, Operator.LE, MU*utilization.getStorage());
    }

    //response time < deadline
    private void addConstraint_6(Problem problem)
    {
        double alpha = 0.5;
        double movingAverage = alpha*dbService.getLastDeploymentTime()+(1-alpha)*dbService.getAverageDeploymentTime();

        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();

        Linear linear =new Linear();
        while (requestIterator.hasNext()) {
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();
            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice = fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    double executionDelay_a_f = calculateExecutionDelayInFogDevice(request, fogdevice);
                    linear.add(executionDelay_a_f, decisionVariableX);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                double executionDelay_a_F = calculateExecutionDelayInControlNode(request);
                linear.add(executionDelay_a_F, decisionVariableY);
            }

            String decisionVariableZ = "z_" + "_" + appModuleName;
            double executionDelay_a_R = calculateExecutionDelayInCloud(request);
            linear.add(executionDelay_a_R, decisionVariableZ);
        }
        String decisionVariableN = "n";
        linear.add(movingAverage, decisionVariableN);

        problem.add(linear, Operator.LE, this.appDeadline-this.appDeploymentTime);

    }

    //each module can be assigned only 1 time
    private void addConstraint_7(Problem problem) {
        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();

        while (requestIterator.hasNext()) {
            Linear linear = new Linear();
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();
            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice = fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    linear.add(1, decisionVariableX);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                linear.add(1, decisionVariableY);
            }
            String decisionVariableZ = "z_" + "_" + appModuleName;
            linear.add(1, decisionVariableZ);
            String decisionVariableN = "n";
            linear.add(1, decisionVariableN);
            problem.add(linear, "=", 1);
        }

    }


    // make variables binary
    private void addConstraint_1(Problem problem) {
        Iterator<TaskRequest> requestIterator = requests.iterator();
        Iterator<Fogdevice> fogdeviceIterator = children.iterator();

        while (requestIterator.hasNext()) {
            TaskRequest request = requestIterator.next();
            String appModuleName = request.getId();
            String moduleType = request.getServiceType();
            while (fogdeviceIterator.hasNext()) {
                Fogdevice fogdevice = fogdeviceIterator.next();
                if (fogdevice.getServiceTypes().contains(moduleType)) {
                    String fogDeviceName = fogdevice.getId();
                    String decisionVariableX = "x_" + appModuleName + "_" + fogDeviceName;
                    Linear linear = new Linear();
                    linear.add(1, decisionVariableX);
                    problem.add(linear, Operator.LE, 1);
                    problem.add(linear, Operator.GE, 0);
                    problem.setVarType(decisionVariableX, VarType.INT);
                }
            }
            if (dbService.getServiceTypes().contains(moduleType)) {
                String decisionVariableY = "y_" + appModuleName;
                Linear linearY = new Linear();
                linearY.add(1, decisionVariableY);
                problem.add(linearY, Operator.LE, 1);
                problem.add(linearY, Operator.GE, 0);
                problem.setVarType(decisionVariableY, VarType.INT);
            }
            String decisionVariableZ = "z_" + "_" + appModuleName;
            Linear linearZ = new Linear();
            linearZ.add(1, decisionVariableZ);
            problem.add(linearZ, Operator.LE, 1);
            problem.add(linearZ, Operator.GE, 0);
            problem.setVarType(decisionVariableZ, VarType.INT);

        }
        Linear linearN = new Linear();
        String decisionVariableN = "n";
        linearN.add(1, decisionVariableN);
        problem.add(linearN, Operator.LE, 1);
        problem.add(linearN, Operator.GE, 0);
        problem.setVarType(decisionVariableN, VarType.INT);
    }

    private double calculateExecutionDelayInFogDevice(TaskRequest request, Fogdevice fogdevice) {
        return 0;
    }
    private double calculateExecutionDelayInControlNode(TaskRequest request) {
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

}

