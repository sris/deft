package org.deftserver.example.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.deftserver.ioloop.EventHandler;
import org.deftserver.ioloop.IOLoop;
import org.deftserver.ioloop.IOStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineServer implements EventHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(LineServer.class);
	
	private ServerSocketChannel channel;
	private LineHandler handler;
	
	public LineServer(LineHandler handler) {
		this.handler = handler;
	}
	
	public void listen(InetSocketAddress endpoint) {
		bind(endpoint);
		start();
	}

	public void bind(InetSocketAddress endpoint) {
		try {
			channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
		} catch (IOException e) {
			logger.error("Error creating ServerSocketChannel: {}", e);
		}
		try {
			channel.socket().bind(endpoint);
		} catch (IOException e) {
			logger.error("Could not bind socket: {}", e);
		}
	}
	
	public void start() {
		IOLoop.getInstance().addHandler(channel, this, SelectionKey.OP_ACCEPT);
	}

	@Override
	public void handleEvents(SelectionKey key) {
		try {
			SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
			socketChannel.configureBlocking(false);
			IOStream stream = new IOStream(socketChannel);
			new LineConnection(stream, handler);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
