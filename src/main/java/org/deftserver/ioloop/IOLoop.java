package org.deftserver.ioloop;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOLoop {
	
	private final static Logger logger = LoggerFactory.getLogger(IOLoop.class);
	private final static IOLoop instance = new IOLoop();
	private Selector selector;
	

	private IOLoop() {
		try {
			selector = Selector.open();
		} catch (IOException e) {
			logger.error("Error opening selector: {}", e);
		}
	}
	
	public static IOLoop getInstance() {
		return instance;
	}

	public void start() {
		Thread.currentThread().setName("I/O-LOOP");
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

		while (true) {
			try {
				int numSelected = selector.select();
				logger.debug("Selected: {}", numSelected);
				
				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					EventHandler handler = (EventHandler) key.attachment();
					handler.handleEvents(key);
					keys.remove();
				}
			} catch (IOException e) {
				logger.error("Exception received in IOLoop: {}", e);
			}
		}
	}

	public void addHandler(SelectableChannel channel, EventHandler handler, int ops) {
		logger.info("Adding handler for {}", channel);
		try {
			channel.register(selector, ops);
			channel.keyFor(selector).attach(handler);
		} catch (ClosedChannelException e) {
			logger.error("Could not register selector: {}", e);
		}
	}
}
