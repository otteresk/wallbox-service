package net.aahso.homehausen.wallbox_service;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
public class WebClientConfig {

	@Autowired
	private Environment env;
	

	@Bean
	public WebClient inverterWebClient(WebClient.Builder builder) {
		//System.out.println("Inverter API URL: "+env.getProperty("dailysun.inverter.apiurl"));
        return builder
            .baseUrl(env.getProperty("app.inverter.apiurl"))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
	}
	
	@Bean
	public WebClient aahsoWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl("https://aahso.net/o/pv")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
	}

	@Bean
	public String passwordFilename() {
		return env.getProperty("app.inverter.userpasswordfile");
	}

}
