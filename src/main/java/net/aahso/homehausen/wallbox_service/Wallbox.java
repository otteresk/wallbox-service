package net.aahso.homehausen.wallbox_service;

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

import com.fazecast.jSerialComm.SerialPort;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import jakarta.annotation.PreDestroy;

import se.cloudcharge.serialmanager.bus.Bus;
import se.cloudcharge.serialmanager.bus.ports.JSerialCommPort;
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
                        Thread.sleep(3000);

                        loopCount++;
                        System.out.println("Loop: " + loopCount);
                        
						String state = getState();
						System.out.println("Wallbox State: " + state);
		                long timeStampSeconds = Instant.now().getEpochSecond();
						WallBoxState wbs = new WallBoxState(timeStampSeconds, state);
						if (stateList.size() > 0) {
							if (!stateList.peekLast().getState().equals(state)) {
								stateList.add(wbs);
							}
						}
						else {
							stateList.add(wbs);
						}	
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

    private String getState() throws InterruptedException {
        //System.out.println("Reading state ...");
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
        System.out.println("Reading state failed!!!");
        return "XX";
    }

	// get latest WallBoxState
	public WallBoxState getLatestState() {
		return stateList.peekLast();
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

}
