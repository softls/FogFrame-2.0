![FogFrame-2.0](https://github.com/softls/FogFrame-2.0/blob/master/logo_fogframe.png)

# FogFrame-2.0

FogFrame-2.0 provides a framework to create a testbed for fog computing based on Raspberry Pi computers.


The framework consists of the following components:

* Fog controller: responsible for establishing network topology, service deployment in the cloud and the data propagation from fog devices to cloud services.
* Fog node (FN): Fog device that orchestrates subjacent fog colonies consisting of other fog nodes and fog cells.
* Fog cell (FC): Fog device that is connected to fog nodes and sensors\actuators. The main purpose of the fog cell is to execute services and send data to the parent fog node.
* Fog data: Consists of models and util classes shared amongst the other components.
* Host monitor: Monitoring applicatoin that monitors the host and sends the data to a corresponding Redis database for further processing. It is instantiated on every fog device.

You may find additional folders in this repo:
* Folder requets with examples of scenarios to be executed by the framework. Also it contains scripts to generate failures and simulate overloads of devices. scripts for manual pairing for devices, registering service docker images etc.
* Folder run_scripts with the convenient scripts to start and stop all devices. This folder contains subfolders of according component fogcell, fognode. Also it contains scripts to generate failures of devices. 
* In the root folder there are also convenient scripts to update the JARs on all devices based on their IP addresses, either on fog nodes or fog cells, or all the devices at once.

Since the first version (https://github.com/keyban/fogframe), components has changed according to necessary functionalities, i.e., multiple colonies have been enabled, AWS cloud integration have been implemented, different resource provisioning mechanisms have been introduced.


#### Software Specifications:

FogFrame-2.0 testbed was evaluated with the following software versions:

- Docker 17.0.6-ce
- Java 8
- Apache Maven 3.2.3
- OpenStack Cloud (Keystone v2.0, Nova)
- AWS Cloud (AWS Java SDK 1.11.106)
- Hypriot OS (preinstalled on the Raspberry Pis)


#### Hardware Specifications:

- IBM X1 Carbon, (8GB RAM, Intel CORE i5) (Fog Controller was running on a VMware VM Ubuntu 16.04 64-bit, 4GB RAM)
- 1x Linksys Access Point
- 6x Raspberry Pi 3
- 3x Groove Pis with humidity and temperature sensors attached


#### Installation Guidelines:

1. To get the framework up and running, all Raspberry Pis and the device where you will be running the fog controller need 
to be connected to the same network. 
I use Raspberry Pis for fog nodes and fog cells, and the fog controller is running in a docker container on my personal notebook. 
It will help to make DHCP reservation of network IP addresses for all the devices for experiments for convenience.
2. It is necessary to establish SSH communication between your main computer from which you do all the configurations and the devices. 
This step is not explained, this information can be found on the Internet. 
It would be beneficial to read through the configuration details described in this thesis: https://www.dsg.tuwien.ac.at/team/sschulte/theses/Bachmann_Master.pdf
3. In the fogdata components in src/main/java/at.ac.tuwien.infosys.util there is Constants file where it is needed to 
write all the ip addresses that will be used in the framework. 
4. The different components need to be built using mvn install. Or the whole project FogFrame-2.0 has to be built at once. 
For that navigate to the folder FogFrame-2.0 and execute mvn clean install. 
Please note, that if you plan to start the separate components as docker containers, you need to set a flag in application.properties
of according device to TRUE, and then execute mvn install. If the flag is set to FALSE then the component can be started without a docker 
container, for example on your computer. Also, each component has main.properties file, which has to contain information about 
the device's IP address, port, parent devices, location coordinates. This file need to be modified up according to your network
 and desired topology for each device. This file in located in according component folders in src/main/resources.
5. After that, the resulting JAR files together with the Dockerfile, run scripts and main.properties have to be transferred to Raspberry Pis. Those files are summarized in the folder run_scripts. 
Make sure that the properties of the each fog cell or fog node are correct, i.e., modify the properties filewith actual ip addresses and intended coordinates of devices.
On each device you will have to copy JAR file from the target folder of according component, JAR file of the hostmonitor service, the Dockerfile, main.properties file, and scripts from the run_scripts folder. When all the files are copied to coorresponding devices, you set up all the parameters of the main.properties on each device, components can be started.
6. The fog controller needs to be set up on a computer or a raspberry pi in the same network as the other fog devices.
7. From all the components, the first one you need to start is Fog Controller. In order to start the Fog Controller, first a Redis database needs to be started using the startDB.sh script. Then the run.sh script needs to be executed. It is also beneficial to start a script and redicrect the stdout to a file, for example, fogcontroller.log for further analysis. 
In case you have troubles to remove ANSI colored output from the terminal into a readable file, this command will help.
8. In order to be able to connect to AWS, it is necessary to fill in credential.properties file in the focontroller/src/main/resources/cloud
9. To start a cloud service which will write sensor data into a database, it is needed to run a script located in requests/paper_eval/start-cloud-service.sh 
Make sure that you modify that script with the correct ip address of the device where your fog controller is running. It is also necessary to have a docker hub account and login into it in the browser before executing this script. This will enable to pull the necessary docker image on the cloud machine.
If you want to execute any scenario, it is necessary to start this cloud service before starting execution scenario. In the case when fog colonies want to delegate service execution to the cloud, the fog controller will request different VMs and deploy there necessary docker containers.
10. When Fog Controller is started, fog nodes can be started next and then fog cells. It is important to start them in this order for correct parent-children connection. To start the different components the run.sh script needs to be executed. There is a script in the folder run_scripts called start-all-raspberries.sh that shows how to start components on devices all at once.
11. Each component has a dedicated webpage, which can be reached by the ip address of the corresponding device and its port, for example, http://192.168.1.101:8080. Each components listen on different ports, therefore if to choose to start them all at once on single device, there will be no collision in ports.
12. During the experiments, if you want to change the used resource provisitioning method, 
you will need to open a project, and set annotation @Primary in the according class in the fog node: 
src/main/java/at.ac.tuwien.infosys.reasoner.resourceProvisioning.impl and then rebuild the fog node component with mvn clean install and transfer a new JAR to the Raspberry Pis of your fog nodes. You may want to implement new resource provisioning methods in the framework by implementing according abstract class IResourceProvisioning, and use it in experiments. 
13. Similarly, you may change the connection to the cloud provider. For that, in the fog controller component in the 
src/main/java/at.ac.tuwien.infosys.cloud.impl set the annotation @Primary to the neccessary API implementation. Currently AwsService is a primary implementation. OpenStack service is available in the framework as well.
14. When all devices are paired successfully, application requests can be sent to the fog nodes from the folder 
requests/paper-eval, where the examples of evaluation scenarios are given. 
You will need to modify scripts with your IP adresses of the fog node where you want to send the applications.
15. The API endpoints for submitting applications can be found in the ReasonerController class of the fog node component in src/main/java/at.ac.tuwien.infosys.reasoner 


#### License

Apache 2.0 © Olena Skarlat

Apache 2.0 © Kevin Bachmann


### Sources

Cloud graphic by Yannick from Flaticon is licensed under CC BY 3.0. Made with Logo Maker

