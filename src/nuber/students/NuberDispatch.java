package nuber.students;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;


/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking
	 */
	private final int MAX_DRIVERS = 999;

	private boolean logEvents = false;

	// Thread-safe queue to store available drivers
	private final BlockingQueue<Driver> driverQueue;

	// Count of pending bookings
	private int pendingBookings; 
	
	 // Incremental ID for each booking
	private int bookingID;

	// Map to store different regions
	private final HashMap<String, NuberRegion> regions;

	/**
	 * Creates a new dispatch objects and instantiates the required regions and any
	 * other objects required. It should be able to handle a variable number of
	 * regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they
	 *                   can handle
	 * @param logEvents  Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents) {
		this.logEvents = logEvents;
		this.driverQueue = new ArrayBlockingQueue<>(MAX_DRIVERS);
		this.pendingBookings = 0;
        this.bookingID = 1;
		this.regions = new HashMap<>();

		System.out.println("Creating Nuber Dispatch");
		System.out.println("Creating " + regionInfo.size() + " regions");

		// Create NuberRegion objects for each region
		for (String regionName : regionInfo.keySet()) {
			int maxSimultaneousBookings = regionInfo.get(regionName);
			System.out.println("Creating Nuber region for " + regionName);
			regions.put(regionName, new NuberRegion(this, regionName, maxSimultaneousBookings));
		}

		System.out.println("Done creating " + regionInfo.size() + " regions");
	}

	/**
	 * Adds drivers to a queue of idle driver.
	 * 
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver) {
		try {
            driverQueue.put(newDriver);  // Add driver to the blocking queue
            return true;
        } catch (InterruptedException e) {
        	 System.err.println("Failed to add driver to queue");
            e.printStackTrace();
            return false;
        }
	}

	/**
	 * Gets a driver from the front of the queue
	 * 
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver() {
		 try {
	            Driver driver = driverQueue.take();  // Block until a driver is available
	            pendingBookings--;  // Decrement pending bookings as driver is assigned
	            return driver;
	        } catch (InterruptedException e) {
	            System.err.println("Failed to retrieve a driver.");
	            e.printStackTrace();
	            return null;
	        }
	}

	/**
	 * Prints out the string booking + ": " + message to the standard output only if
	 * the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {

		if (!logEvents)
			return;

		System.out.println(booking + ": " + message);

	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be
	 * returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and
	 * null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region    The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		NuberRegion nuberRegion = regions.get(region);
		 if (region == null) {
	            System.err.println("Invalid region: " + region);
	            return null;
	        }

	        Future<BookingResult> result = nuberRegion.bookPassenger(passenger);
	        if (result != null) {
	            pendingBookings++;  // Increment pending bookings if successfully booked
	            bookingID++;  // Generate a new booking ID
	        }

	        return result;
	}

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from
	 * dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be
	 * reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver() {
		return pendingBookings;
	}

	/**
	 * Tells all regions to finish existing bookings already allocated, and stop
	 * accepting new bookings
	 */
	public void shutdown() {
		for (NuberRegion region : regions.values()) {
			region.shutdown();
		}
	}

}
