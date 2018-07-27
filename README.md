![FogFrame-2.0](https://github.com/softls/FogFrame-2.0/blob/master/logo_fogframe.png)

# FogFrame-2.0

FogFrame-2.0 provides a framework to create a testbed for fog computing based on Raspberry Pi computers.


The framework consists of the following components:

* Fog controller: responsible the service deployment in the cloud and the data propagation from fog devices to cloud services.
* Fog node (FN): Fog device that orchestrates subjacent fog colonies consisting of other fog nodes and fog cells.
* Fog cell (FC): Fog device that is connected to fog nodes and sensors\actuators. The main purpose of the fog cell is to execute services and send data to the parent fog node.
* Fog data: Consists of models and util classes shared amongst the other components.
* Hostmonitor: Monitoring applicatoin that monitors the host and sends the data to a corresponding Redis database for further processing. Has to be instantiated on every fog device.



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

1. To get the framework up and running, all Raspberrys need to be connected to the same WiFi network.
2. The different projects need to be built using mvn install.
3. After that, the resulting JAR files and the corresponding run scripts in the folder run_scripts can be transferred to Raspberry Pis.
If both required JAR files, which means component itself, i.e., fog node or fog cell, and the hostmonitor service, the Dockerfile, and the run.sh script are transferred successfully, the components can be started.
To start the different components the run.sh script needs to be executed.
`$ sh run.sh`
4. In case of cloud usage, the fog controller needs to be set up on a computer in the same network as the other fog devices.
5. In order to start the Fog Controller, a Redis database needs to be started using the startDB.sh script. Then the run.sh script needs to be executed.


#### Usage

When all devices are paired successfully, application requests can be sent to the fog control nodes.
Examples of evaluation scenarios are given in the requests folder.


#### License

Apache 2.0 © Olena Skarlat

Apache 2.0 © Kevin Bachmann


### Sources

Cloud graphic by Yannick from Flaticon is licensed under CC BY 3.0. Made with Logo Maker

