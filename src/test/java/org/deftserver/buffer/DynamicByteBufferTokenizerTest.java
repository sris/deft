package org.deftserver.buffer;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DynamicByteBufferTokenizerTest {

	private DynamicByteBufferTokenizer dbbt;
	private static final int INITIAL_CAPACITY = 10;	// bytes
	private byte[] delimiter1 = "\r\n".getBytes(Charset.forName("ASCII"));
	
	private byte[] buildData(int size, int value) {
		byte[] data = new byte[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte)value;
		}		
		return data;
	}
	
	private byte[] arrayConcat(byte[]... arrays) {
		int newLength = 0;
		for (byte[] b : arrays) {
			newLength += b.length;
		}
		byte[] result = new byte[newLength];
		for (int i = 0; i < arrays.length; i++) {
			System.arraycopy(arrays[i], 0, result, 
					i > 0 ? arrays[i-1].length : 0, arrays[i].length);
		}
		return result;
	}
	
	@Rule
	 public ExpectedException exception = ExpectedException.none();

	
	@Before
	public void allocation() {
		this.dbbt = new DynamicByteBufferTokenizer(INITIAL_CAPACITY, delimiter1);
	}
	
	@Test
	public void appendOneItem() {
		this.dbbt.append(ByteBuffer.wrap(buildData(1, 99)));
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendCapacityNbrItem() {
		this.dbbt.append(ByteBuffer.wrap(buildData(INITIAL_CAPACITY, 99)));
		assertFalse(this.dbbt.hasNext());
	}
	
	@Test
	public void appendCapacityNbrItemMultipleAppends() {
		for (int i = 0; i < INITIAL_CAPACITY; i++) {
			this.dbbt.append(ByteBuffer.wrap(buildData(1, 99)));
			assertFalse(this.dbbt.hasNext());
		}
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendDelimiter() {
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		assertTrue(this.dbbt.hasNext());
		ByteBuffer data = this.dbbt.next();
		assertArrayEquals(delimiter1, data.array());
		assertFalse(this.dbbt.hasNext());
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendCapacityEndWithDelimiter() {
		this.dbbt.append(ByteBuffer.wrap(buildData(INITIAL_CAPACITY - delimiter1.length, 99)));
		assertFalse(this.dbbt.hasNext());
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		assertTrue(this.dbbt.hasNext());
		byte[] shouldbe = buildData(INITIAL_CAPACITY, 99);
		shouldbe[8] = delimiter1[0];
		shouldbe[9] = delimiter1[1];
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		assertFalse(this.dbbt.hasNext());
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}

	@Test
	public void appendCapacityDelimiterInMiddle() {
		byte[] part1 = buildData((int)Math.floor((INITIAL_CAPACITY-delimiter1.length)/2.0), 99);
		byte[] part2 = buildData((int)Math.ceil((INITIAL_CAPACITY-delimiter1.length)/2.0), 99);
		this.dbbt.append(ByteBuffer.wrap(part1));
		assertFalse(this.dbbt.hasNext());
		
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		this.dbbt.append(ByteBuffer.wrap(part2));
		assertTrue(this.dbbt.hasNext());
		
		byte[] shouldbe = arrayConcat(part1, delimiter1); 
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		assertFalse(this.dbbt.hasNext());
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendCapacityHalfDelimiterStart() {
		this.dbbt.append(ByteBuffer.wrap(new byte[] { delimiter1[0] }));
		assertFalse(this.dbbt.hasNext());
		byte[] part1 = buildData(INITIAL_CAPACITY-1, 99);
		this.dbbt.append(ByteBuffer.wrap(part1));
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendCapacityTwoDelimiter() {
		byte[] part1 = buildData(3, 99);
		byte[] part2 = buildData(3, 99);
		this.dbbt.append(ByteBuffer.wrap(part1));
		assertFalse(this.dbbt.hasNext());
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		assertTrue(this.dbbt.hasNext());
		this.dbbt.append(ByteBuffer.wrap(part2));
		assertTrue(this.dbbt.hasNext());
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		assertTrue(this.dbbt.hasNext());
		
		byte[] shouldbe = arrayConcat(part1, delimiter1);
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		assertTrue(this.dbbt.hasNext());
		assertTrue(this.dbbt.hasNext());

		shouldbe = arrayConcat(part2, delimiter1);
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		assertFalse(this.dbbt.hasNext());
		assertFalse(this.dbbt.hasNext());
		
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendTenTimesCapacityNbrItem() {
		this.dbbt.append(ByteBuffer.wrap(buildData(INITIAL_CAPACITY*10, 99)));
		assertFalse(this.dbbt.hasNext());
	}
	
	@Test
	public void appendTenTimesCapacityNbrItemMultipleAppends() {
		for (int i = 0; i < INITIAL_CAPACITY*10; i++) {
			this.dbbt.append(ByteBuffer.wrap(buildData(1, 99)));
			assertFalse(this.dbbt.hasNext());
		}
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
}