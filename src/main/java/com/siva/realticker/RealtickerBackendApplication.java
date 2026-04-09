package com.siva.realticker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.siva.realticker.controller",
		"com.siva.realticker.service",
		"com.siva.realticker"
})
public class RealtickerBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(RealtickerBackendApplication.class, args);
	}
}