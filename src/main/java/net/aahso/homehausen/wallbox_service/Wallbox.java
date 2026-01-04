package net.aahso.homehausen.wallbox_service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import jakarta.annotation.PreDestroy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;

import se.cloudcharge.serialmanager.bus.Bus;
import se.cloudcharge.serialmanager.bus.ports.JSerialCommPort;
import se.cloudcharge.serialmanager.command.QueryResponse;
import se.cloudcharge.serialmanager.command.BaseCommand;
import se.cloudcharge.serialmanager.command.Command;
import se.cloudcharge.serialmanager.command.CommandEnum;
import se.cloudcharge.serialmanager.command.response.RegexpResponseSpec;
import se.cloudcharge.serialmanager.command.response.ResponseSpec;
import se.cloudcharge.serialmanager.exceptions.PortInUseException;
import se.cloudcharge.serialmanager.exceptions.PortNotAvailableException;
import se.cloudcharge.serialmanager.exceptions.RFIDOnBusException;
import se.cloudcharge.serialmanager.model.Device;
import se.cloudcharge.serialmanager.model.evcc.EVCC2;
import se.cloudcharge.serialmanager.model.evcc.State;


@Component
public class Wallbox {

    private final int CALL_INIT    = 0;
    private final int CALL_RUNNING = 1;
    private final int CALL_READY   = 2;
    private final int CALL_ERROR   = 3;

	@Autowired
	private Environment env;

	// injected
    private final TaskExecutor taskExecutor;

	private String initStatus = "NONE";
    private Bus myBus = null;
    private EVCC2 myevcc2 = null;
    private State box_state = null;
    private int address = -1;
    private int state_call_status = CALL_INIT;
    private int bool_call_status = CALL_INIT;
    private Boolean bool_return_value = false;

	private volatile boolean running = true;
    private volatile Thread workerThread;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
	private LinkedList<WallBoxState> stateList = new LinkedList<WallBoxState>();

	// Constructor for Wallbox
	public Wallbox( TaskExecutor taskExecutor ) {
        this.taskExecutor = taskExecutor;

        try {
			init();
        }
        catch (PortInUseException piue) {
			initStatus = "PORT_IN_USE";
			logger.error("Error: Serial port in use. Stopping." );
		}
		catch (Exception e) {
			initStatus = "INIT_ERROR";
			e.printStackTrace();
        }

		if (initStatus.equals("OK")) logger.info("Wallbox successfully initialized!");
	}

	// MAIN DATA PUMP LOOP
    @EventListener(ApplicationReadyEvent.class)
    public void runDataPump(ApplicationReadyEvent ev) {
        taskExecutor.execute(() -> {
            workerThread = Thread.currentThread();
		    int loopCount = 0;

			try {
                while (running && initStatus.equals("OK") && loopCount > -3) {
                    try {

						// start with some sleep
                        Thread.sleep(2500);

                        loopCount++;
                        System.out.println("Loop: " + loopCount);
                        
						String state = getState();
						System.out.println("Wallbox State: " + state);
                        long timeStampSeconds = Instant.now().getEpochSecond();

                        // add to state list
                        addToList(state, timeStampSeconds);

                        // write state to file
                        saveStateToFile(stateList.peekLast());
                        
						System.out.println("State List size: " + stateList.size());

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

    //////////////////
    ///    INIT    ///
    //////////////////

	private void init() throws PortInUseException, PortNotAvailableException, RFIDOnBusException {

        SerialPort port=null;
        
        SerialPort serPorts[] = SerialPort.getCommPorts();
        logger.info("I found "+serPorts.length+" serial ports");

		for (int i=0; i<serPorts.length; i++) {
        
            port = serPorts[i];
            String sysname = port.getSystemPortName();
            logger.info("Found: " + sysname );
            
            if (sysname.startsWith("ttyUSB")) {
                logger.info("- will use it" );
                //System.out.println("Name:        " + port.getDescriptivePortName() );
                //System.out.println("Description: " + port.getPortDescription() );
                //System.out.println("BaudRate:    " + port.getBaudRate() );
                break;
            }
            else {
                port = null;
            }
        }
        if (port==null) {
            logger.error("No appropriate serial port found. Stopping." );
			initStatus = "NO_SERIAL_PORT";
			return;    
        }

		JSerialCommPort commPort = new JSerialCommPort(port);
		//System.out.println("Comm port: " + commPort.getName() );
		myBus = new Bus(commPort);
		//System.out.println("Bus: " + myBus.getBusName() );

        myevcc2 = new EVCC2(myBus, 0);
        logger.info("EVCC2: "+myevcc2.toString());

        // find Device (=Wallbox)
        //logger.info("Scanning bus for devices ...");
        final ArrayList<Device> devices = new ArrayList<Device>();
        for (int i=1;i<=5;i++) {
	        logger.info("Scanning bus for devices. Attempt: "+i+" ...");
			devices.addAll(myevcc2.scan());
			if (devices.size()>0) break;
		}
        logger.info("Number of devices: " + devices.size() );
        for (Device dev : devices) {
            logger.info("- Name of Device: "+dev.getName() + " Address: "+dev.getAddress() );
            address = dev.getAddress();
            myevcc2 = new EVCC2(myBus, address);
        }

        if (devices.size() == 0) {
			initStatus = "NO_DEVICE_FOUND";
            logger.error("No device found. Stopping." );
            return;
        }

		initStatus = "OK";

	}
	
    //////////////////
    /// READ STATE ///
    //////////////////

    private String getState() {
        //System.out.println("Reading state ...");
		try {
            while(state_call_status == CALL_RUNNING) Thread.sleep(100); // wait if previous call is still running
            final int MAX_TRIES = 3; 
            for (int tries = 1; tries <= MAX_TRIES; tries++) {
                myevcc2.getState().subscribe(state_observer);
                while(state_call_status == CALL_RUNNING) Thread.sleep(50);
                if (state_call_status == CALL_ERROR) {
                    logger.warn("Error reading state - Retrying");
                    Thread.sleep(1*1000);
                }
                else {
                    return box_state.getState();
                }
            }
        } catch (InterruptedException e) {
            logger.error("Interrupt in getState.");
        } catch (Exception e) {
            logger.error("Exception in getState: " + e.getMessage());
        }
        System.out.println("Reading state failed!");
        return "XX";
    }

    //////////////////////
    /// START CHARGING ///
    //////////////////////

    public StatusLine startCharging(int current) {
        if (current < 6 || current > 16) {
            logger.warn("Requested current " + current + "A is out of range (6-16A).");
            return new StatusLine(Instant.now().getEpochSecond(), "Bad Charge Request.");
        }

        String state = null;
        Boolean response = false;
        final int[] TRIES = {5,9};
        int LOOP_END = 2;
        for (int loop=1; loop<=LOOP_END; loop++) {
            // try to set current directly (if state is C2, this will work)
            response = setCurrent(current);
            // !!!! process reposnse

            // check state is C2
            System.out.println("Check if status turns to C2");
            for (int tries=1; tries<=TRIES[loop-1]; tries++) {
                state = getState();
                System.out.println("State: "+state);
                if (state.equals("C2")) break; 
                // sleep and try again
                try { Thread.sleep(7000); }
                catch (Exception e) { e.printStackTrace(); }
            }
            
            if (state.equals("C2")) break; 
            if (loop==LOOP_END) break;
            response = resetWallbox();
            // no matter what the response is, wait a bit (reset never fails)
            try { Thread.sleep(6000); }
            catch (Exception e) { e.printStackTrace(); }

        }

        return new StatusLine(7, "response: "+response+"; final state: "+state);
    }

    //////////////////////
    /// STOP CHARGING  ///
    //////////////////////
    // use copied setImax to set current below 6A. This will stop charging

    public StatusLine stopCharging() {

        final int MAX_TRIES = 3;
        System.out.println("Stopping charging ..."); 
        
        int pwm = 60; // = OFF!
        ResponseSpec dummyMatch = new RegexpResponseSpec("[" + EVCC2.R_PREFIX + "]\\w*");
        Command command = new BaseCommand(CommandEnum.EVCC2_SET_IMAX.write(address, pwm),
                                            EVCC2.Q_PREFIX, EVCC2.R_PREFIX, 1, dummyMatch);
        for (int tries = 1; tries <= MAX_TRIES; tries++) {
            try {            
                QueryResponse response = myBus.query(command, EVCC2.evcc2BusOptions);
                if (response.getCode().equals(QueryResponse.ResponseCode.OK)) {
                    System.out.println("Stop request successful.");
                    return new StatusLine(Instant.now().getEpochSecond(), "Stop request successful.");
                }
                else {
                    System.out.println("stop request failed - Retrying");
                    Thread.sleep(2*1000);
                }
            } catch (Exception e) {
                System.out.println("Exception in stopCharging: " + e.getMessage());
                if (tries<MAX_TRIES) System.out.println("Retrying...");
            }
        }
        System.out.println("stop request failed!!!");
        return new StatusLine(Instant.now().getEpochSecond(), "stop request failed!!!");
    }


    /////////////////
    // SET CURRENT //
    /////////////////

    private Boolean setCurrent(int current) {
        final int MAX_TRIES = 3;
        System.out.println("Setting current to " + current + " ..."); 

        try {
            while(bool_call_status == CALL_RUNNING) Thread.sleep(100); // wait if previous call is still running

            for (int tries = 1; tries <= MAX_TRIES; tries++) {
                myevcc2.setIMax(current).subscribe(bool_observer);
                while(bool_call_status == CALL_RUNNING) Thread.sleep(20);
                if (bool_call_status == CALL_ERROR) {
                    System.out.println("setting current failed - Retrying");
                    Thread.sleep(1000);
                }
                else {
                    System.out.println("Successfully set current to: " + current);
                    return bool_return_value;
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupt in setCurrent.");
        } catch (Exception e) {
            System.out.println("Exception in setCurrent: " + e.getMessage());
        }
        System.out.println("setting current failed!!!");
        return false;
    }

    /////////////////
    /// RESET BOX ///
    /////////////////
        
    private Boolean resetWallbox() {

        System.out.println("Resetting...");
        try {
            while(bool_call_status == CALL_RUNNING) Thread.sleep(100); // wait if previous call is still running

            myevcc2.modifyState(State.MOD_RESET).subscribe(bool_observer);
            while(bool_call_status == CALL_RUNNING) Thread.sleep(50);
            //System.out.println(bool_call_status);
            if (bool_call_status == CALL_ERROR) {
                System.out.println("Error in Resetting - we don't care");
                return false;
            }
            else {
                return true;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Resetting failed!!!");
        
        return false;

    }

	// get latest WallBoxState
	public WallBoxState getLatestState() {
		return stateList.peekLast();
	}

    // add to state list and clean up old list entries
    private void addToList(String state, long timeStampSeconds) {
        if (stateList.size() == 0) {
            stateList.add(new WallBoxState(timeStampSeconds, state));
            return;
        }        
        WallBoxState lastwbs = stateList.peekLast();
        if (lastwbs.getState().equals(state)) {
            lastwbs.setTimeStamp(timeStampSeconds);
        }
        else {
            stateList.add(new WallBoxState(timeStampSeconds, state));
        }
        
        // remove old data points (older than 120 minutes)
        if (stateList.size() > 10) {
            System.out.println("Cleaning up old state entries...");
            stateList.removeIf((WallBoxState w) -> w.getTimeStamp() < Instant.now().getEpochSecond() - 2*60*60);
        }
    }

    // save data point to file
    private void saveStateToFile(WallBoxState wbs) {
        ObjectMapper objectMapper = new ObjectMapper();
        String saveFilename = env.getProperty("app.savefilename");

        try (FileWriter myWriter = new FileWriter(saveFilename, true)) {
            myWriter.write(objectMapper.writeValueAsString(wbs)+"\n");
        } catch (IOException e) {
            System.out.println("Error writing to file.");
            e.printStackTrace();
        }
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

    /// Observer for State
    private Observer<State> state_observer = new Observer<State>() {
        @Override
        public void onSubscribe(final Disposable disposable) {
            state_call_status = CALL_RUNNING;
            // System.out.println("State.onSubscribe");
        }
        @Override
        public void onNext(State s) {
            // System.out.println("State.onNext");
            box_state = s;
            // System.out.println(s.getState());
        }
        @Override
        public void onComplete() {
            state_call_status = CALL_READY;
            //System.out.println("State.onComplete");
        }
        @Override
        public void onError(final Throwable e) {
            state_call_status = CALL_ERROR;
            //System.out.println("State.onError");
        }
    };
    /// (end) Observer for State

    /// Observer for Boolean
    private Observer<Boolean> bool_observer = new Observer<Boolean>() {
        @Override
        public void onSubscribe(final Disposable disposable) {
            bool_call_status = CALL_RUNNING;
            // System.out.println("Boolean.onSubscribe");
        }
        @Override
        public void onNext(Boolean b) {
            bool_return_value = b;
            // System.out.println("Boolean.onNext");
            // System.out.println(b);
        }
        @Override
        public void onComplete() {
            bool_call_status = CALL_READY;
            //System.out.println("Boolean.onComplete");
        }
        @Override
        public void onError(final Throwable e) {
            bool_call_status = CALL_ERROR;
            //System.out.println("Boolean.onError");
        }
    };
    /// (end) Observer for Boolean

}
