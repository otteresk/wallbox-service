package net.aahso.homehausen.wallbox_service;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController // This means that this class is a REST Controller
@RequestMapping(path="/data") // This means URL's start with /data (after Application path)
public class DataController {

	private final Wallbox wallbox;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public DataController(Wallbox wb) {
        this.wallbox = wb;
        logger.info("DataController constructed!");
	}
	
    ////////////////////////////////////////////////////////////////////////////
    // get latest data from wallbox 
    ////////////////////////////////////////////////////////////////////////////
    @GetMapping(path="/latest")
    @ResponseBody
    public DataPoint getLatestData() {

		// just to test
		DataPoint response = new DataPoint(42, 0, 0, 0, 0, 0);
        
        return response;
    }
	

}
