package org.deftserver.example.echo;

import java.net.InetSocketAddress;

import org.deftserver.ioloop.IOLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineTest implements LineHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(LineTest.class);
	private final static int port = 9999;
	
	public static void main(String[] args) {
		LineServer server = new LineServer(new LineTest());
		server.listen(new InetSocketAddress(port));
		logger.info("Listening on port {}", port);
		IOLoop.getInstance().start();
	}
	
	
	@Override
	public void handleLine(LineRequest request) {
		logger.info(">{}#", request.getLine());
		request.getConnection().write("< Deft LineServer: " + request.getLine() + "\n");
	}
}
