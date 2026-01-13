package com.song.my_ipim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MyIpimApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyIpimApplication.class, args);
	}

}
