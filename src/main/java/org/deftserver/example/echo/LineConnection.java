package org.deftserver.example.echo;

import org.deftserver.ioloop.IOStream;
import org.deftserver.web.AsyncCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineConnection implements AsyncCallback<String> {
	
	private final static Logger logger = LoggerFactory.getLogger(LineConnection.class);
	
	private IOStream stream;
	private LineHandler handler;
	enum State {
	    WAITING 
	};
	private State state = State.WAITING;
	
	public LineConnection(IOStream stream, LineHandler handler) {
		this.stream = stream;
		this.handler = handler;
		this.stream.readUntil("\r\n", this);
	}
	
	@Override
	public void onFailure(Throwable caught) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSuccess(String result) {
		handler.handleLine(new LineRequest(this, result));
	}
}
