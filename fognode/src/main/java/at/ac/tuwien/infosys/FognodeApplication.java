package at.ac.tuwien.infosys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by Kevin Bachmann on 27/10/2016.
 */
@SpringBootApplication
@EnableScheduling
public class FognodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FognodeApplication.class, args);
		System.out.println("\n" +
				"  ______          _   _           _      \n" +
				" |  ____|        | \\ | |         | |     \n" +
				" | |__ ___   __ _|  \\| | ___   __| | ___ \n" +
				" |  __/ _ \\ / _` | . ` |/ _ \\ / _` |/ _ \\\n" +
				" | | | (_) | (_| | |\\  | (_) | (_| |  __/\n" +
				" |_|  \\___/ \\__, |_| \\_|\\___/ \\__,_|\\___|\n" +
				"             __/ |                       \n" +
				"            |___/                        \n");
	}
}
