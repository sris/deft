package org.deftserver.web.protocol;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.deftserver.util.DateUtil;
import org.deftserver.util.HttpUtil;
import org.deftserver.web.handler.StaticContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

public class HttpResponse {
	
	private final static Logger logger = LoggerFactory.getLogger(HttpProtocolImpl.class);
	
	private final SocketChannel clientChannel;
	
	private /*<> AtomicInteger */ int statusCode = 200;	// default response status code
	
	//<> TODO RS 100924 could experiment with cliff clicks high scale lib (e.g. NonBlockingHashMap) instead of
	//<> the CCHM used below 
	//<> private final ConcurrentMap<String, String> headers = new ConcurrentHashMap<String, String>();
	
	private final Map<String, String> headers = new HashMap<String, String>();
	private boolean headersCreated = false;
	private String responseData = "";
	private final boolean keepAlive;
	
	public HttpResponse(SocketChannel sc, boolean keepAlive) {
		clientChannel = sc;
		headers.put("Server", "DeftServer/0.1.0");
		headers.put("Date", DateUtil.getCurrentAsString());

		if (keepAlive) {
			this.keepAlive = true;
			headers.put("Connection", "Keep-Alive");
		} else {
			this.keepAlive = false;
			headers.put("Connection", "Close");	
		}
	}
	
	public void setStatusCode(int sc) {
		statusCode = sc;
	}
	
	public void setHeader(String header, String value) {
		headers.put(header, value);
	}

	public HttpResponse write(String data) {
		responseData +=data;
		return this;
	}
		
	public long flush() {
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();			
			responseData = initial + responseData;
			headersCreated = true;
		}
		ByteBuffer output = ByteBuffer.wrap(responseData.getBytes(Charsets.UTF_8));
		long bytesWritten = 0;
		try {
			bytesWritten = clientChannel.write(output);
		} catch (IOException e) {
			logger.error("Error writing response: {}", e.getMessage());
		} finally {
			responseData = "";
		}
		return bytesWritten;
	}
	
	public long finish() {
		long bytesWritten = 0;
		if (clientChannel.isOpen()) {
			if (!headersCreated) {
				setHeader("Etag", HttpUtil.getEtag(responseData.getBytes()));
				setHeader("Content-Length", ""+responseData.getBytes(Charsets.UTF_8).length);	// TODO RS faster/better with new Integer(..)?
			}
			bytesWritten = flush();
			if (!keepAlive) {
				Closeables.closeQuietly(clientChannel);
			}
		}	
		return bytesWritten;
	}
	
	private /*<> synchronzied */ String createInitalLineAndHeaders() {
		StringBuilder sb = new StringBuilder(HttpUtil.createInitialLine(statusCode));
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(header.getKey());
			sb.append(": ");
			sb.append(header.getValue());
			sb.append("\r\n");
		}
		
		sb.append("\r\n");
		return sb.toString();
	}

	/**
	 * Should only be called by {@link StaticContentHandler}.
	 * @param file Requested static resource 
	 */
	public long write(File file) {
		//setHeader("Etag", HttpUtil.getEtag(file));
		setHeader("Content-Length", String.valueOf(file.length()));
		long bytesWritten = 0;
		flush();	// write initial line + headers
		try {
			bytesWritten = new RandomAccessFile(file, "r").getChannel().transferTo(0, file.length(), clientChannel);
		} catch (IOException e) {
			logger.error("Error writing (static file) response: {}", e.getMessage());
		}
		return bytesWritten;
	}

	
}
