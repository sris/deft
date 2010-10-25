package org.deftserver.example.echo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.deftserver.ioloop.IOStream;
import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineConnection implements AsyncCallback<byte[]> {
	
	private final static Logger logger = LoggerFactory.getLogger(LineConnection.class);
	
	private IOStream stream;
	private LineHandler handler;
	private final String delimiter = "\r\n";
	
	public LineConnection(IOStream stream, LineHandler handler) {
		this.stream = stream;
		this.handler = handler;
		readLine();
	}
	
	public void write(String data) {
		// TODO: Check if closed?
		try {
			this.stream.write(ByteBuffer.wrap(data.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			logger.error("Failed to encode line: {} {}", data, e);
		}
	}
	
	private void readLine() {
		try {
			this.stream.readUntil(delimiter.getBytes(), this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onFailure(Throwable caught) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSuccess(byte[] result) {
		String data = new String(result);
		handler.handleLine(new LineRequest(this, data.substring(0, data.indexOf(delimiter))));
		readLine();
	}
}
