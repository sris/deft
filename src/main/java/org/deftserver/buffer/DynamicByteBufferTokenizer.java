package org.deftserver.buffer;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicByteBufferTokenizer implements Iterator<ByteBuffer> {
	
	private final static Logger logger = LoggerFactory.getLogger(DynamicByteBufferTokenizer.class);
	
	private byte[] value;
	private byte[] delimiter = {};
	
	private int delimiterLenght;
	private int searchPos = 0;
	
	private int count = 0;
	
	private boolean hasNext = false;
	
	public DynamicByteBufferTokenizer(int capacity, byte[] delimiter) {
		value = new byte[capacity];
		setDelimiter(delimiter);
	}

	public void setDelimiter(byte[] delimiter) {
		if (!this.delimiter.equals(delimiter)) {
			this.delimiter = delimiter;
			this.delimiterLenght = delimiter.length;
		}
	}
	
	// buffer must be flipped by caller
	public DynamicByteBufferTokenizer append(ByteBuffer buffer) {
		int len = buffer.remaining();
		int newCount = count + len;
		ensureCapacity(newCount);
		buffer.get(value, count, len);
		count = newCount;
		return this;
	}
	
	public void ensureCapacity(int minimumCapacity) {
		if (minimumCapacity > value.length) {
			expandCapacity(minimumCapacity);
		}
	}
	
	void expandCapacity(int minimumCapacity) {
		int newCapacity = (value.length + 1) * 2;
		if (newCapacity < 0) {
			newCapacity = Integer.MAX_VALUE;
		} else if (minimumCapacity > newCapacity) {
			newCapacity = minimumCapacity;
		}
		value = Arrays.copyOf(value, newCapacity);
	}
	
	private int search() {
		int end = count - delimiterLenght;
		int match = 0;
		while (searchPos <= end && (match = match()) != delimiterLenght) {
			searchPos++;
		}
		if (match == delimiterLenght) {
			// match found
			return searchPos;
		} else {
			return -1;
		}
	}
	
	private int match() {
		int matchLength = 0;
		while (matchLength < delimiterLenght &&
				delimiter[matchLength] == value[searchPos + matchLength]) {
			matchLength++;
		}
		return matchLength;
	}
	
	private void compact(int num) {
		System.arraycopy(value, num, value, 0, count - num);
		count -= num;
		searchPos = 0;
		hasNext = false;
	}
	
	public ByteBuffer slice(int num) {
		if (num > size()) {
			throw new BufferUnderflowException();
		}
		return consume(num);
	}
	
	public int size() {
		return count;
	}
	
	private ByteBuffer consume(int num) {
		ByteBuffer result = ByteBuffer.wrap(Arrays.copyOfRange(value, 0, num));
		compact(num);
		return result;
	}
	
	@Override
	public boolean hasNext() {
		if (searchPos <= count - delimiterLenght && 
				search() >= 0) {
			hasNext = true;
		}
		return hasNext;
	}

	// returns flipped buffer (ready to read)
	@Override
	public ByteBuffer next() {
		if (searchPos <= count - delimiterLenght &&
				!hasNext) {
			hasNext();
		}
		if (!hasNext) {
			throw new NoSuchElementException();
		}
		return consume(searchPos + delimiterLenght);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
