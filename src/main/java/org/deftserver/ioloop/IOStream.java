package org.deftserver.ioloop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.deftserver.buffer.DynamicByteBufferTokenizer;
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
	private final int chunkSize = 1500;
	
	private DynamicByteBufferTokenizer data;
	private boolean readingUntil = false;
	private int readingBytes = 0;
	
	private AsyncCallback<byte[]> callback;
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
		this.readBuffer = ByteBuffer.allocate(chunkSize);
		this.data = new DynamicByteBufferTokenizer(chunkSize);
		this.ioOps = SelectionKey.OP_READ;
		this.ioloop.addHandler(this.channel, this, SelectionKey.OP_READ);
	}

	// TODO: Should this return byte[] or ByteBuffer. cmp. write()
	public void readUntil(byte[] delimiter, AsyncCallback<byte[]> callback) throws IOException {
		this.data.setDelimiter(delimiter);
		if (this.data.hasNext()) {
			callback.onSuccess(this.data.next().array());
			return;
		}
		checkClosed();
		this.readingUntil = true;
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
		// TODO: Buffer written data?
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
			data.append(readBuffer);
			
			if (isReadingUntil()) {
				searchForDelimiter();
			} else if (isReadingBytes()) {
				;
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
		if (data.hasNext()) {
			consumeCallback().onSuccess(data.next().array());
		}
	}
	
	private AsyncCallback<byte[]> consumeCallback() {
		AsyncCallback<byte[]> cb = callback;
		callback = null;
		readingUntil = false;
		readingBytes = 0;
		return cb;
	}
	
	private boolean isReading() {
		return isReadingUntil();
	}
	
	private boolean isWriting() {
		return writeBuffer != null ? writeBuffer.remaining() > 0 : false;
	}
	
	private boolean isReadingUntil() {
		return readingUntil;
	}
	
	private boolean isReadingBytes() {
		return readingBytes > 0;
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
}
