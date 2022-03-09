package com.mindoo.domino.jna.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.NotesDatabase;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class for basic read and write support of the Notes client workspace.<br>
 * We provide functionality to
 * <ul>
 * <li>Read/write the tab titles</li>
 * <li>Set workspace flags showUnreadMarks, showServerNames, stackDatabases</li>
 * <li>Read/write icon server, filename, title, position and tabindex</li>
 * </ul>
 * <br>
 * The class currently does not allow creation of new icons and we don't have read support
 * for icon data, neither in old (low-res) nor new (hi-res) format. We also don't have
 * support for all icon flags like unread counts. Some of these things might be added later
 * after doing more reverse engineering.
 * 
 * @author Karsten Lehmann
 */
public class NotesWorkspace {
	//fixed structure sizes (icon structure depends on version)
	private static final int HEADERSIZE = 58;
	private static final int TABSIZE = 40;
	
	private static final int POS_HEADER_FLAGS = 0x06;
	private static final int POS_HEADER_ICONCOUNT = 0x0C;
	private static final int POS_HEADER_TABCOUNT = 0x12;
	
	private static final int FLAGS_SHOWUNREADMARKS = 0x01;
	private static final int FLAGS_SHOWSERVERNAMES = 0x02;
	private static final int FLAGS_STACKDATABASES = 0x04;

	private NotesDatabase m_dbDesktop;
	private int m_workspaceObjId;

	private List<WorkspaceTab> m_tabs = new ArrayList<>();
	private List<WorkspaceIcon> m_icons = new ArrayList<>();
	
	//buffer with all workspace data
	protected ByteBuffer m_workspaceAllBuf;
	//buffer with the workspace header
	protected ByteBuffer m_workspaceHeaderBuf;
	//buffer with iconCount*icondata plus additional hi-res icons
	protected ByteBuffer m_allIconDataBuf;
	//buffer with just the additional hi-res icons
	protected ByteBuffer m_remainingIconDataBuf;
	private boolean m_modified;
	
	/**
	 * Loads the Notes workspace info
	 * 
	 * @param dbDesktop desktop database, e.g. desktop8.ndk
	 */
	public NotesWorkspace(NotesDatabase dbDesktop) {
		m_dbDesktop = dbDesktop;
		init();
	}
	
	public boolean isModified() {
		return m_modified;
	}
	
	/**
	 * Returns a change date of the Workspace
	 * 
	 * @return date/time
	 */
	public NotesTimeDate getWorkspaceTimeDate() {
		int desktopTimeInnards1 = m_workspaceHeaderBuf.getInt(0x16);
		int desktopTimeInnards0 = m_workspaceHeaderBuf.getInt(0x1A);
		return new NotesTimeDate(new int[] {desktopTimeInnards0, desktopTimeInnards1});
	}
	
	public NotesWorkspace setWorkspaceTimeDate(NotesTimeDate td) {
		int[] innards = td.getInnards();
		m_workspaceHeaderBuf.putInt(0x16, innards[1]);
		m_workspaceHeaderBuf.putInt(0x1A, innards[0]);
		return this;
	}
	
	public boolean isShowUnreadMarks() {
		byte flags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
		return (flags & FLAGS_SHOWUNREADMARKS) == FLAGS_SHOWUNREADMARKS;
	}
	
	public NotesWorkspace setShowUnreadMarks(boolean b) {
		if (b) {
			if (!isShowUnreadMarks()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags | FLAGS_SHOWUNREADMARKS) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		else {
			if (isShowUnreadMarks()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags & ~FLAGS_SHOWUNREADMARKS) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		return this;
	}
	
	public boolean isShowServerNames() {
		byte flags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
		return (flags & FLAGS_SHOWSERVERNAMES) == FLAGS_SHOWSERVERNAMES;
	}
	
	public NotesWorkspace setShowServerNames(boolean b) {
		if (b) {
			if (!isShowServerNames()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags | FLAGS_SHOWSERVERNAMES) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		else {
			if (isShowServerNames()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags & ~FLAGS_SHOWSERVERNAMES) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		return this;
	}
	
	public boolean isStackDatabases() {
		byte flags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
		return (flags & FLAGS_STACKDATABASES) == FLAGS_STACKDATABASES;
	}
	
	public NotesWorkspace setStackDatabases(boolean b) {
		if (b) {
			if (!isStackDatabases()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags | FLAGS_STACKDATABASES) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		else {
			if (isStackDatabases()) {
				byte oldFlags = m_workspaceHeaderBuf.get(POS_HEADER_FLAGS);
				byte newFlags = (byte) ((oldFlags & ~FLAGS_STACKDATABASES) & 0xff);
				m_workspaceHeaderBuf.put(POS_HEADER_FLAGS, newFlags);
				m_modified = true;
			}
		}
		return this;
	}
	
	public List<WorkspaceTab> getTabs() {
		return Collections.unmodifiableList(m_tabs);
	};

	public List<WorkspaceIcon> getIcons() {
		return Collections.unmodifiableList(m_icons);
	}
	
	public List<WorkspaceIcon> getIcons(WorkspaceTab tab) {
		int idx = m_tabs.indexOf(tab);
		if (idx==-1) {
			return Collections.emptyList();
		}
		else {
			return m_icons
					.stream()
					.filter((icon) -> {
						return icon.getTabIndex() == idx;
					})
					.collect(Collectors.toList());
		}
	}
	
	public NotesDatabase getDatabase() {
		return m_dbDesktop;
	}
	
	@Override
	public String toString() {
		return "NotesWorkspace [database=" + getDatabase() + ", timedate=" + getWorkspaceTimeDate()
				+ ", showUnreadMarks=" + isShowUnreadMarks() + ", showServerNames=" + isShowServerNames()
				+ ", stackDatabases=" + isStackDatabases() + ", tabs=" + getTabs() + ", numberoficons="
				+ getIcons().size() + "]";
	}

	/**
	 * Stores the changes in the desktop database. This currently does not trigger
	 * any visual update in the Notes Client, but the changes appear when the client
	 * is closed and reopened.
	 * 
	 * @return this workspace
	 */
	public NotesWorkspace store() {
		int objSize = m_workspaceAllBuf.limit();
		if (m_dbDesktop.isRecycled()) {
			throw new NotesError("Desktop database already recycled");
		}
		
		final DHANDLE.ByReference retCopyBufferHandle = DHANDLE.newInstanceByReference();
		short result = Mem.OSMemAlloc((short) 0, objSize, retCopyBufferHandle);
		NotesErrorUtils.checkResult(result);

		try {
			//write data into copy buffer
			Pointer ptrBuffer = Mem.OSLockObject(retCopyBufferHandle);
			try {
				byte[] newData = new byte[objSize];
				m_workspaceAllBuf.position(0);
				m_workspaceAllBuf.get(newData);
				m_workspaceAllBuf.position(0);
				
				ptrBuffer.write(0, newData, 0, objSize);
			}
			finally {
				Mem.OSUnlockObject(retCopyBufferHandle);
			}

			HANDLE.ByValue hDbByVal = m_dbDesktop.getHandle().getByValue();

			IntByReference retSize = new IntByReference();
			ShortByReference retClass = new ShortByReference();
			ShortByReference retPrivileges = new ShortByReference();

			result = NotesNativeAPI.get().NSFDbGetObjectSize(
					hDbByVal,
					m_workspaceObjId,
					NotesConstants.OBJECT_UNKNOWN,
					retSize,
					retClass,
					retPrivileges);
			NotesErrorUtils.checkResult(result);

			//make sure the NSF object has the right size
			if (retSize.getValue() != objSize) {
				result = NotesNativeAPI.get().NSFDbReallocObject(hDbByVal, m_workspaceObjId, objSize);
				NotesErrorUtils.checkResult(result);
			}

			//write copy buffer into NSF object
			result = NotesNativeAPI.get().NSFDbWriteObject(
					hDbByVal,
					m_workspaceObjId,
					retCopyBufferHandle.getByValue(),
				0,
				objSize);
			NotesErrorUtils.checkResult(result);
			
		}
		finally {
			//free copy buffer
			Mem.OSMemFree(retCopyBufferHandle.getByValue());
		}
		
		m_modified = false;
		return this;
	}
	
	private void init() {
		DBClass dbClass = m_dbDesktop.getDbClass();

		if (dbClass != DBClass.DESKTOP) {
			throw new IllegalStateException("Database "+m_dbDesktop.getAbsoluteFilePathOnLocal()+" has the wrong type: "+dbClass);
		}

		HANDLE hDb = m_dbDesktop.getHandle();
		IntByReference retNoteId = new IntByReference();
		short result = NotesNativeAPI.get().NSFDbGetSpecialNoteID(hDb.getByValue(), (short) 18, retNoteId);
		NotesErrorUtils.checkResult(result);

		m_workspaceObjId = retNoteId.getValue();

		IntByReference retSize = new IntByReference();
		ShortByReference retClass = new ShortByReference();
		ShortByReference retPrivileges = new ShortByReference();

		result = NotesNativeAPI.get().NSFDbGetObjectSize(
				hDb.getByValue(),
				m_workspaceObjId,
				NotesConstants.OBJECT_UNKNOWN,
				retSize,
				retClass,
				retPrivileges);
		NotesErrorUtils.checkResult(result);

		int size = retSize.getValue();

		DHANDLE.ByReference rethBuffer = DHANDLE.newInstanceByReference();

		result = NotesNativeAPI.get().NSFDbReadObject(
				hDb.getByValue(),
				m_workspaceObjId,
				0,
				size,
				rethBuffer);
		NotesErrorUtils.checkResult(result);

		//read the whole workspace obj: header + tabCount*tabdata + iconCount*icondata + some new stuff (probably hi-res icon data)
		byte[] workspaceData;

		Pointer ptr = Mem.OSLockObject(rethBuffer.getByValue());
		try {
			workspaceData = ptr.getByteArray(0, size);
		}
		finally {
			Mem.OSUnlockObject(rethBuffer.getByValue());
			result = Mem.OSMemFree(rethBuffer.getByValue());
			NotesErrorUtils.checkResult(result);
		}

		m_workspaceAllBuf = ByteBuffer.wrap(workspaceData).order(ByteOrder.nativeOrder());
		m_workspaceAllBuf.position(0);
		
		m_workspaceHeaderBuf = m_workspaceAllBuf.slice().order(ByteOrder.nativeOrder());
		m_workspaceHeaderBuf.limit(HEADERSIZE);
		
		//iconCount is a WORD, so that would allow 0xFFFF icons in total, which seems to be a lot
		int iconCount = m_workspaceHeaderBuf.getShort(POS_HEADER_ICONCOUNT) & 0xffff;

		//tabCount is a BYTE, so some more tabs than 32 should be possible
		int tabCount = m_workspaceHeaderBuf.get(POS_HEADER_TABCOUNT) & 0xff;

		//but the problem here is that stacked icons (e.g. local/remote replicas of the same DB) are
		//counted separately, so without breaking backward compatibility, it's pretty risky to
		//allow a lot more tabs
		
		for (int i=1; i<=tabCount; i++) {
			m_workspaceAllBuf.position(HEADERSIZE + (i-1) * TABSIZE);
			ByteBuffer tabDataBuf = m_workspaceAllBuf.slice().order(ByteOrder.nativeOrder());
			tabDataBuf.limit(TABSIZE);
			tabDataBuf.position(0);
			
			WorkspaceTab tab = createWorkspaceTab(tabDataBuf);
			m_tabs.add(tab);
		}

		m_workspaceAllBuf.position(HEADERSIZE + (tabCount) * TABSIZE);
		
		//buffer with iconCount*icondata (old icons) plus data of hi-res icons
		m_allIconDataBuf = m_workspaceAllBuf.slice().order(ByteOrder.nativeOrder());
		
		int iconSize = getIconStructureSize();
		
		int iconOffset = 0;
		List<ByteBuffer> icons = new ArrayList<>();
		
		int fileNameLength = getFileNameLength();
		int titleLength = getTitleLength();
		
		//read basic icon info (e.g. where it's located, low-res icon)
		for (int i=1; i<=iconCount; i++) {
			m_allIconDataBuf.position(iconOffset);
			ByteBuffer currIconBuf = m_allIconDataBuf.slice().order(ByteOrder.nativeOrder());
			currIconBuf.limit(iconSize);
			currIconBuf.position(0);
			
			WorkspaceIcon icon = createWorkspaceIcon(currIconBuf, fileNameLength, titleLength);
			m_icons.add(icon);
			
			icons.add(currIconBuf);
			
			iconOffset += iconSize;
		}

		//sort icons by tabindex / y / x / server / filename
		Collections.sort(m_icons, new Comparator<WorkspaceIcon>() {

			@Override
			public int compare(WorkspaceIcon o1, WorkspaceIcon o2) {
				int tabIndex1 = o1.getTabIndex();
				int tabIndex2 = o2.getTabIndex();
				
				if (tabIndex1 < tabIndex2) {
					return -1;
				}
				else if (tabIndex1 > tabIndex2) {
					return 1;
				}
				
				int y1 = o1.getPosY();
				int y2 = o2.getPosY();
				
				if (y1 < y2) {
					return -1;
				}
				else if (y1 > y2) {
					return 1;
				}
				
				int x1 = o1.getPosX();
				int x2 = o2.getPosX();
				
				if (x1 < x2) {
					return -1;
				}
				else if (x1 > x2) {
					return 1;
				}
				
				String server1 = o1.getServer();
				String server2 = o2.getServer();
				int c = server1.compareToIgnoreCase(server2);
				if (c!=0) {
					return c;
				}
				
				String fileName1 = o1.getFileName();
				String fileName2 = o2.getFileName();
				c = fileName1.compareToIgnoreCase(fileName2);
				if (c!=0) {
					return c;
				}
				
				return System.identityHashCode(o1) - System.identityHashCode(o2);
			}
			
		});
		
		//we might want to decode this data later on as well; this buffer contains hi-res icons in some format
		m_allIconDataBuf.position(iconOffset);
		if (m_allIconDataBuf.remaining()>0) {
			m_remainingIconDataBuf = m_allIconDataBuf.slice().order(ByteOrder.nativeOrder());
		}
		else {
			m_remainingIconDataBuf = null;
		}
	}
	
	protected WorkspaceTab createWorkspaceTab(ByteBuffer tabBuf) {
		return new WorkspaceTab(tabBuf);
	}
	
	protected WorkspaceIcon createWorkspaceIcon(ByteBuffer iconBuf, int fileNameLength, int titleLength) {
		return new WorkspaceIcon(iconBuf, fileNameLength, titleLength);
	}
	
	private int getIconStructureSize() {
		byte version = m_workspaceHeaderBuf.get(0);
		
		if (version == 0x8 || version == 0x9) { // r4
			return 828;
		}
		else if (version == 0xa || version == 0xb) { // r5/6 || r12.01
			return 1064;
		}
		else {
			//we need to be sure that newer Notes Clients keep a compatible format
			throw new IllegalStateException("Unsupported/unknown desktop version: "+(version & 0xff));
		}
	}

	private int getTitleLength() {
		byte version = m_workspaceHeaderBuf.get(0);
		
		if (version == 0x8 || version == 0x9) { // R4
			return 32;
		}
		else if (version == 0xa || version == 0xb) { // R5/6 || R12.01
			return 96;
		}
		else {
			//we need to be sure that newer Notes Clients keep a compatible format
			throw new IllegalStateException("Unsupported/unknown desktop version: "+(version & 0xff));
		}
	}

	private int getFileNameLength() {
		byte version = m_workspaceHeaderBuf.get(0);
		
		if (version == 0x8 || version == 0x9) { // r4
			return 100;
		}
		else if (version == 0xa || version == 0xb) { // r5/6 || r12.01
			return 256;
		}
		else {
			//we need to be sure that newer Notes Clients keep a compatible format
			throw new IllegalStateException("Unsupported/unknown desktop version: "+(version & 0xff));
		}
	}

	public static class WorkspaceTab implements IAdaptable {
		private ByteBuffer m_tabData;
		
		private WorkspaceTab(ByteBuffer tabData) {
			m_tabData = tabData;
		}
		
		protected ByteBuffer getBuffer() {
			return m_tabData;
		}

//		Tab content:
//		[00 00 00 00 00 00 04 00]   [........]
//		[74 61 62 31 00 00 00 00]   [tab1....]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]

		public String getTitle() {
			int tabTitleLen = m_tabData.get(6) & 0xff;
			byte[] tabTitleData = new byte[tabTitleLen];
			m_tabData.position(8);
			m_tabData.get(tabTitleData, 0, tabTitleLen);
			String tabTitle = NotesStringUtils.fromLMBCS(tabTitleData);
			return tabTitle;
		}
		
		public WorkspaceTab setTitle(String title) {
			Memory titleMem = NotesStringUtils.toLMBCS(title, false); // not null-terminated here
			int titleLen = (int) titleMem.size();
			if (titleLen > 32) {
				throw new IllegalArgumentException("Title exceeds max size of 32 bytes");
			}
			byte[] titleArr = titleMem.getByteArray(0, titleLen);
			m_tabData.put(6, (byte) (titleLen & 0xff));
			//overwrite old title
			m_tabData.position(8);
			m_tabData.put(new byte[32]);
			
			m_tabData.position(8);
			m_tabData.put(titleArr);
			
			return this;
		}

		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (ByteBuffer.class.equals(clazz)) {
				return (T) m_tabData;
			}
			return null;
		}

		@Override
		public String toString() {
			return "WorkspaceTab [title=" + getTitle() + "]";
		}
		
	}
	
	public class WorkspaceIcon implements IAdaptable  {
		private ByteBuffer m_iconData;
		private int m_fileNameLength;
		private int m_titleLength;

//		icon content:		
//		[6a 01 00 00 00 20 05 03]   [j.... ..]
//		[02 66 61 76 6f 72 69 74]   [.favorit]
//		[65 2e 6e 73 66 00 00 00]   [e.nsf...]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 46 61 76]   [.....Fav]
//		[6f 72 69 74 65 73 00 00]   [orites..]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 78 16]   [......x.]
//		[7e 00 d9 7d 25 80 00 00]   [~..}%...]
//		[02 20 20 01 00 00 fc 7f]   [.  .....]
//		[ff ff f8 3f ff ff f8 1f]   [...?....]
//		...
		
		private WorkspaceIcon(ByteBuffer iconData, int fileNameLength, int titleLength) {
			m_iconData = iconData;
			m_fileNameLength = fileNameLength;
			m_titleLength = titleLength;
		}
		
		protected ByteBuffer getBuffer() {
			return m_iconData;
		}
		
		private String getNetPath() {
			m_iconData.position(9);
			byte[] pathData = new byte[m_fileNameLength];
			m_iconData.get(pathData);
			
			return NotesStringUtils.fromLMBCS(pathData, -1);
		}
		
		private WorkspaceIcon setNetPath(String path) {
			Memory pathMem = NotesStringUtils.toLMBCS(path, true);
			if (pathMem.size() > m_fileNameLength) {
				throw new IllegalArgumentException(MessageFormat.format("Path exceeds the max length of {0} bytes.", m_fileNameLength));
			}
			m_iconData.position(9);
			byte[] pathData = pathMem.getByteArray(0, (int) pathMem.size());
			m_iconData.put(pathData);
			m_modified = true;
			return this;
		}
		
		public String getTitle() {
			m_iconData.position(9 + m_fileNameLength);
			byte[] titleData = new byte[m_titleLength];
			m_iconData.get(titleData);
			
			return NotesStringUtils.fromLMBCS(titleData, -1);
		}

		public WorkspaceIcon setTitle(String title) {
			Memory titleMem = NotesStringUtils.toLMBCS(title, true);
			if (titleMem.size() > m_titleLength) {
				throw new IllegalArgumentException(MessageFormat.format("Title exceeds the max length of {0} bytes.", m_titleLength));
			}
			m_iconData.position(9 + m_fileNameLength);
			byte[] titleData = titleMem.getByteArray(0, (int) titleMem.size());
			m_iconData.put(titleData);
			m_modified = true;
			return this;
		}

		public int getTabIndex() {
			return m_iconData.get(0x06) & 0xff;
		}
		
		public WorkspaceIcon setTabIndex(int idx) {
			m_iconData.put(0x06, (byte) (idx & 0xff));
			m_modified = true;
			return this;
		}
		
		public int getPosX() {
			return m_iconData.get(0x08) & 0xff;
		}
		
		public WorkspaceIcon setPosX(int x) {
			m_iconData.put(0x08, (byte) (x & 0xff));
			m_modified = true;
			return this;
		}
		
		public int getPosY() {
			return m_iconData.get(0x07) & 0xff;
		}
		
		public WorkspaceIcon setPosY(int y) {
			m_iconData.put(0x07, (byte) (y & 0xff));
			m_modified = true;
			return this;
		}
		
		public String getFileName() {
			String netPath = getNetPath();
			int iPos = netPath.indexOf("!!");
			return iPos==-1 ? netPath : netPath.substring(iPos+2);
		}
		
		public WorkspaceIcon setFileName(String name) {
			String server = getServer();
			String netPath = StringUtil.isEmpty(server) ? name : (server + "!!" + name);
			return setNetPath(netPath);
		}
		
		public String getServer() {
			String netPath = getNetPath();
			int iPos = netPath.indexOf("!!");
			return iPos==-1 ? "" : netPath.substring(0, iPos);
		}
		
		public WorkspaceIcon setServer(String server) {
			String serverCanonical = NotesNamingUtils.toCanonicalName(server);
			String fileName = getFileName();
			String netPath = StringUtil.isEmpty(serverCanonical) ? fileName : (serverCanonical + "!!" + fileName);
			return setNetPath(netPath);
		}
		
		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (ByteBuffer.class.equals(clazz)) {
				return (T) m_iconData;
			}
			return null;
		}

		@Override
		public String toString() {
			return "WorkspaceIcon [path=" + getNetPath() + ", title="
					+ getTitle() + ", tabindex=" + getTabIndex() + ", x=" + getPosX() + ", y="
					+ getPosY() + "]";
		}
		
	}

}