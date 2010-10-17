package org.deftserver.example.echo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.deftserver.ioloop.IOStream;
import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineConnection implements AsyncCallback<String> {
	
	private final static Logger logger = LoggerFactory.getLogger(LineConnection.class);
	
	private IOStream stream;
	private LineHandler handler;
	private final String delimiter = "\r\n";
	
	public LineConnection(IOStream stream, LineHandler handler) throws IOException {
		this.stream = stream;
		this.handler = handler;
		this.stream.readUntil(delimiter.getBytes(), this);
	}
	
	public void write(String data) {
		// TODO: Check if closed?
		try {
			this.stream.write(ByteBuffer.wrap(data.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			logger.error("Failed to encode line: {} {}", data, e);
		}
	}
	
	@Override
	public void onFailure(Throwable caught) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSuccess(String result) {
		handler.handleLine(new LineRequest(this, result.substring(0, result.indexOf(delimiter))));
	}
}
