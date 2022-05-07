package vttp2022.project.addressprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AddressprocessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AddressprocessorApplication.class, args);
	}


}
