# Description

This event counter keeps track when the event happened in last window of time. The window is defined in seconds with default set to 300 (5 minutes). The higher the window the higher time is needed to count events. To signal the event has occurred user can can call method logEvent. To count the events happened user can call method countEvents(). This method takes a parameter as seconds to count event happened since that time and now. 


This event counter is designed to count up to 2 million events per second. Any calls to logEvent above that limit will result is incorrect count. If countEvent is called with value less than 0 it returns 0. If it is called value more window then only events happened in entire window of time are returned. 

This count does maintain a state internally with background threads. Hence it needs open and close method. Before using any of above methods user needs to call open method. To release resources user needs to call close method. If any of the method called before calling open or after calling close may result in runtime exception. 

## Example:

<pre>
	EventCounter counter = new EventCounter(); //Creates counter with 5 minute window. 
	counter.open(); // Initialize counter
	counter.logEvent(); // Signal event happened now
	counter.countEvent(30); // Count events happened in last 30 seconds 
	counter.close(); // Free resources
</pre>

# Requirements 
- git version 2.15.0
- openjdk 13.0.1 2019-10-15
- OpenJDK Runtime Environment (build 13.0.1+9)
- OpenJDK 64-Bit Server VM (build 13.0.1+9, mixed mode, sharing)
- Apache Maven 3.6.2 


# Building 
- Clone the event-counter project from remote git repository 
- cd into event-counter directory
- Run following command to build the project 
-- man clean install -DskipTests 
- The jar will be generated at event-counter/target/event-counter-0.0.1-SNAPSHOT.jar that can be loaded into your project
