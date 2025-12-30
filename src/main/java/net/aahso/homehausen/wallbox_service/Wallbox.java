package net.aahso.homehausen.wallbox_service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PreDestroy;

import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import reactor.core.publisher.Mono;

@Component
public class Wallbox {


	@Autowired
	private Environment env;

	// injected
    private final TaskExecutor taskExecutor;

    private volatile boolean running = true;
    private volatile Thread workerThread;
	private ObjectMapper objectMapper = new ObjectMapper();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
	// Constructor for Wallbox
	public Wallbox( TaskExecutor taskExecutor ) {
        this.taskExecutor = taskExecutor;

		// connect !!!

		logger.info("Wallbox successfully constructed!");
	}

    @EventListener(ApplicationReadyEvent.class)
    public void runDataPump(ApplicationReadyEvent ev) {
        taskExecutor.execute(() -> {
            workerThread = Thread.currentThread();
		    int loopCount = 0;
			String responseJson = "";
            try {
                while (running && loopCount > -3) {
                    try {

						// start with some sleep
                        Thread.sleep(3000);

                        loopCount++;
                        System.out.println("Loop: " + loopCount);
                        

                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                    } catch (Exception e) {
                        // Log error but keep running
                        System.err.println("Error in Wallbox Data Pump: " + e.getMessage());
                    }
                }
            } finally {
                workerThread = null;
            }
        });
    }





	// clean shut down
    @PreDestroy
    private void stopThread() {
        logger.info("Shutdown requested: stopping Wallbox data pump");
        running = false;
        Thread t = workerThread;
        if (t != null) {
            t.interrupt();
            try {
                t.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


}
