package org.deftserver.ioloop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sris
 *
 */
public class IOStream implements EventHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(IOStream.class);
	
	private IOLoop ioloop;
	private SocketChannel channel;

	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	
	// TODO: Make delimiter/tokenizer purley byte oriented.
	// TODO: Investigate proper method for byte stream buffering.
	private StringBuilder readBytes;
	private String readDelimiter;
	private int lastLoc;
	
	private AsyncCallback<String> callback;
	private int ioOps;
	
	public IOStream(SocketChannel channel) {
		this.channel = channel;
		try {
			this.channel.configureBlocking(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.ioloop = IOLoop.getInstance();
		// TODO: Use a proper buffer size
		this.readBuffer = ByteBuffer.allocate(10);
		this.readBytes = new StringBuilder();
		this.readDelimiter = null;
		this.lastLoc = 0;
		this.ioOps = SelectionKey.OP_READ;
		this.ioloop.addHandler(this.channel, this, SelectionKey.OP_READ);
	}

	// TODO: Should this return byte[] or ByteBuffer. cmp. write()
	public void readUntil(byte[] delimiter, AsyncCallback<String> callback) throws IOException {
		readDelimiter = new String(delimiter);
		int loc = readBytes.indexOf(readDelimiter);
		if (loc >= 0) {
			callback.onSuccess(consume(loc + readDelimiter.length()));
			return;
		}
		// TODO: Need to check if socket closed?
		checkClosed();
		
		lastLoc = readBytes.length();
		this.callback = callback;
		addOps(SelectionKey.OP_READ);
	}

	// TODO: Implement byte mode.
	// TODO: Maybe switch to direct buffers when reading large files
	//		 i.e. image uploads
	public void readBytes(int numBytes, AsyncCallback<String> callback) {
		
	}
	
	/**
	 * Asynchronously writes data the channel of this IOStream
	 * 
	 * Changes in data before callback has been invoked might
	 * be reflected in the data written
	 * 
	 * TODO: Add callback/readyness notification
	 * 
	 * @param data
	 */
	public void write(ByteBuffer data) {
		// TODO: Need callback?
		// TODO: Need to check if socket closed?
		writeBuffer = data;
		addOps(SelectionKey.OP_WRITE);
	}
	
	@Override
	public void handleEvents(SelectionKey key) {
		// Handle read
		if (key.isReadable()) {
			handleRead();
		}
		// Check if open, read might have detected EOF
		if (isClosed()) {
			return;
		}
		// Handle write
		if (key.isWritable()) {
			handleWrite();
		}

		// Check if still reading
		// This is sorta weird
		// TODO: Investigate how to detect HUP etc. on idle connection.
		int ops = SelectionKey.OP_READ;
		if (isReading()) {
			ops |= SelectionKey.OP_READ;
		}
		
		// Check if there's more to write
		if (isWriting()) {
			ops |= SelectionKey.OP_WRITE;
		}
		// TODO: Check if ioOps changed
		if (ops != ioOps) {
			ioOps = ops;
			ioloop.updateHandler(channel, ioOps);
		}
	}

	private void handleRead() {
		// TODO: Investigate if it's a problem that data will be read even though nobody wants it
		//       Will it ever happen. i.e. neither read_x is invoked but stream is open.
		
		try {
			readBuffer.clear();
			int bytesRead = channel.read(readBuffer);
			if (bytesRead <= 0) {
				logger.debug("Read subzero length: {}", bytesRead);
				close();
				return;
			}
			
			readBuffer.flip();
			byte[] data = new byte[readBuffer.remaining()];
			readBuffer.get(data);
			readBytes.append(new String(data));
			if (isReadingUntil()) {
				searchForDelimiter();
			}
		} catch (IOException e) {
			logger.debug("Error on channel {}: {}", channel, e);
		}
	}
	
	public void handleWrite() {
		// TODO: Handle not being able to write entire buffer.
		try {
			int bytesWritten = channel.write(writeBuffer);
			if (bytesWritten <= 0) {
				logger.debug("Wrote subzero length: {}", bytesWritten);
				close();
			} else {
				//writeBuffer.position(writeBuffer.position() + bytesWritten);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void addOps(int ops) {
		logger.debug("Adding ops: old: {} new: {}", ioOps, ops);
		logger.debug("(ioOps & ops): {}", (ioOps & ops));
		if ((ioOps & ops) == 0) {
			logger.debug("Adding ops: {}", ops);
			ioOps |= ops;
			ioloop.updateHandler(channel, ioOps);
		}
	}
	
	private void searchForDelimiter() {
		int loc = readBytes.indexOf(readDelimiter, lastLoc);
		if (loc >= 0) {
			int delimiterLength = consumeDelimiter();
			consumeCallback().onSuccess(consume(loc + delimiterLength));
		}
		lastLoc = readBytes.length();		
	}
	
	private AsyncCallback<String> consumeCallback() {
		AsyncCallback<String> cb = callback;
		callback = null;
		return cb;
	}
	
	private int consumeDelimiter() {
		int delimiterLength = readDelimiter.length();
		readDelimiter = null;
		lastLoc = 0;
		return delimiterLength;
	}
	
	private boolean isReading() {
		return isReadingUntil();
	}
	
	private boolean isWriting() {
		return writeBuffer != null ? writeBuffer.remaining() > 0 : false;
	}
	
	private boolean isReadingUntil() {
		return readDelimiter != null;
	}
	
	public void close() {
		logger.debug("Closing channel: {}", channel);
		ioloop.removeHandler(channel);
		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isClosed() {
		return !channel.isOpen();
	}
	
	private void checkClosed() throws IOException {
		if (isClosed()) {
			throw new IOException();
		}
	}
	
	private String consume(int loc) {
		String result = readBytes.substring(0, loc);
		readBytes.delete(0, loc);
		return result;
	}
}
