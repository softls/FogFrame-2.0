package at.ac.tuwien.infosys.cloud.impl;

import at.ac.tuwien.infosys.cloud.ICloudProviderService;
import at.ac.tuwien.infosys.model.DockerContainer;
import at.ac.tuwien.infosys.model.DockerHost;
import at.ac.tuwien.infosys.model.DockerImage;
import at.ac.tuwien.infosys.util.Constants;
import at.ac.tuwien.infosys.util.Utils;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.internal.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by olena on 8/10/17.
 */
@Primary
@Service("Aws")
@Slf4j
public class AwsService implements ICloudProviderService {

  //  @Value("${aws.default.image}")
    private String awsDefaultImageId;
  //  @Value("${aws.default.image.flavor}")
    private String awsDefaultImageFlavor;
  //  @Value("${aws.default.securitygroup}")
    private String awsDefaultSecuritygroup;
  //  @Value("${aws.keypair.name}")
    private String awsKeypairName;


    private AmazonEC2 amazonEC2Client;

    private HashSet<String> portSet = new HashSet<String>();


    @Override
    public void setup() {
        Properties prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream("cloud/credential.properties"));
        } catch (IOException e) {
            log.error("Could not load properties.", e);
        }
        String awsAccessKeyId = prop.getProperty("aws.access.key.id");
        String awsAccessKey = prop.getProperty("aws.access.key");
        String awsDefaultRegion = prop.getProperty("aws.default.region");
        awsDefaultImageId=prop.getProperty("aws.default.image");
        awsDefaultImageFlavor=prop.getProperty("aws.default.image.flavor");
        awsDefaultSecuritygroup=prop.getProperty("aws.default.securitygroup");
        awsKeypairName=prop.getProperty("aws.keypair.name");

        BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessKeyId, awsAccessKey);
        amazonEC2Client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(awsDefaultRegion)
                .build();

        System.out.println("Successfully connected to AWS with user " + awsAccessKeyId);
    }

    @Override
    public DockerHost startVM(DockerHost dh) throws Exception {
        setup();


        if (dh == null) {
            dh = new DockerHost(awsDefaultImageId);
            dh.setFlavor(awsDefaultImageFlavor);
        }

        String cloudInit = "";
        try {
            cloudInit = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("docker-config/cloud-init"), "UTF-8");
        } catch (IOException e) {
            System.out.println("Could not load cloud init file");
        }
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(awsDefaultImageId)
                .withInstanceType(awsDefaultImageFlavor)
                .withMinCount(1)
                .withMaxCount(1)
                .withUserData(org.glassfish.jersey.internal.util.Base64.encodeAsString(cloudInit))
                .withKeyName(awsKeypairName)
                .withSecurityGroups(awsDefaultSecuritygroup);

        RunInstancesResult run_response = amazonEC2Client.runInstances(runInstancesRequest);


        String instanceId = run_response.getReservation().getInstances().get(0).getInstanceId();
        Instance instance = getAwsInstance(instanceId);

        String allocationIp = instance.getPublicIpAddress();

        dh.setName(instance.getInstanceId());
        dh.setUrl(allocationIp);


        //wait until the dockerhost is available
        Boolean connection = false;
        while (!connection) {
            try {
                Thread.sleep(1000);
                final DockerClient docker = getDockerClient(dh.getUrl(), 60000);
                String ping = docker.ping();
                connection = true;
            } catch (DockerException ex) {
                System.out.println("Dockerhost is not available yet. "+ ex);
            } catch (InterruptedException e) {
                System.out.println("Dockerhost is not available yet. "+ e);
            }
        }

        System.out.println("Successfully associated Elastic IP address %s " + allocationIp + "with instance %s"  + instanceId);



        System.out.println("Server with id: " + dh.getName() + " and IP " + allocationIp + " was started.");

        return dh;

    }

    private DockerClient getDockerClient(String url, int connectTimeout){
        return DefaultDockerClient.builder().
                uri(URI.create("http://" + url + ":2375")).
                connectTimeoutMillis(connectTimeout).
                build();
    }

    @Override
    public void stopDockerHost(String name) {
        StopInstancesRequest request = new StopInstancesRequest()
                .withInstanceIds(name);
        amazonEC2Client.stopInstances(request);
    }

    @Override
    public DockerContainer startDockerContainer(String url, DockerImage image) {

        final DockerClient docker = getDockerClient(url);
        try {
            String serviceKey = image.getServiceKey();
            // tweak to get the cloud service pull the images
            if(!serviceKey.startsWith(Constants.IMAGE_PREFIX)) serviceKey = Constants.IMAGE_PREFIX+serviceKey;
            docker.pull(serviceKey);

            String[] ports = {Utils.generateRandomPort(portSet)};
            String[] exposedPorts = image.getExposedPorts();
            Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();
            for (String port : ports) {
                List<PortBinding> hostPorts = new ArrayList<PortBinding>();
                hostPorts.add(PortBinding.of("0.0.0.0", port));
                portBindings.put(image.getExposedPorts()[0], hostPorts);
            }

            final HostConfig expected = HostConfig.builder()
                    .portBindings(portBindings)
                    .privileged(image.isPrivileged())
                    .build();

            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(serviceKey)
                    .hostConfig(expected)
                    .exposedPorts(exposedPorts)
                    .build();

            String uuid = UUID.randomUUID().toString();
            String containerName = serviceKey.replace("/","-")+"_"+uuid;
            final ContainerCreation creation = docker.createContainer(containerConfig, containerName);
            final String id = creation.id();

            docker.startContainer(id);

            return new DockerContainer(id, serviceKey, Integer.valueOf(ports[0]), containerName, image);
        } catch (DockerException e) {
            log.error("Could not start container", e);
        } catch (InterruptedException e) {
            log.error("Could not start container", e);
        }
        return new DockerContainer();
    }

    private DockerClient getDockerClient(String url){
        return getDockerClient(url, 60000);
    }

    @Override
    public void stopDockerContainer(String url, String containerId) {

        final DockerClient docker = getDockerClient(url);
        log.info("Docker client: "+docker.toString());
        try {
            int count = 0;
            int maxTries = 10;
            while (true) {
                try {
                    log.info("Trying to kill container at url: "+url+" with Id: "+containerId);
                    docker.killContainer(containerId);
                    log.info("Killing container with Id: "+containerId);
                    break;
                } catch (InterruptedException | DockerException e) {
                    log.warn("Could not kill a docker container - trying again.", e);
                    if (++count == maxTries) throw e;
                }
            }
        } catch (DockerException | InterruptedException e) {
            log.error("Could not kill the container", e);
        }
    }

    private Instance getAwsInstance(String instanceId) {
        int counter = 0;

        while (counter <= 50) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                System.out.println("Exception "+ e);
            }

            DescribeInstancesRequest request = new DescribeInstancesRequest();
            DescribeInstancesResult response = amazonEC2Client.describeInstances(request);
            for (Reservation reservation : response.getReservations()) {
                for (Instance temp : reservation.getInstances()) {
                    if (temp.getInstanceId().equals(instanceId)) {
                        if(temp.getPublicIpAddress() != null && !temp.getPublicIpAddress().isEmpty()) {
                            return temp;
                        }
                        break;
                    }
                }
            }

            counter = counter + 1;
        }

        return null;

    }
}
