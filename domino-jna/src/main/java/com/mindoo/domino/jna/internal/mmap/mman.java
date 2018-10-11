package com.mindoo.domino.jna.internal.mmap;

import java.io.RandomAccessFile;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * Source: <a href="https://github.com/voldemort/voldemort/blob/master/src/java/voldemort/store/readonly/io/jna/mman.java" target="_blank">mman.java</a><br>
 * License: Apache 2.0<br>
 * <br>
 * Will probably only work on Linux and Mac. Possible solution on Windows: <a href="https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/win32/Kernel32.java#L1032">MapViewOfFile</a>.
 */
public class mman {
	/** Page can be read. */
	public static final int PROT_READ = 0x1;
	/** Page can be written. */
	public static final int PROT_WRITE = 0x2;
	/** Page can be executed. */
	public static final int PROT_EXEC = 0x4;
	/** Page can not be accessed. */
	public static final int PROT_NONE = 0x0;

	/** Share changes. */
	public static final int MAP_SHARED = 0x01;
	/** Changes are private. */
	public static final int MAP_PRIVATE = 0x02;

	//http://www.delorie.com/djgpp/doc/incs/fcntl.h
	
	/** Open for reading only. */
	public static final int O_RDONLY = (int) (0x0000 & 0xffff);
	/** Open for writing only. */
	public static final int O_WRONLY = (int) (0x0001 & 0xffff);
	/** Open for reading and writing. The result is undefined if this flag is applied to a FIFO. */
	public static final int O_RDWR = (int) (0x0002 & 0xffff);

	/**
	 * If the file exists, this flag has no effect except as noted under
	 * O_EXCL below. Otherwise, the file shall be created; the user ID of
	 * the file shall be set to the effective user ID of the process; the
	 * group ID of the file shall be set to the group ID of the file's
	 * parent directory or to the effective group ID of the process;
	 * and the access permission bits (see &lt;sys/stat.h&gt;) of the file mode
	 * shall be set to the value of the third argument taken as type mode_t
	 * modified as follows: a bitwise AND is performed on the file-mode
	 * bits and the corresponding bits in the complement of the process'
	 * file mode creation mask. Thus, all bits in the file mode whose
	 * corresponding bit in the file mode creation mask is set are cleared.
	 * When bits other than the file permission bits are set, the effect is
	 * unspecified. The third argument does not affect whether the file is
	 * open for reading, writing, or for both. Implementations shall provide
	 * a way to initialize the file's group ID to the group ID of the parent
	 * directory. Implementations may, but need not, provide an
	 * implementation-defined way to initialize the file's group ID to the
	 * effective group ID of the calling process.
	 */
	public static final int O_CREAT = (int) (0x0100	& 0xffff);
	/**
	 * If O_CREAT and O_EXCL are set, open() shall fail if the file exists.
	 * The check for the existence of the file and the creation of the file
	 * if it does not exist shall be atomic with respect to other threads
	 * executing open() naming the same filename in the same directory
	 * with O_EXCL and O_CREAT set. If O_EXCL and O_CREAT are set, and
	 * path names a symbolic link, open() shall fail and set errno
	 * to [EEXIST], regardless of the contents of the symbolic link. If
	 * O_EXCL is set and O_CREAT is not set, the result is undefined.
	 */
	public static final int O_EXCL = (int) (0x0200 & 0xffff);
	/**
	 * If set and path identifies a terminal device, open() shall not
	 * cause the terminal device to become the controlling terminal
	 * for the process.
	 */
	public static final int O_NOCTTY = (int) (0x0400 & 0xffff);
	/**
	 * If the file exists and is a regular file, and the file is successfully
	 * opened O_RDWR or O_WRONLY, its length shall be truncated to 0, and
	 * the mode and owner shall be unchanged. It shall have no effect on
	 * FIFO special files or terminal device files. Its effect on other file
	 * types is implementation-defined. The result of using O_TRUNC with
	 * O_RDONLY is undefined.
	 */
	public static final int O_TRUNC = (int) (0x0800 & 0xffff);
	/**
	 * If set, the file offset shall be set to the end of the file prior to each write.
	 */
	public static final int O_APPEND = (int) (0x1000 & 0xffff);
	/**
	 * When opening a FIFO with O_RDONLY or O_WRONLY set:<br>
	 * <br>
	 * If O_NONBLOCK is set, an open() for reading-only shall return without
	 * delay. An open() for writing-only shall return an error if no process
	 * currently has the file open for reading.<br>
	 * <br>
	 * If O_NONBLOCK is clear, an open() for reading-only shall block the
	 * calling thread until a thread opens the file for writing. An open()
	 * for writing-only shall block the calling thread until a thread opens
	 * the file for reading.
	 */
	public static final int O_NONBLOCK = (int) (0x2000 & 0xffff);
	
	// http://linux.die.net/man/2/mmap
	// http://www.opengroup.org/sud/sud1/xsh/mmap.htm
	// http://linux.die.net/include/sys/mman.h
	// http://linux.die.net/include/bits/mman.h

	// off_t = 8
	// size_t = 8
	
	/**
	 * Map the given region of the given file descriptor into memory.
	 * Returns a Pointer to the newly mapped memory throws an
	 * IOException on error.
	 */
	
	/**
	 * Map the given region of the given file descriptor into memory.
	 * Returns a Pointer to the newly mapped memory throws an
	 * IOException on error.<br>
	 * <br>
	 * The contents of a file mapping (as opposed to an anonymous mapping;
	 * see MAP_ANONYMOUS below), are initialized using length bytes starting
	 * at offset offset in the file (or other object) referred to by the
	 * file descriptor fd.  offset must be a multiple of the page size as
	 * returned by sysconf(_SC_PAGE_SIZE).
	 * 
	 * @param len number of bytes to map
	 * @param prot describes the desired memory protection of the mapping: {@link #PROT_NONE}, {@link #PROT_READ}, {@link #PROT_WRITE}, {@link #PROT_EXEC}
	 * @param flags flags
	 * @param fd file descriptor
	 * @param off offset to start mapping, must be a multiple of the page size as returned by sysconf(_SC_PAGE_SIZE)
	 * @return pointer
	 */
	public static Pointer mmap(long len, int prot, int flags, int fd, long off) {

		// we don't really have a need to change the recommended pointer.
		Pointer addr = new Pointer(0);

		Pointer result = CLibrary.mmap(addr,
				new NativeLong(len),
				prot,
				flags,
				fd,
				new NativeLong(off));

		if(Pointer.nativeValue(result) == -1) {
			throw new RuntimeException("mmap failed: " + errno.strerror());
		}

		return result;

	}

	/**
	 * Unmap the given region.  Returns 0 on success or -1 (and sets
	 * errno) on failure.
	 * @param addr address
	 * @param len length
	 */
	public static void munmap(Pointer addr, long len) {
		int result = CLibrary.munmap(addr, new NativeLong(len));

		if(result != 0) {
			throw new RuntimeException("munmap failed: " + errno.strerror());
		}
	}

	/**
	 * Lock the given region.  Does not report failures.
	 * @param addr address
	 * @param len length
	 */
	public static void mlock(Pointer addr, long len) {
		int res = CLibrary.mlock(addr, new NativeLong(len));
		if(res != 0) {
			throw new RuntimeException("Mlock failed probably because of insufficient privileges, errno: "
					+ errno.strerror() + ", return value:" + res);
		}
	}

	/**
	 * Unlock the given region.  Does not report failures.
	 * 
	 * @param addr address
	 * @param len length
	 */
	public static void munlock(Pointer addr, long len) {
		if(CLibrary.munlock(addr, new NativeLong(len)) != 0) {
			throw new RuntimeException("munlock failed:" + errno.strerror());
		}
	}

	public static int open(String fileName, int flags) {
		int fd = CLibrary.open(fileName, flags);
		if (fd==-1) {
			throw new RuntimeException("Open failed: " + errno.strerror());
		}
		return fd;
	}
	
	public static int close(int fd) {
		int result = CLibrary.close(fd);
		if (result==-1) {
			throw new RuntimeException("Close failed:" + errno.strerror());
		}
		return result;
	}
	
	public static class CLibrary {

		public static native Pointer mmap(Pointer addr,
				NativeLong len,
				int prot,
				int flags,
				int fildes,
				NativeLong off);

		public static native int munmap(Pointer addr, NativeLong len);

		public static native int mlock(Pointer addr, NativeLong len);

		public static native int munlock(Pointer addr, NativeLong len);
		
		public static native int open(String name, int options);

		public static native int close(int fd);
		
		static {
			Native.register("c");
		}

	}

	public static void main(String[] args) throws Exception {
		String filePath = "/tmp/largefile.bin";
		int fileLength = 100 * 1024 * 1024;
		
		RandomAccessFile f = new RandomAccessFile(filePath, "rw");
        f.setLength(fileLength);
        f.close();
		
		int fd = open(filePath, O_RDWR);
		try {
			System.out.println("File descriptor is: " + fd);

			Pointer addr = mmap(fileLength, PROT_WRITE, mman.MAP_SHARED, fd, 0L);
			
			System.out.println("mmap address is: " + Pointer.nativeValue(addr));

			// try to mlock it directly
			mlock(addr, fileLength);
			
			addr.setByte(0, (byte) 1);
			addr.setByte(1, (byte) 2);
			addr.setByte(2, (byte) 3);
			
			munlock(addr, fileLength);

			munmap(addr, fileLength);
		}
		finally {
			if (fd!=0)
				close(fd);
		}
        
	}
}