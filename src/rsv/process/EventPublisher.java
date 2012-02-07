package rsv.process;


import org.apache.log4j.Logger;

import rsv.process.control.RSVMain;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class EventPublisher {
	static Logger logger = Logger.getLogger(EventPublisher.class);  
	
    public void publish(String routing_key, String msg) {
		try {
			//connect to rabbitmq server
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(RSVMain.conf.getProperty("rabbitmq.username"));
			factory.setPassword(RSVMain.conf.getProperty("rabbitmq.pass"));
			factory.setVirtualHost(RSVMain.conf.getProperty("rabbitmq.vhost"));
			factory.setHost(RSVMain.conf.getProperty("rabbitmq.host"));
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();
			
			//public message
			String exchange = RSVMain.conf.getProperty("rabbitmq.exchange");
	        channel.exchangeDeclare(exchange, "topic");
			channel.basicPublish(exchange, routing_key, null, msg.getBytes());
			
			//close it up
			channel.close();
			conn.close();
			
			logger.debug("posted to event server:" + msg);
		} catch (Exception e) {
			logger.error("Failed to publish event", e);
		}	
    }
}
