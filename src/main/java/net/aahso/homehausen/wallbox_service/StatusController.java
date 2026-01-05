package net.aahso.homehausen.wallbox_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController // This means that this class is a REST Controller
@RequestMapping(path="/status") // This means URL's start with /status (after Application path)
public class StatusController {

	private final Wallbox wallbox;
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public StatusController(Wallbox wb) {
        this.wallbox = wb;
        logger.info("Status Controller constructed!");
	}
	
    ////////////////////////////////////////////////////////////////////////////
    // get latest data from wallbox 
    ////////////////////////////////////////////////////////////////////////////
    @GetMapping(path="/latest")
    @ResponseBody
    public WallBoxState getLatestState() {
		WallBoxState response = wallbox.getLatestState();
        return response;
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // get status lines for charge request 
    ////////////////////////////////////////////////////////////////////////////
    @GetMapping(path="/charging")
    @ResponseBody
    public List<StatusLine> getStatusLines() {
        return wallbox.getStatusLines();
    }



}
