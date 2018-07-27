package at.ac.tuwien.infosys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by Kevin Bachmann on 14/11/2016.
 */
@SpringBootApplication
@EnableScheduling
public class FogController {

    public static void main(String[] args) {
        SpringApplication.run(FogController.class, args);
        System.out.println("\n" +
                "  ______           _____            _             _ _           \n" +
                " |  ____|         / ____|          | |           | | |          \n" +
                " | |__ ___   __ _| |     ___  _ __ | |_ _ __ ___ | | | ___ _ __ \n" +
                " |  __/ _ \\ / _` | |    / _ \\| '_ \\| __| '__/ _ \\| | |/ _ \\ '__|\n" +
                " | | | (_) | (_| | |___| (_) | | | | |_| | | (_) | | |  __/ |   \n" +
                " |_|  \\___/ \\__, |\\_____\\___/|_| |_|\\__|_|  \\___/|_|_|\\___|_|   \n" +
                "             __/ |                                              \n" +
                "            |___/                                               \n");


    }
}