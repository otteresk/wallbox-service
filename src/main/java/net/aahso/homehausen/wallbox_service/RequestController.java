package net.aahso.homehausen.wallbox_service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController // This means that this class is a REST Controller
@RequestMapping(path="/request") // This means URL's start with /request (after Application path)
public class RequestController {

	private final Wallbox wallbox;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public RequestController(Wallbox wb) {
        this.wallbox = wb;
        logger.info("Request Controller constructed!");
	}
	
    // Start Charging Process 
    @PostMapping(path="/charge")
    @ResponseBody
    public StatusLine startCharging( @RequestParam(name="current", required=true) int current) {

        if (current < 6 || current > 16) {
            logger.warn("Requested current " + current + "A is out of range (6-16A).");
            return new StatusLine(Instant.now().getEpochSecond(), "Bad Charge Request.");
        }

		StatusLine response = wallbox.startCharging(current);
        return response;
    }
	
    // STOP Charging Process 
    @PostMapping(path="/stop")
    @ResponseBody
    public StatusLine stopCharging( @RequestParam(name="confirm", required=true) String confirm) {

        if ( !confirm.equals("iamsure") ) {
            logger.warn("Bad Stop request.");
            return new StatusLine(Instant.now().getEpochSecond(), "Bad Stop Request.");
        }

		StatusLine response = wallbox.stopCharging();
        return response;
    }


}
