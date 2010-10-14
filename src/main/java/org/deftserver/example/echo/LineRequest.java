package org.deftserver.example.echo;

public class LineRequest {
	
	private LineConnection connection;
	private String line;
	
	public LineRequest(LineConnection connection, String line) {
		this.connection = connection;
		this.line = line;
	}

	public LineConnection getConnection() {
		return connection;
	}

	public String getLine() {
		return line;
	}
}
