package com.mindoo.domino.jna.test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.utils.DumpUtil;
import com.mindoo.domino.jna.utils.LZ1CompressOutputStream;
import com.mindoo.domino.jna.utils.LZ1Decompress;
import com.mindoo.domino.jna.utils.PlatformUtils;

import lotus.domino.Session;

/**
 * Tests compression and decompression using LZ1 algorithm
 * 
 * @author Karsten Lehmann
 */
public class TestLZ1Compression extends BaseJNATestClass {

	@Test
	public void testCompression() {
		runWithSession(new IDominoCallable<Object>() {

			@Override
			public Object call(Session session) throws Exception {
				int uncompressedRandomDataSize = 500000;
				System.out.println("Generating input data for compression ("+uncompressedRandomDataSize+" bytes)");
				
				byte[] uncompressedRandomData = new byte[uncompressedRandomDataSize];
//				int x=0;
				for (int i=0; i<uncompressedRandomData.length; i++) {
//					uncompressedRandomData[i] = (byte) (Math.random()*255);
//					uncompressedRandomData[i] = (byte) (x & 0xff);
					uncompressedRandomData[i] = (byte) (i % 4);
//					if ((i % 20) == 0)
//						x++;
				}
				
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				LZ1CompressOutputStream lz1Out = new LZ1CompressOutputStream(bOut, 1000000);
				try {
					lz1Out.write(uncompressedRandomData);
				}
				finally {
					lz1Out.close();
				}
				
				byte[] compressedRandomData = bOut.toByteArray();
				int sizeOfCompressedData = compressedRandomData.length;
				System.out.println("Compressed data size: "+sizeOfCompressedData+" bytes");
				
				DisposableMemory compressedRandomDataMem = new DisposableMemory(sizeOfCompressedData);
				compressedRandomDataMem.write(0, compressedRandomData, 0, compressedRandomData.length);
				
				System.out.println("compressedRandomDataMem:\n"+DumpUtil.dumpAsAscii(compressedRandomDataMem, 200));
				
				DisposableMemory uncompressedRandomDataOutMem = new DisposableMemory(uncompressedRandomDataSize);
				short result;
				if (PlatformUtils.is64Bit()) {
					result = NotesNativeAPI64.get().LZ1Decompress(compressedRandomDataMem,
							uncompressedRandomDataOutMem, uncompressedRandomDataSize);
				}
				else {
					result = NotesNativeAPI32.get().LZ1Decompress(compressedRandomDataMem,
							uncompressedRandomDataOutMem, uncompressedRandomDataSize);
				}
				NotesErrorUtils.checkResult(result);
				
				byte[] uncompressedDataToCompare = uncompressedRandomDataOutMem.getByteArray(0, uncompressedRandomDataSize);
				
				System.out.println("Original:\n"+DumpUtil.dumpAsAscii(ByteBuffer.wrap(uncompressedRandomData), 100));
				System.out.println("Result of compress-decompress:\n"+DumpUtil.dumpAsAscii(ByteBuffer.wrap(uncompressedDataToCompare), 100));
				Assert.assertArrayEquals(uncompressedRandomData, uncompressedDataToCompare);
				
				return null;
			}
		});
	}
	
}
