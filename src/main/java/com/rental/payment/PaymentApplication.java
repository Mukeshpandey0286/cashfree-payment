package com.rental.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) {

		// Load .env
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		// Set as system properties for Spring to use
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		SpringApplication.run(PaymentApplication.class, args);
	}

}
