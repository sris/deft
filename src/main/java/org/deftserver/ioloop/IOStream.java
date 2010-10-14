package org.deftserver.ioloop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOStream implements EventHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(IOStream.class);
	
	private SocketChannel channel;
	private IOLoop ioloop;
	private ByteBuffer buffer;
	private StringBuilder readBuffer;
	private String readDelimiter;
	private int lastLoc;
	private AsyncCallback<String> callback;
	
	public IOStream(SocketChannel channel) {
		this.channel = channel;
		try {
			this.channel.configureBlocking(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.ioloop = IOLoop.getInstance();
		this.buffer = ByteBuffer.allocate(1500);
		this.readBuffer = new StringBuilder();
		this.readDelimiter = null;
		this.lastLoc = 0;
		this.ioloop.addHandler(this.channel, this, SelectionKey.OP_READ);
	}

	public void readUntil(String delimiter, AsyncCallback<String> callback) {
//	        _check_closed()
//	        _add_io_state(self.io_loop.READ)
		int loc = readBuffer.indexOf(delimiter);
		if (loc >= 0) {
			callback.onSuccess(consume(loc + delimiter.length()));
			return;
		}
		readDelimiter = delimiter;
		lastLoc = readBuffer.length();
		this.callback = callback;
	}

	@Override
	public void handleEvents(SelectionKey key) {
		if (key.isReadable()) {
			handleRead();
		}
	}

	private void handleRead() {
		try {
			buffer.clear();
			int bytesRead = channel.read(buffer);
			if (bytesRead <= 0) {
				logger.debug("Read zero length");
				close();
			}
			logger.debug("Buffer: {}", buffer.array());
			readBuffer.append(new String(buffer.array()));
			if (readDelimiter != null) {
				int loc = readBuffer.indexOf(readDelimiter, lastLoc);
				if (loc >= 0) {
					AsyncCallback<String> cb = callback;
					int delimiterLength = readDelimiter.length();
					callback = null;
					readDelimiter = null;
					lastLoc = 0;
					cb.onSuccess(consume(loc + delimiterLength));
				}
				lastLoc = readBuffer.length();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		
	}
	
	private String consume(int loc) {
		String result = readBuffer.substring(0, loc);
		readBuffer.delete(0, loc);
		return result;
	}
}
