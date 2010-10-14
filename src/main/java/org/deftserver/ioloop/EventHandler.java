package org.deftserver.ioloop;

import java.nio.channels.SelectionKey;

public interface EventHandler {
	public void handleEvents(SelectionKey key);
}
