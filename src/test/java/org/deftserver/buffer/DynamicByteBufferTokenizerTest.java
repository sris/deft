package org.deftserver.buffer;

import static org.junit.Assert.*;

import java.nio.BufferUnderflowException;
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
	private byte[] delimiter2 = {0,0,0,0,0,0,0,0,0,0};
	
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
		int pos = 0;
		for (int i = 0; i < arrays.length; i++) {
			System.arraycopy(arrays[i], 0, result, pos, arrays[i].length);
			pos += arrays[i].length;
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
	
	@Test
	public void appendHundredTimesCapacityNbrItem() {
		this.dbbt.append(ByteBuffer.wrap(buildData(INITIAL_CAPACITY*100, 99)));
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityDelimiterInStarNbrItem() {
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		this.dbbt.append(ByteBuffer.wrap(buildData(INITIAL_CAPACITY*100, 99)));
		assertTrue(this.dbbt.hasNext());
		assertArrayEquals(delimiter1, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityDelimiterInMiddleNbrItem() {
		byte[] part1 = buildData(INITIAL_CAPACITY*50, 99);
		byte[] part2 = buildData(INITIAL_CAPACITY*50, 88);
		this.dbbt.append(ByteBuffer.wrap(part1));
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		this.dbbt.append(ByteBuffer.wrap(part2));
		assertTrue(this.dbbt.hasNext());
		byte[] shouldbe = arrayConcat(part1, delimiter1);
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityDelimiterInEndNbrItem() {
		byte[] part1 = buildData(INITIAL_CAPACITY*50, 99);
		byte[] part2 = buildData(INITIAL_CAPACITY*50, 88);
		this.dbbt.append(ByteBuffer.wrap(part1));
		this.dbbt.append(ByteBuffer.wrap(part2));
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		assertTrue(this.dbbt.hasNext());
		byte[] shouldbe = arrayConcat(part1, part2, delimiter1);
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityChunkedDelimiterInStarNbrItem() {
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		for (int i = 0; i < 100; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, i);
			this.dbbt.append(ByteBuffer.wrap(data));
		}
		
		assertTrue(this.dbbt.hasNext());
		assertArrayEquals(delimiter1, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityChunkedDelimiterInMiddleNbrItem() {
		byte[] shouldbe = {};
		for (int i = 0; i < 50; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, i);
			this.dbbt.append(ByteBuffer.wrap(data));
			shouldbe = arrayConcat(shouldbe, data);
		}
		
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		shouldbe = arrayConcat(shouldbe, delimiter1);

		for (int i = 0; i < 50; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, 50+i);
			this.dbbt.append(ByteBuffer.wrap(data));
		}

		assertTrue(this.dbbt.hasNext());
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityChunkedDelimiterInEndNbrItem() {
		byte[] shouldbe = {};
		for (int i = 0; i < 100; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, i);
			this.dbbt.append(ByteBuffer.wrap(data));
			shouldbe = arrayConcat(shouldbe, data);
		}
		this.dbbt.append(ByteBuffer.wrap(delimiter1));
		shouldbe = arrayConcat(shouldbe, delimiter1);
		assertTrue(this.dbbt.hasNext());
		assertArrayEquals(shouldbe, this.dbbt.next().array());
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}

	@Test
	public void appendHundredTimesCapacityHundredDelimiters() {
		byte[][] shouldbees = new byte[100][INITIAL_CAPACITY + delimiter1.length];
		for (int i = 0; i < shouldbees.length; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, i);
			this.dbbt.append(ByteBuffer.wrap(data));
			this.dbbt.append(ByteBuffer.wrap(delimiter1));
			assertTrue(this.dbbt.hasNext());
			shouldbees[i] = arrayConcat(data, delimiter1);
		}

		for (int i = 0; i < shouldbees.length; i++) {
			assertArrayEquals(shouldbees[i], this.dbbt.next().array());
		}
		
		assertFalse(this.dbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		this.dbbt.next();
	}
	
	@Test
	public void appendHundredTimesCapacityHundredBigDelimiters() {
		DynamicByteBufferTokenizer aDbbt = new DynamicByteBufferTokenizer(INITIAL_CAPACITY, delimiter2);
		byte[][] shouldbees = new byte[100][INITIAL_CAPACITY + delimiter2.length];
		for (int i = 0; i < shouldbees.length; i++) {
			byte[] data = buildData(INITIAL_CAPACITY, i+1);
			aDbbt.append(ByteBuffer.wrap(data));
			aDbbt.append(ByteBuffer.wrap(delimiter2));
			assertTrue(aDbbt.hasNext());
			shouldbees[i] = arrayConcat(data, delimiter2);
		}
		
		for (int i = 0; i < shouldbees.length; i++) {
			assertArrayEquals(shouldbees[i], aDbbt.next().array());
		}
		
		assertFalse(aDbbt.hasNext());
		exception.expect(NoSuchElementException.class);
		aDbbt.next();
	}
	
	@Test
	public void sliceEmptyBuffer() {
		exception.expect(BufferUnderflowException.class);
		this.dbbt.slice(10);
	}

	@Test
	public void sliceCapacityBuffer() {
		byte[] shouldbe = buildData(INITIAL_CAPACITY, 99);
		this.dbbt.append(ByteBuffer.wrap(shouldbe));
		assertArrayEquals(shouldbe, this.dbbt.slice(INITIAL_CAPACITY).array());
		exception.expect(BufferUnderflowException.class);
		this.dbbt.slice(10);
	}
	
	@Test
	public void sliceTenTimeCapacityBuffer() {
		int num = INITIAL_CAPACITY*10;
		byte[] shouldbe = buildData(num, 99);
		this.dbbt.append(ByteBuffer.wrap(shouldbe));
		assertArrayEquals(shouldbe, this.dbbt.slice(num).array());
		exception.expect(BufferUnderflowException.class);
		this.dbbt.slice(1);
	}
	
	@Test
	public void sliceTenTimeCapacityBufferTenTimes() {
		int num = INITIAL_CAPACITY*10;
		byte[] shouldbe = buildData(num, 99);
		this.dbbt.append(ByteBuffer.wrap(shouldbe));
		for (int i = 0; i < 10; i++) {
			assertArrayEquals(buildData(INITIAL_CAPACITY, 99), this.dbbt.slice(INITIAL_CAPACITY).array());
		}
		exception.expect(BufferUnderflowException.class);
		this.dbbt.slice(1);
	}
}