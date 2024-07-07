package com.mindoo.domino.jna;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.mindoo.domino.jna.constants.DBClass;
import com.mindoo.domino.jna.constants.NoteClass;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.internal.DisposableMemory;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.HANDLE;
import com.mindoo.domino.jna.richtext.FontStyle.StandardColors;
import com.mindoo.domino.jna.richtext.IRichTextNavigator;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.Pair;
import com.mindoo.domino.jna.utils.StringUtil;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Utility class for read and write support of the Notes client workspace.<br>
 * We provide functionality to
 * <ul>
 * <li>Read/write the workspace page titles and colors</li>
 * <li>Set workspace flags showUnreadMarks, showServerNames, stackDatabases</li>
 * <li>Read/write workspace icons: server, filename, title, position and pageindex</li>
 * <li>Read/write classic and modern (true color) icon image</li>
 * <li>Create new workspace icons, optionally copying a database's icon from the design (classic/modern format)</li>
 * <li>Create new workspace pages</li>
 * <li>Move workspace pages with all their icons</li>
 * </ul>
 * <u>Usage:</u><br>
 * Create a new {@link NotesWorkspace} instance and pass the {@link NotesDatabase} of the local
 * Notes desktop (e.g. desktop8.ndk) in the constructor.
 * Call the various methods to read and tweak the workspace configuration/content, then call {@link #store()}
 * 
 * @author Karsten Lehmann
 */
public class NotesWorkspace {
	//fixed structure sizes (icon structure depends on version)
	private static final int HEADERSIZE = 58;
	private static final int PAGESIZE = 40;
	
	//position of certain elements in the workspace header
	private static final int POS_HEADER_FLAGS = 0x06;
	private static final int POS_HEADER_ICONCOUNT = 0x0C;
	private static final int POS_HEADER_PAGECOUNT = 0x12;
	
	private static final int FLAGS_SHOWUNREADMARKS = 0x01;
	private static final int FLAGS_SHOWSERVERNAMES = 0x02;
	private static final int FLAGS_STACKDATABASES = 0x04;

	//32x32 pixels, 1 bit each for the transparency mask
	private static final int ICONMASK_SIZE = 128;
	//32x32 pixels, 4 bit each for the color index (0-15)
	private static final int ICONBITMAP_SIZE = 512;
	
	private NotesDatabase m_dbDesktop;
	private int m_workspaceObjId;

	private List<WorkspacePage> m_pages = new ArrayList<>();
	private List<WorkspaceIcon> m_icons = new ArrayList<>();
	
	//buffer with the workspace header
	protected ByteBuffer m_workspaceHeaderBuf;
	private boolean m_modified;
	
	private List<Integer> m_dbObjectsToFreeOnSave;
	
	/**
	 * Loads the Notes workspace info
	 * 
	 * @param dbDesktop desktop database, e.g. desktop8.ndk
	 */
	public NotesWorkspace(NotesDatabase dbDesktop) {
		m_dbDesktop = dbDesktop;
		m_dbObjectsToFreeOnSave = new ArrayList<>();
		init();
	}
	
	private void checkRecycled() {
		if (m_dbDesktop.isRecycled()) {
			throw new NotesError("Desktop database is recycled");
		}
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
	
	/**
	 * Sets the workspace change date
	 * 
	 * @param td new date
	 * @return this instance
	 */
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
	
	/**
	 * Returns a list of all workspace pages
	 * 
	 * @return pages
	 */
	public List<WorkspacePage> getPages() {
		return Collections.unmodifiableList(new ArrayList<>(m_pages));
	};

	/**
	 * Returns the first workspace page
	 * 
	 * @return first page
	 */
	public Optional<WorkspacePage> getFirstPage() {
		return m_pages.isEmpty() ? Optional.empty() : Optional.of(m_pages.get(0));
	}
	
	/**
	 * Returns the last workspace page
	 * 
	 * @return last page
	 */
	public Optional<WorkspacePage> getLastPage() {
		return m_pages.isEmpty() ? Optional.empty() : Optional.of(m_pages.get(m_pages.size()-1));
	}
	
	/**
	 * Finds a page by its name
	 * 
	 * @param title page title to search for
	 * @return page if found
	 */
	public Optional<WorkspacePage> getPage(String title) {
		return m_pages.stream().filter((page) -> { return title.equals(page.getTitle()); }).findFirst();
	}
	
	/**
	 * Returns all workspace icons
	 * 
	 * @return icons
	 */
	public List<WorkspaceIcon> getIcons() {
		return Collections.unmodifiableList(new ArrayList<>(m_icons));
	}
	
	/**
	 * Returns all workspace icons on the specified page
	 * 
	 * @param page page
	 * @return icons on page
	 */
	public List<WorkspaceIcon> getIcons(WorkspacePage page) {
		int idx = m_pages.indexOf(page);
		if (idx==-1) {
			return Collections.emptyList();
		}
		else {
			return m_icons
					.stream()
					.filter((icon) -> {
						return icon.getPageIndex() == idx;
					})
					.collect(Collectors.toList());
		}
	}
	
	public NotesDatabase getDatabase() {
		return m_dbDesktop;
	}
	
	@Override
	public String toString() {
		return "NotesWorkspace [database=" + m_dbDesktop + ", timedate=" + getWorkspaceTimeDate()
				+ ", showUnreadMarks=" + isShowUnreadMarks() + ", showServerNames=" + isShowServerNames()
				+ ", stackDatabases=" + isStackDatabases() + ", pages=" + getPages() + ", numberoficons="
				+ m_icons.size() + "]";
	}

	/**
	 * Stores the changes in the desktop database. This currently does not trigger
	 * any visual update in the Notes Client. Not sure if this can be achieved. For
	 * the meantime it is best to modify the workspace while the Notes Client is
	 * not running or still in its startup phase.
	 * 
	 * @return this workspace
	 */
	public NotesWorkspace store() {
		//assemble new workspace data
		int workspaceSize = HEADERSIZE +
				m_pages.size() * PAGESIZE +
				m_icons.size() * getIconStructureSize();
		ByteBuffer newWorkspaceBuf = ByteBuffer.allocate(workspaceSize)
				.order(ByteOrder.nativeOrder());

		//update icon and page count in header
		m_workspaceHeaderBuf.putShort(POS_HEADER_ICONCOUNT, (short) (m_icons.size() & 0xffff));
		m_workspaceHeaderBuf.put(POS_HEADER_PAGECOUNT, (byte) (m_pages.size() & 0xff));
		
		//write header
		m_workspaceHeaderBuf.position(0);
		newWorkspaceBuf.put(m_workspaceHeaderBuf);
		
		//write pages
		for (int i=0; i<m_pages.size(); i++) {
			WorkspacePage currPage = m_pages.get(i);
			ByteBuffer currPageBuf = currPage.m_pageBuf;
			currPageBuf.position(0);
			newWorkspaceBuf.put(currPageBuf);
		}
		
		//write icons
		for (int i=0; i<m_icons.size(); i++) {
			WorkspaceIcon currIcon = m_icons.get(i);
			ByteBuffer currIconBuf = currIcon.m_iconBuf;
			currIconBuf.position(0);
			newWorkspaceBuf.put(currIconBuf);
		}
		
		if (m_dbDesktop.isRecycled()) {
			throw new NotesError("Desktop database already recycled");
		}
		
		final DHANDLE.ByReference retCopyBufferHandle = DHANDLE.newInstanceByReference();
		short result = Mem.OSMemAlloc((short) 0, workspaceSize, retCopyBufferHandle);
		NotesErrorUtils.checkResult(result);

		try {
			//write data into copy buffer
			Pointer ptrBuffer = Mem.OSLockObject(retCopyBufferHandle);
			try {
				byte[] newWorkspaceData = new byte[workspaceSize];
				newWorkspaceBuf.position(0);
				newWorkspaceBuf.get(newWorkspaceData);
				ptrBuffer.write(0, newWorkspaceData, 0, workspaceSize);
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
			if (retSize.getValue() != workspaceSize) {
				result = NotesNativeAPI.get().NSFDbReallocObject(hDbByVal, m_workspaceObjId, workspaceSize);
				NotesErrorUtils.checkResult(result);
			}

			//write copy buffer into NSF object
			result = NotesNativeAPI.get().NSFDbWriteObject(
					hDbByVal,
					m_workspaceObjId,
					retCopyBufferHandle.getByValue(),
				0,
				workspaceSize);
			NotesErrorUtils.checkResult(result);
			
			Set<Integer> deletedHandles = new HashSet<>();
			
			for (int objId : m_dbObjectsToFreeOnSave) {
				if (!deletedHandles.contains(objId)) {
					try {
						NotesDatabaseObject dbObj = new NotesDatabaseObject(m_dbDesktop, objId);
						dbObj.delete();
						deletedHandles.add(objId);
					}
					catch (Exception e) {
						//ignore
					}
				}
				
			}
			m_dbObjectsToFreeOnSave.clear();
		}
		finally {
			//free copy buffer
			Mem.OSMemFree(retCopyBufferHandle.getByValue());
		}
		
		m_modified = false;
		return this;
	}

	/**
	 * Loads the workspace structures
	 */
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

		//read the whole workspace obj: header + pageCount*pagedata + iconCount*icondata
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

		ByteBuffer workspaceAllBuf = ByteBuffer.wrap(workspaceData).order(ByteOrder.nativeOrder());
		workspaceAllBuf.position(0);
		
		byte[] workspaceHeaderData = new byte[HEADERSIZE];
		workspaceAllBuf.get(workspaceHeaderData);
		m_workspaceHeaderBuf = ByteBuffer.wrap(workspaceHeaderData).order(ByteOrder.nativeOrder());
		
		//iconCount is a WORD, so that would allow 0xFFFF icons in total, which seems to be a lot
		int iconCount = m_workspaceHeaderBuf.getShort(POS_HEADER_ICONCOUNT) & 0xffff;

		//pageCount is a BYTE, so some more pages than 32 should be possible
		int pageCount = m_workspaceHeaderBuf.get(POS_HEADER_PAGECOUNT) & 0xff;

		//but the problem here is that stacked icons (e.g. local/remote replicas of the same DB) are
		//counted separately, so without breaking backward compatibility, it's pretty risky to
		//allow a lot more pages
		
		for (int i=0; i<pageCount; i++) {
			workspaceAllBuf.position(HEADERSIZE + i * PAGESIZE);
			byte[] pageData = new byte[PAGESIZE];
			workspaceAllBuf.get(pageData);
			ByteBuffer pageDataBuf = ByteBuffer.wrap(pageData).order(ByteOrder.nativeOrder());
			
			WorkspacePage page = createWorkspacePage(pageDataBuf);
			m_pages.add(page);
		}

		workspaceAllBuf.position(HEADERSIZE + pageCount * PAGESIZE);
		
		//buffer with iconCount*icondata (old icons) plus data of hi-res icons
		ByteBuffer allIconDataBuf = workspaceAllBuf.slice().order(ByteOrder.nativeOrder());
		
		int iconSize = getIconStructureSize();
		
		int iconOffset = 0;
		List<ByteBuffer> icons = new ArrayList<>();
		
		int fileNameLength = getFileNameLength();
		int titleLength = getTitleLength();
		
		//read basic icon info (e.g. where it's located, low-res icon)
		for (int i=0; i<iconCount; i++) {
			allIconDataBuf.position(iconOffset);
			
			byte[] iconData = new byte[iconSize];
			allIconDataBuf.get(iconData);
			ByteBuffer currIconBuf = ByteBuffer.wrap(iconData).order(ByteOrder.nativeOrder());
			currIconBuf.position(0);
			
			WorkspaceIcon icon = createWorkspaceIcon(allIconDataBuf.position(), currIconBuf, fileNameLength, titleLength);
			m_icons.add(icon);
			
			icons.add(currIconBuf);
			
			iconOffset += iconSize;
		}
	}
	
	protected WorkspacePage createWorkspacePage(ByteBuffer pageBuf) {
		return new WorkspacePage(pageBuf);
	}
	
	protected WorkspaceIcon createWorkspaceIcon(int iconDataOffset, ByteBuffer iconBuf, int fileNameLength, int titleLength) {
		return new WorkspaceIcon(iconBuf, fileNameLength, titleLength);
	}
	
	public enum WorkspaceVersion {R4, R5_6, R12 }
	
	public WorkspaceVersion getWorkspaceVersion() {
		byte version = m_workspaceHeaderBuf.get(0);
		
		if (version == 0x8 || version == 0x9) { // r4
			return WorkspaceVersion.R4;
		}
		else if (version == 0xa) { // r5/6
			return WorkspaceVersion.R5_6;
		}
		else if (version == 0xb) { // r12.0.1
			return WorkspaceVersion.R12;
		}
		else {
			//we need to be sure that newer Notes Clients keep a compatible format
			throw new IllegalStateException("Unsupported/unknown desktop version: "+(version & 0xff));
		}
	}
	
	private int getIconStructureSize() {
		switch (getWorkspaceVersion()) {
		case R4:
			return 828;
		case R5_6:
		case R12:
		default:
			return 1064;
		}
	}

	private int getTitleLength() {
		switch (getWorkspaceVersion()) {
		case R4:
			return 32;
		case R5_6:
		case R12:
		default:
			return 96;
		}
	}

	private int getFileNameLength() {
		switch (getWorkspaceVersion()) {
		case R4:
			return 100;
		case R5_6:
		case R12:
		default:
			return 256;
		}
	}

	/**
	 * A single page of the workspace
	 * 
	 * @author Karsten Lehmann
	 */
	public class WorkspacePage implements IAdaptable {
		private ByteBuffer m_pageBuf;
		
		private WorkspacePage(ByteBuffer pageData) {
			m_pageBuf = pageData;
		}
		
		protected ByteBuffer getBuffer() {
			return m_pageBuf;
		}

		/**
		 * Returns the index of this page (0=first page)
		 * 
		 * @return index or -1 if page has been removed from workspace
		 */
		public int getPageIndex() {
			return getPages().indexOf(this);
		}
		
		/**
		 * Returns all icons on this page
		 * 
		 * @return icons
		 */
		public List<WorkspaceIcon> getIcons() {
			int pageIdx = getPageIndex();
			if (pageIdx==-1) {
				return Collections.emptyList();
			}
			
			return m_icons
					.stream()
					.filter((icon) -> {
						return pageIdx == icon.getPageIndex();
					})
					.collect(Collectors.toList());
		}
		
		/**
		 * Returns all icons on this page at the specified position
		 * 
		 * @param x x position (0 = first column)
		 * @param y y position (0 = first row)
		 * @return icons
		 */
		public List<WorkspaceIcon> getIconsAtPosition(int x, int y) {
			int pageIdx = getPageIndex();
			if (pageIdx==-1) {
				return Collections.emptyList();
			}
			
			return getIcons()
					.stream()
					.filter((icon) -> {
						return pageIdx == icon.getPageIndex() && x == icon.getPosX() && y == icon.getPosY();
					})
					.collect(Collectors.toList());
		}
		
//		Page content:
//		[00 00 00 00 00 00 04 00]   [........]
//		[74 61 62 31 00 00 00 00]   [tab1....]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]
//		[00 00 00 00 00 00 00 00]   [........]

		public String getTitle() {
			int pageTitleLen = m_pageBuf.get(6) & 0xff;
			byte[] pageTitleData = new byte[pageTitleLen];
			m_pageBuf.position(8);
			m_pageBuf.get(pageTitleData, 0, pageTitleLen);
			String pageTitle = NotesStringUtils.fromLMBCS(pageTitleData);
			return pageTitle;
		}
		
		public WorkspacePage setTitle(String title) {
			Memory titleMem = NotesStringUtils.toLMBCS(title, false); // not null-terminated here
			int titleLen = (int) titleMem.size();
			if (titleLen > 32) {
				throw new IllegalArgumentException(MessageFormat.format("Title exceeds max size of 32 bytes: {0}", title));
			}
			byte[] titleArr = titleMem.getByteArray(0, titleLen);
			m_pageBuf.put(6, (byte) (titleLen & 0xff));
			//overwrite old title
			m_pageBuf.position(8);
			m_pageBuf.put(new byte[32]);
			
			m_pageBuf.position(8);
			m_pageBuf.put(titleArr);
			m_modified = true;
			
			return this;
		}

		/**
		 * Returns the color of the page
		 * 
		 * @return color
		 */
		public Color getColor() {
			int idx = getColorIndex();
			if (idx < pagePaletteColors.length) {
				return pagePaletteColors[idx];
			}
			return Color.BLACK;
		}
		
		/**
		 * Returns the current page color palette index
		 * 
		 * @return index
		 * @see NotesWorkspace#getAvailablePageColors()
		 */
		public int getColorIndex() {
			return Byte.toUnsignedInt(m_pageBuf.get(2));
		}
		
		/**
		 * Changes the page color
		 * 
		 * @param color new color index in palette
		 * @return this page
		 * @see NotesWorkspace#getAvailablePageColors()
		 */
		public WorkspacePage setColorIndex(int color) {
			if (color<0 || color>=255) {
				throw new IllegalArgumentException(MessageFormat.format("Color index must be between 0 and 255: {0}", color));
			}
			
			m_pageBuf.put(2, (byte) (color & 0xff));
			m_modified = true;
			return this;
		}
		
		/**
		 * Finds the nearest palette color for the given {@link StandardColors} in the palette
		 * and sets it for the page
		 * 
		 * @param stdColor color to set
		 * @return this page
		 */
		public WorkspacePage setColor(StandardColors stdColor) {
			int idx = findNearestPageColor(stdColor);
			return setColorIndex(idx);
		}

		/**
		 * Finds the nearest palette color for the given {@link Color} in the palette
		 * and sets it for the page
		 * 
		 * @param color color to set
		 * @return this page
		 */
		public WorkspacePage setColor(Color color) {
			int idx = findNearestTabColor(color.getRed(), color.getGreen(), color.getBlue());
			return setColorIndex(idx);
		}

		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (ByteBuffer.class.equals(clazz)) {
				return (T) m_pageBuf;
			}
			return null;
		}

		@Override
		public String toString() {
			return "WorkspacePage [title=" + getTitle() + ", color="+getColor()+ "]";
		}
		
		/**
		 * Adds an icon to the workspace page
		 * 
		 * @param db database to add the icon for
		 * @param x x position
		 * @param y y position
		 * @return the new icon
		 */
		public WorkspaceIcon addIcon(NotesDatabase db, int x, int y) {
			WorkspaceIcon icon = addIcon(db.getTitle(), db.getServer(), db.getRelativeFilePath(), db.getReplicaID(), x, y);
			icon.setIconFrom(db);
			return icon;
		}
		
		/**
		 * Adds an icon to the workspace page
		 * 
		 * @param title database title
		 * @param server database server
		 * @param filePath database filepath
		 * @param replicaId optional database replicaid or null/empty string
		 * @param x x position
		 * @param y y position
		 * @return the new icon
		 */
		public WorkspaceIcon addIcon(String title, String server, String filePath,
				String replicaId, int x, int y) {
			
			int iconSize = getIconStructureSize();
			int fileNameLength = getFileNameLength();
			int titleLength = getTitleLength();
			
			ByteBuffer buf = ByteBuffer.allocate(iconSize).order(ByteOrder.nativeOrder());
			WorkspaceIcon icon = new WorkspaceIcon(buf, fileNameLength, titleLength);
			icon.init();
			icon.setTitle(title);
			icon.setServer(server);
			icon.setFilePath(filePath);
			if (!StringUtil.isEmpty(replicaId)) {
				icon.setReplicaID(replicaId);
			}
			
			icon.move(this, x, y);
			m_icons.add(icon);
			m_modified=true;
			return icon;
		}
		
		/**
		 * Moves a page within the workspace with all its icons
		 * 
		 * @param targetPageTitle title of new page position
		 * @param addBefore true to add before targetPage
		 * @return this workspace page
		 * @throws Throwable if target page could not be found
		 */
		public WorkspacePage movePage(String targetPageTitle,
				boolean addBefore) throws Throwable {
			WorkspacePage targetPage = NotesWorkspace.this.getPage(targetPageTitle).orElseThrow(() -> {
				throw new IllegalArgumentException(MessageFormat.format("No page found with title: {0}", targetPageTitle));
			});
			
			return movePage(targetPage, addBefore);
		}
		
		/**
		 * Moves a page within the workspace with all its icons
		 * 
		 * @param targetPage new page position
		 * @param addBefore true to add before targetPage
		 * @return this workspace page
		 */
		public WorkspacePage movePage(WorkspacePage targetPage,
				boolean addBefore) {
			
			NotesWorkspace.this.movePage(this, targetPage, addBefore);
			return this;
		}
	}
	
	public class WorkspaceIcon implements IAdaptable  {
		private static final int ICON_MASK_POS_R5 = 0x17a;
		private static final int ICON_MASK_POS_R4 = 0x09e;
		private static final int ICON_BITMAP_POS_R5 = 0x1fa;
		private static final int ICON_BITMAP_POS_R4 = 0x11e;
		private ByteBuffer m_iconBuf;
		private int m_fileNameLength;
		private int m_titleLength;
		
		private WorkspaceIcon(ByteBuffer iconData, int fileNameLength, int titleLength) {
			m_iconBuf = iconData;
			m_fileNameLength = fileNameLength;
			m_titleLength = titleLength;
		}
		
		protected ByteBuffer getBuffer() {
			return m_iconBuf;
		}
		
		private String getNetPath() {
			m_iconBuf.position(9);
			byte[] pathData = new byte[m_fileNameLength];
			m_iconBuf.get(pathData);
			
			return NotesStringUtils.fromLMBCS(pathData, -1);
		}
		
		private WorkspaceIcon setNetPath(String path) {
			Memory pathMem = NotesStringUtils.toLMBCS(path, true);
			if (pathMem.size() > m_fileNameLength) {
				throw new IllegalArgumentException(MessageFormat.format("Path exceeds the max length of {0} bytes.", m_fileNameLength));
			}
			m_iconBuf.position(9);
			byte[] pathData = pathMem.getByteArray(0, (int) pathMem.size());
			m_iconBuf.put(pathData);
			m_modified = true;
			return this;
		}
		
		public int getAdditionalDataRRV() {
			return m_iconBuf.getInt(0);
		}

		private void setAdditionalDataRRV(int val) {
			m_iconBuf.putInt(0, val);
			m_modified = true;
		}
		
		/**
		 * Clears any cache data associated with the workspace icon (probably the design cache)
		 */
		public void clearAdditionalData() {
			int objId = getAdditionalDataRRV();
			if (objId!=0) {
				try {
					NotesDatabaseObject dbObj = new NotesDatabaseObject(m_dbDesktop, objId);
					dbObj.delete();
				}
				catch (NotesError e) {
					short status = (short) (e.getId() & NotesConstants.ERR_MASK);
					if (status!=551) {
						throw e;
					}
				}
				setAdditionalDataRRV(0);
			}
		}
		
		public ByteBuffer getAdditionalData() {
			int objPtr = getAdditionalDataRRV();
			if (objPtr==0) {
				return null;
			}
			
			HANDLE hDb = m_dbDesktop.getHandle();

			IntByReference retSize = new IntByReference();
			ShortByReference retClass = new ShortByReference();
			ShortByReference retPrivileges = new ShortByReference();

			short result = NotesNativeAPI.get().NSFDbGetObjectSize(
					hDb.getByValue(),
					objPtr,
					NotesConstants.OBJECT_UNKNOWN,
					retSize,
					retClass,
					retPrivileges);
			short status = (short) (result & NotesConstants.ERR_MASK);
			if (status == 1537) { // invalid on disk structure, probably invalid object id
				return null;
			}
			NotesErrorUtils.checkResult(result);

			int size = retSize.getValue();

			DHANDLE.ByReference rethBuffer = DHANDLE.newInstanceByReference();

			result = NotesNativeAPI.get().NSFDbReadObject(
					hDb.getByValue(),
					objPtr,
					0,
					size,
					rethBuffer);
			NotesErrorUtils.checkResult(result);

			//read the whole workspace obj: header + pageCount*tabdata + iconCount*icondata
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

			ByteBuffer iconDataBuf = ByteBuffer.wrap(workspaceData).order(ByteOrder.nativeOrder());
			iconDataBuf.position(0);

			byte[] workspaceHeaderData = new byte[HEADERSIZE];
			iconDataBuf.get(workspaceHeaderData);

			return iconDataBuf;
		}
		
		public boolean isOnTopOfStack() {
			return isStackDatabases() && (getFlags() & 0x40) == 0x00;
		}
		
		public WorkspaceIcon moveToTopOfStack() {
			if (!isStackDatabases()) {
				throw new IllegalStateException("Workspace icons are not stacked");
			}
			
			WorkspaceIcon thisIcon = this;
			
			String replicaId = getReplicaID();
			
			if (!StringUtil.isEmpty(replicaId)) {
				getIcons()
				.stream()
				.filter((icon) -> {
					return !thisIcon.equals(icon) && replicaId.equals(icon.getReplicaID());
				})
				.forEach((icon) -> {
					short oldFlags = icon.getFlags();
					if ((oldFlags & 0x0040) != 0x0040) {
						short newFlags = (short) ((oldFlags | 0x0040) & 0xffff);
						icon.setFlags(newFlags);
					}
				});
			}
			
			short newFlags = (short) ((getFlags() & ~0x0040) & 0xffff);
			setFlags(newFlags);
			return this;
		}
		
		public short getFlags() {
			return m_iconBuf.getShort(4);
		}
		
		public WorkspaceIcon setFlags(short flags) {
			short oldFlags = getFlags();
			if (oldFlags!=flags) {
				m_iconBuf.putShort(4, flags);
				m_modified = true;
			}
			return this;
		}
		
		public short getFlags2() {
			switch (getWorkspaceVersion()) {
			case R4:
				return m_iconBuf.getShort(0x336);
			case R5_6:
			case R12:
			default:
				return m_iconBuf.getShort(0x412);
			}
		}
		
		public WorkspaceIcon setFlags2(short flags) {
			short oldFlags2 = getFlags2();
			if (oldFlags2!=flags) {
				switch (getWorkspaceVersion()) {
				case R4:
					m_iconBuf.putShort(0x336, flags);
				case R5_6:
				case R12:
				default:
					m_iconBuf.putShort(0x412, flags);
				}
				m_modified = true;
			}
			return this;
		}
		
		public String getTitle() {
			m_iconBuf.position(9 + m_fileNameLength);
			byte[] titleData = new byte[m_titleLength];
			m_iconBuf.get(titleData);
			
			return NotesStringUtils.fromLMBCS(titleData, -1);
		}

		public WorkspaceIcon setTitle(String title) {
			Memory titleMem = NotesStringUtils.toLMBCS(title, true);
			if (titleMem.size() > m_titleLength) {
				throw new IllegalArgumentException(MessageFormat.format("Title exceeds the max length of {0} bytes.", m_titleLength));
			}
			m_iconBuf.position(9 + m_fileNameLength);
			byte[] titleData = titleMem.getByteArray(0, (int) titleMem.size());
			m_iconBuf.put(titleData);
			m_modified = true;
			return this;
		}

		/**
		 * Returns the index of the page that contains the icon
		 * 
		 * @return index
		 */
		public int getPageIndex() {
			return m_iconBuf.get(0x06) & 0xff;
		}
		
		/**
		 * Returns the workspace page an icon belongs to
		 * 
		 * @return page if icon is part of the workspace
		 */
		public Optional<WorkspacePage> getPage() {
			int idx = getPageIndex();
			if (idx < m_pages.size()) {
				return Optional.of(m_pages.get(idx));
			}
			else {
				return Optional.empty();
			}
		}
		
		/**
		 * Returns the x coordinate of the icon
		 * 
		 * @return 0 based x coordinate
		 */
		public int getPosX() {
			return m_iconBuf.get(0x08) & 0xff;
		}
		
		/**
		 * Returns the y coordinate of the icon
		 * 
		 * @return 0 based y coordinate
		 */
		public int getPosY() {
			return m_iconBuf.get(0x07) & 0xff;
		}

		/**
		 * Moves the icon in the workspace
		 * 
		 * @param page target page
		 * @param x target x position
		 * @param y target y position
		 * @return this instance
		 * @throws IconPositionNotAvailable
		 */
		public WorkspaceIcon move(WorkspacePage page, int x, int y) throws IconPositionNotAvailable {
			int targetPageIdx = m_pages.indexOf(page);
			if (targetPageIdx==-1) {
				throw new IllegalArgumentException(MessageFormat.format("Page is not part of the workspace: {0}",page));
			}
			
			boolean isStacked = isStackDatabases();
			String ourReplicaID = getReplicaID();
			
			//find icons that exist at the target position
			WorkspaceIcon iconsAtPos = getIcons()
			.stream()
			.filter((icon) -> {
				if (isStacked) {
					return targetPageIdx == icon.getPageIndex() &&
							x == icon.getPosX() &&
							y == icon.getPosY() &&
							!ourReplicaID.equals(icon.getReplicaID());
				}
				else {
					return targetPageIdx == icon.getPageIndex() &&
							x == icon.getPosX() &&
							y == icon.getPosY();
				}
			})
			.findFirst()
			.orElse(null);
			
			if (iconsAtPos!=null) {
				if (isStacked) {
					throw new IconPositionNotAvailable(MessageFormat.format("The position (x="+x+", y="+y+
							") already contains an icon of a different database: {0}", iconsAtPos));
				}
				else {
					throw new IconPositionNotAvailable(MessageFormat.format("The position (x="+x+", y="+y+
							") already contains an icon: {0}", iconsAtPos));
				}
			}
			
			return moveSafe(targetPageIdx, x, y);
		}
		
		/**
		 * Changes the icon position without checks
		 * 
		 * @param pageIdx new page index
		 * @param x new x position
		 * @param y new y position
		 * @return this icon
		 */
		private WorkspaceIcon moveSafe(int pageIdx, int x, int y) {
			m_iconBuf.put(0x06, (byte) (pageIdx & 0xff));
			m_iconBuf.put(0x08, (byte) (x & 0xff));
			m_iconBuf.put(0x07, (byte) (y & 0xff));
			m_modified = true;
			return this;
		}
		
		public String getFilePath() {
			String netPath = getNetPath();
			int iPos = netPath.indexOf("!!");
			return iPos==-1 ? netPath : netPath.substring(iPos+2);
		}
		
		public WorkspaceIcon setFilePath(String path) {
			String server = getServer();
			String netPath = StringUtil.isEmpty(server) ? path : (server + "!!" + path);
			return setNetPath(netPath);
		}
		
		public String getServer() {
			String netPath = getNetPath();
			int iPos = netPath.indexOf("!!");
			return iPos==-1 ? "" : netPath.substring(0, iPos);
		}
		
		public WorkspaceIcon setServer(String server) {
			String serverCanonical = NotesNamingUtils.toCanonicalName(server);
			String fileName = getFilePath();
			String netPath = StringUtil.isEmpty(serverCanonical) ? fileName : (serverCanonical + "!!" + fileName);
			return setNetPath(netPath);
		}
		
 		public String getReplicaID() {
			m_iconBuf.position(9 + m_fileNameLength + m_titleLength + 1);
			int innards0 = m_iconBuf.getInt();
			int innards1 = m_iconBuf.getInt();
			return NotesStringUtils.innardsToReplicaId(new int[] {innards0, innards1});
		}

		public WorkspaceIcon setReplicaID(String repId) {
			int[] innards = NotesStringUtils.replicaIdToInnards(repId);
			m_iconBuf.position(9 + m_fileNameLength + m_titleLength + 1);
			m_iconBuf.putInt(innards[0]);
			m_iconBuf.putInt(innards[1]);
			m_modified = true;
			return this;
		}

		public int getUnread() {
			m_iconBuf.position(9 + m_fileNameLength + m_titleLength + 9);
			return Short.toUnsignedInt(m_iconBuf.getShort());
		}

		public int getTimestamp() {
			switch (getWorkspaceVersion()) {
			case R4:
			{
				return m_iconBuf.getInt(0x336);
			}
			default:
			{
				return m_iconBuf.getInt(0x40E);
			}
			}
		}
		
		public WorkspaceIcon setTimestamp(int val) {
			switch (getWorkspaceVersion()) {
			case R4:
			{
				m_iconBuf.putInt(0x336, val);
			}
			default:
			{
				m_iconBuf.putInt(0x40E, val);
			}
			}
			return this;
		}
		
		/**
		 * Removes the icon image (classic and true color)
		 * 
		 * @return this icon
		 */
		public WorkspaceIcon clearIcon() {
			byte[] iconMaskArr = new byte[ICONMASK_SIZE];
			Arrays.fill(iconMaskArr, (byte) 255);
			byte[] iconBitmapArr = new byte[ICONBITMAP_SIZE];

			setIconMask(ByteBuffer.wrap(iconMaskArr));
			setIconBitmap(ByteBuffer.wrap(iconBitmapArr));
			
			int trueColorObjId = getTrueColorIconObjectId();
			if (trueColorObjId!=0) {
				m_dbObjectsToFreeOnSave.add(trueColorObjId);
				setTrueColorIconObjectId(0);
			}
			m_modified = true;
			return this;
		}

		//extracted standard workspace icon (blue Domino logo)
		final byte[] initialIconR5 = new byte[] {
				(byte) 0xae, 0x76, 0x00, 0x00, 0x00, 0x30, 0x05, 0x01, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1b, 0x5c, 0x79, 0x00, 0x51, (byte) 0x88, 0x25, (byte) 0xc1, 0x00, 0x00, 0x02, 0x20, 0x20, 0x01, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xf0,
				0x0f, (byte) 0xff, (byte) 0xff, (byte) 0xe0, 0x07, (byte) 0xff, (byte) 0xff, (byte) 0xc0, 0x03, (byte) 0xff, (byte) 0xfe, 0x00, 0x00, 0x7f, (byte) 0xfc, 0x00, 0x00, 0x3f, (byte) 0xf8, 0x00, 0x00, 0x1f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00,
				0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xe0, 0x00, 0x00, 0x07, (byte) 0xc0, 0x00, 0x00, 0x03, (byte) 0x80, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x01, (byte) 0xc0, 0x00, 0x00, 0x03, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf0, 0x00,
				0x00, 0x0f, (byte) 0xf0, 0x00, 0x00, 0x0f, (byte) 0xf8, 0x00, 0x00, 0x1f, (byte) 0xfc, 0x00, 0x00, 0x3f, (byte) 0xff, (byte) 0xf0, 0x0f, (byte) 0xff, (byte) 0xff, (byte) 0xf8, 0x1f, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f,
				0x11, 0x11, 0x11, 0x11, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, 0x1d, (byte) 0xdd, (byte) 0xdd, (byte) 0xd1, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, (byte) 0xff, (byte) 0xf1,
				(byte) 0xdd, 0x77, 0x77, (byte) 0xdd, 0x1f, (byte) 0xff, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, 0x11, 0x11, (byte) 0xd7, 0x77, 0x77, 0x7d, 0x11, 0x11, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x11, (byte) 0xdd, (byte) 0xdd,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xdd, (byte) 0xdd, 0x11, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, 0x1d, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, (byte) 0xd1, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x0f, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, (byte) 0xf0, 0x00, 0x00, (byte) 0xf1, 0x11, (byte) 0xd7, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x11, 0x1f, 0x00, 0x0f, 0x11, (byte) 0xdd, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, (byte) 0xdd, 0x11, (byte) 0xf0, (byte) 0xf1, 0x1d, 0x77, 0x77, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x77, (byte) 0xd1, 0x1f, (byte) 0xf1, (byte) 0xd7, 0x77, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x77, 0x7d, 0x1f, (byte) 0xf1, (byte) 0xd7, 0x77, 0x77, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x77, 0x7d, 0x1f, (byte) 0xf1, (byte) 0xd7, 0x77, 0x77, 0x77, (byte) 0xd1, (byte) 0xd7, 0x77, 0x77, 0x7d, 0x1d, 0x77, 0x77, 0x77, 0x7d, 0x1f, (byte) 0xf1, 0x1d, (byte) 0xdd, (byte) 0xdd, (byte) 0xdd, 0x11,
				(byte) 0xd7, 0x77, 0x77, 0x7d, 0x11, (byte) 0xdd, (byte) 0xdd, (byte) 0xdd, (byte) 0xd1, 0x1f, 0x0f, 0x11, 0x11, 0x1d, (byte) 0xdd, (byte) 0xd1, 0x1d, (byte) 0xdd, (byte) 0xdd, (byte) 0xd1, 0x1d, (byte) 0xdd, (byte) 0xd1, 0x11, 0x11, (byte) 0xf0, 0x00, (byte) 0xff, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d,
				0x11, (byte) 0xdd, (byte) 0xdd, 0x11, (byte) 0xd7, 0x77, 0x7d, 0x1f, (byte) 0xff, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d, 0x1d, 0x77, 0x77, (byte) 0xd1, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d,
				(byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, (byte) 0xd7, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x77, 0x7d, (byte) 0xd7, 0x77, 0x7d, 0x1f, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, 0x1d, (byte) 0xdd, (byte) 0xd1,
				(byte) 0xd7, 0x77, 0x77, 0x7d, 0x1d, (byte) 0xdd, (byte) 0xd1, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x0f, 0x11, 0x11, 0x11, 0x1d, 0x77, 0x77, (byte) 0xd1, 0x11, 0x11, 0x11, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff,
				0x11, (byte) 0xdd, (byte) 0xdd, 0x11, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xf1, 0x11, 0x11, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x0f, (byte) 0xff, (byte) 0xff, (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x70, (byte) 0xc8, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x45, 0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};
						
		private void init() {
			if (NotesWorkspace.this.getWorkspaceVersion() != WorkspaceVersion.R4) {
				//initial icon currently only working for R5+, need to copy icon data from r4
				m_iconBuf.position(0);
				m_iconBuf.put(initialIconR5);
				setAdditionalDataRRV(0);
			}
			
			//replica id
			m_iconBuf.position(9 + m_fileNameLength + m_titleLength + 1);
			m_iconBuf.putInt(0);
			m_iconBuf.putInt(0);
			
			//flags
			m_iconBuf.putShort(4, (short) 0x2400);
			
			//unread count
			m_iconBuf.putShort(9 + m_fileNameLength + m_titleLength + 9, (short) 0xffff);

			WorkspaceVersion version = getWorkspaceVersion();
			
			switch (version) {
			case R4:
				//flags2
				m_iconBuf.putShort(0x336, (short) 0x8000);
				//timestamp
				m_iconBuf.putInt(0x332, 0x01494cb6);
				break;
			case R5_6:
			case R12:
			default:
				//flags2
				m_iconBuf.putShort(0x412, (short) 0x8000);
				//timestamp
				m_iconBuf.putInt(0x40E, 0x01494cb6);
			}
		}
		
		/**
		 * Returns the icon transparency information
		 * 
		 * @return icon mask (128 byte, 32x32 pixel, 1 bit per pixel)
		 */
		public ByteBuffer getIconMask() {
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				//icon mask
				m_iconBuf.position(0x09e);
			}
			case R5_6:
			case R12:
			default:
			{
				//iconmask
				m_iconBuf.position(0x17a);
			}
			}

			ByteBuffer iconMaskBuf = m_iconBuf.slice().order(ByteOrder.nativeOrder());
			iconMaskBuf.limit(ICONMASK_SIZE);
			iconMaskBuf = iconMaskBuf.duplicate();
			iconMaskBuf.position(0);
			return iconMaskBuf;
		}
		
		/**
		 * Returns the workspace icon bitmap, 32x32 pixels, eacb pixel encoded as 4 bit (16 colors).
		 * 
		 * @return bitmap
		 */
		public ByteBuffer getIconBitmap() {
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				//icon bitmap
				m_iconBuf.position(0x11e);
			}
			case R5_6:
			case R12:
			default:
			{
				//icon bitmap
				m_iconBuf.position(0x1fa);
			}
			}

			byte[] data = new byte[ICONBITMAP_SIZE];
			m_iconBuf.get(data);
			ByteBuffer iconBitmapBuf = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
			iconBitmapBuf.position(0);
			return iconBitmapBuf;
		}
		
		/**
		 * Returns the DB icon transparency information
		 * 
		 * @param x x coordinate
		 * @param y y coornidate
		 * @return true if transparent
		 */
		public boolean getIconMask(int x, int y) {
			checkRecycled();
			
			int offset = (31-y)*4 + (x >> 3);
			int mod = x % 8;

			WorkspaceVersion version = getWorkspaceVersion();
			int positionOffset;

			byte b;
			switch (version) {
			case R4:
			{
				positionOffset = ICON_MASK_POS_R4 + offset;
			}
			case R5_6:
			case R12:
			default:
			{
				positionOffset = ICON_MASK_POS_R5 + offset;
			}
			}

			b = m_iconBuf.get(positionOffset);

			switch (mod) {
			case 0:
				return (b & 128) != 0;
			case 1:
				return (b & 64) != 0;
			case 2:
				return (b & 32) != 0;
			case 3:
				return (b & 16) != 0;
			case 4:
				return (b & 8) != 0;
			case 5:
				return (b & 4) != 0;
			case 6:
				return (b & 2) != 0;
			case 7:
				return (b & 1) != 0;
			default:
				throw new IllegalStateException();
			}
		}
		
		/**
		 * Changes the DB icon transparency information
		 * 
		 * @param x x coordinate
		 * @param y y coordinate
		 * @param isSet true if transparent
		 * @return this icon
		 */
		private WorkspaceIcon setIconMask(int x, int y, boolean isSet) {
			checkRecycled();
			
			int offset = (31-y)*4 + (x >> 3);
			int mod = x % 8;


			WorkspaceVersion version = getWorkspaceVersion();
			int positionOffset;

			byte b;
			switch (version) {
			case R4:
			{
				positionOffset = ICON_MASK_POS_R4 + offset;
			}
			case R5_6:
			case R12:
			default:
			{
				positionOffset = ICON_MASK_POS_R5 + offset;
			}
			}

			b = m_iconBuf.get(positionOffset);
			byte newVal = b;
			
			switch (mod) {
			case 0:
			{
				if (isSet) {
					newVal |= 128;
				}
				else {
					newVal &= ~128;
				}
				break;
			}
			case 1:
			{
				if (isSet) {
					newVal |= 64;
				}
				else {
					newVal &= ~64;
				}
				break;
			}
			case 2:
			{
				if (isSet) {
					newVal |= 32;
				}
				else {
					newVal &= ~32;
				}
				break;
			}
			case 3:
			{
				if (isSet) {
					newVal |= 16;
				}
				else {
					newVal &= ~16;
				}
				break;
			}
			case 4:
			{
				if (isSet) {
					newVal |= 8;
				}
				else {
					newVal &= ~8;
				}
				break;
			}
			case 5:
			{
				if (isSet) {
					newVal |= 4;
				}
				else {
					newVal &= ~4;
				}
				break;
			}
			case 6:
			{
				if (isSet) {
					newVal |= 2;
				}
				else {
					newVal &= ~2;
				}
				break;
			}
			case 7:
			{
				if (isSet) {
					newVal |= 1;
				}
				else {
					newVal &= ~1;
				}
				break;
			}
			default:
				throw new IllegalStateException();
			}
			
			m_iconBuf.put(positionOffset, newVal);
			m_modified = true;
			return this;
		}
		
		/**
		 * Draws a pixel of the classic DB icon
		 * 
		 * @param x x coordinate (0-31)
		 * @param y y coordinate (0-31)
		 * @param color color or null if transparent
		 * @return this icon
		 */
		public WorkspaceIcon setIconColor(int x, int y, IconColor color) {
			if (color==null) {
				return setIconMask(x, y, true);
			}
			setIconMask(x, y, false);
			
			if (x<0 || x>31) {
				throw new IllegalArgumentException(MessageFormat.format("x must be between 0 and 31: ", x));
			}
			if (y<0 || y>31) {
				throw new IllegalArgumentException(MessageFormat.format("y must be between 0 and 31: ", y));
			}
			
			int offset = (31-y)*16 + (x >> 1);
			int mod = x % 2;

			WorkspaceVersion version = getWorkspaceVersion();
			int positionOffset;
			
			byte b;
			switch (version) {
			case R4:
			{
				positionOffset = ICON_BITMAP_POS_R4 + offset;
			}
			case R5_6:
			case R12:
			default:
			{
				positionOffset = ICON_BITMAP_POS_R5 + offset;
			}
			}
			
			b = m_iconBuf.get(positionOffset);
			
			if (mod == 1) {
				//replace lower 4 bits
				b &= 0xf0;
				b |= color.getIndex();
			}
			else {
				//replace upper 4 bits
				b &= 0x0f;
				b |= (color.getIndex() << 4);
			}
			
			m_iconBuf.put(positionOffset, b);
			m_modified = true;
			return this;
		}
		
		/**
		 * Returns the classic 32x32 pixel 16 color icon in the specified file format
		 * 
		 * @param format icon format, e.g. "PNG" or "GIF" (passed to {@link ImageIO}
		 * @return icon
		 */
		public ByteBuffer getIcon(String format) {
			BufferedImage bi = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

			for (int y=0; y<32; y++) {
				for (int x=0; x<32; x++) {
					Optional<IconColor> color = getIconColor(x, y);
					int alpha = 0; 
					int red   = 0;
					int green = 0;
					int blue  = 0;

					if (color.isPresent()) {
						alpha = 255;
						red = color.get().getRed();
						green = color.get().getGreen();
						blue = color.get().getBlue();
					}

					bi.setRGB(x, y, new Color(red, green, blue, alpha).getRGB());
				}
			}

			ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
			try {
				ImageIO.write(bi, format, imageOut);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return ByteBuffer.wrap(imageOut.toByteArray());
		}
		
		/**
		 * Returns the color of a single pixel in the database icon
		 * 
		 * @param x x coordinate (0-31)
		 * @param y y coordinate (0-31)
		 * @return color (0-15); empty if transparent
		 */
		public Optional<IconColor> getIconColor(int x, int y) {
			if (getIconMask(x, y)) {
				return Optional.empty();
			}
			
			if (x>31) {
				throw new IllegalArgumentException(MessageFormat.format("x must be between 0 and 31: ", x));
			}
			if (y>31) {
				throw new IllegalArgumentException(MessageFormat.format("y must be between 0 and 31: ", y));
			}
			
			int offset = (31-y)*16 + (x >> 1);
			int mod = x % 2;

			WorkspaceVersion version = getWorkspaceVersion();
			byte b;
			switch (version) {
			case R4:
			{
				b = m_iconBuf.get(ICON_BITMAP_POS_R4 + offset);
			}
			case R5_6:
			case R12:
			default:
			{
				b = m_iconBuf.get(ICON_BITMAP_POS_R5 + offset);
			}
			}
			if (mod == 1) {
				b = (byte) (b & 0xf);
			}
			else {
				b = (byte) (b >> 4);
			}
			return IconColor.forIndex( (int) (b & 0xff) );
		}
		
		/**
		 * Copies the database icons into the workspace icon (both classic and true color icon
		 * if present)
		 * 
		 * @param db database to copy icon from
		 * @return this instance
		 */
		public WorkspaceIcon setIconFrom(NotesDatabase db) {
			NotesNote iconNote = db.openIconNote();
			
			byte[] iconMaskArr = new byte[ICONMASK_SIZE];
			Arrays.fill(iconMaskArr, (byte) 255);
			byte[] iconBitmapArr = new byte[ICONBITMAP_SIZE];
			
			if (iconNote!=null) {
				NotesItem itmBitmap = iconNote.getFirstItem("IconBitmap");
				DisposableMemory iconData = itmBitmap.getValueRaw(false);
				
				iconData.read(6, iconMaskArr, 0, iconMaskArr.length);
				iconData.read(134, iconBitmapArr, 0, iconBitmapArr.length);
				
				iconNote.recycle();
			}

			setIconMask(ByteBuffer.wrap(iconMaskArr));
			setIconBitmap(ByteBuffer.wrap(iconBitmapArr));
			
			//try to find a true color icon
			NotesNote trueColorIconNote = db.openTrueColorIconNote();
			if (trueColorIconNote!=null) {
				ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
				
				IRichTextNavigator rtNav = trueColorIconNote.getRichtextNavigator("$ImageData");
				if (rtNav.gotoFirst()) {
					do {
						if (NotesConstants.SIG_CD_IMAGEHEADER == rtNav.getCurrentRecordTypeAsShort()) {
							//Memory imageHeaderMem = rtNav.getCurrentRecordData();
//							typedef struct {
//								   LSIG  Header;        /* Signature and Length */
//								   WORD  ImageType;     /* Type of image (e.g., GIF, JPEG) */
//								   WORD  Width;         /* Width of the image (in pixels) */
//								   WORD  Height;        /* Height of the image (in pixels) */
//								   DWORD ImageDataSize; /* Size (in bytes) of the image data */
//								   DWORD SegCount;      /* Number of CDIMAGESEGMENT records
//								                           expected to follow */
//								   DWORD Flags;         /* Flags (currently unused) */
//								   DWORD Reserved;      /* Reserved for future use */
//								} CDIMAGEHEADER;
						}
						else if (NotesConstants.SIG_CD_IMAGESEGMENT == rtNav.getCurrentRecordTypeAsShort()) {
//							typedef struct {
//								   LSIG Header;   /* Signature and Length */
//								   WORD DataSize; /* Actual Size of image bits in bytes, ignoring
//								                     any filler */
//								   WORD SegSize;  /* Size of segment, is equal to or larger than
//								                     DataSize if filler byte added to maintain word
//								                     boundary */
//								} CDIMAGESEGMENT;
								
							Memory imageSegmentMem = rtNav.getCurrentRecordData();
							short dataSize = imageSegmentMem.getShort(0);
							byte[] imageData = imageSegmentMem.getByteArray(4, Short.toUnsignedInt(dataSize));
							try {
								imageOut.write(imageData);
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							}
						}
					}
					while (rtNav.gotoNext());
				}
				
				if (imageOut.size()>0) {
					int trueColorIconObjId = getTrueColorIconObjectId();
					NotesDatabaseObject trueColorIconObject;
					if (trueColorIconObjId==0) {
						trueColorIconObject = new NotesDatabaseObject(m_dbDesktop, imageOut.size()+2,
								EnumSet.of(NoteClass.DATA), (short) 0, NotesConstants.OBJECT_FILE);
						setTrueColorIconObjectId(trueColorIconObject.getRRV());
					}
					else {
						try {
							trueColorIconObject = new NotesDatabaseObject(m_dbDesktop, trueColorIconObjId);
						}
						catch (NotesError e) {
							short status = (short) (e.getId() & NotesConstants.ERR_MASK);
							if (status==551) {
								trueColorIconObject = new NotesDatabaseObject(m_dbDesktop, imageOut.size()+2,
										EnumSet.of(NoteClass.DATA), (short) 0, NotesConstants.OBJECT_FILE);
								setTrueColorIconObjectId(trueColorIconObject.getRRV());
							}
							else {
								throw e;
							}
						}
					}

					trueColorIconObject.reallocate(imageOut.size()+2, false);
					trueColorIconObject.put(0, new byte[] {0x08, 0x00}); // probably a version number
					trueColorIconObject.put(2, imageOut.toByteArray());
					m_modified = true;
				}
				else {
					int objId = getTrueColorIconObjectId();
					if (objId!=0) {
						m_dbObjectsToFreeOnSave.add(objId);
						setTrueColorIconObjectId(0);
					}
				}
			}
			return this;
		}

		/**
		 * Sets the classic icon bitmap
		 * 
		 * @param buf new bitmap (512 byte, 32x32 pixel, 4 bits per pixel)
		 * @return this icon
		 */
		public WorkspaceIcon setIconBitmap(ByteBuffer buf) {
			if (buf.remaining() != ICONBITMAP_SIZE) {
				throw new IllegalArgumentException(MessageFormat.format(
						"Remaining size of buffer ({0}) must be {1}", buf.remaining(), ICONBITMAP_SIZE));
			}
			byte[] arr = new byte[ICONBITMAP_SIZE];
			buf.get(arr);
			
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				//icon bitmap
				m_iconBuf.position(ICON_BITMAP_POS_R4);
				m_iconBuf.put(arr);
			}
			case R5_6:
			case R12:
			default:
			{
				m_iconBuf.position(ICON_BITMAP_POS_R5);
				//icon bitmap
				m_iconBuf.put(arr);
			}
			}
			
			clearAdditionalData();
			
			m_modified = true;
			return this;
		}
		
		/**
		 * Sets the icon transparency mask
		 * 
		 * @param buf new mask (128 byte, 32x32 pixel with 1 bit per pixel)
		 * @return this icon
		 */
		public WorkspaceIcon setIconMask(ByteBuffer buf) {
			if (buf.remaining() != ICONMASK_SIZE) {
				throw new IllegalArgumentException(MessageFormat.format(
						"Remaining size of buffer ({0}) must be {1}", buf.remaining(), ICONMASK_SIZE));
			}
			byte[] arr = new byte[ICONMASK_SIZE];
			buf.get(arr);
			
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				//icon mask
				m_iconBuf.position(ICON_MASK_POS_R4);
			}
			case R5_6:
			case R12:
			default:
			{
				//icon mask
				m_iconBuf.position(ICON_MASK_POS_R5);
			}
			}

			m_iconBuf.put(arr);
			clearAdditionalData();
			
			m_modified = true;
			return this;
		}
		
		/**
		 * Returns the object id of the true color icon
		 * 
		 * @return object id or 0 if not set
		 */
		private int getTrueColorIconObjectId() {
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				return 0;
			}
			case R5_6:
			case R12:
			default:
			{
				//icon mask
				return m_iconBuf.getInt(0x41C);
			}
			}
		}
		
		/**
		 * Checks if the workspace icon contains a true color icon
		 * 
		 * @return true if exists
		 */
		public boolean hasTrueColorIcon() {
			return getTrueColorIconObjectId()!=0;
		}
		
		/**
		 * Sets the true color icon object id
		 * 
		 * @param objId new object id
		 * @return this icon
		 */
		private WorkspaceIcon setTrueColorIconObjectId(int objId) {
			WorkspaceVersion version = getWorkspaceVersion();

			switch (version) {
			case R4:
			{
				throw new IllegalStateException("True color icons not supported in R4");
			}
			case R5_6:
			case R12:
			default:
			{
				//icon mask
				m_iconBuf.putInt(0x41C, objId);
				m_modified = true;
				return this;
			}
			}
		}
		
		/**
		 * Returns the binary true color icon data
		 * 
		 * @return icon data if available
		 */
		public Optional<TrueColorIcon> getTrueColorIcon() {
			checkRecycled();

			int objId = getTrueColorIconObjectId();
			if (objId!=0) {
				NotesDatabaseObject dbObj = new NotesDatabaseObject(m_dbDesktop, objId);
				ByteBuffer dbObjData = dbObj.asByteBuffer();
				short version = dbObjData.getShort();
				ByteBuffer iconData = dbObjData.slice().order(ByteOrder.nativeOrder());
				iconData.position(0);
				
				return Optional.of(new TrueColorIcon(version, iconData));
			}
			return Optional.empty();
		}
		
		/**
		 * Replaces the content of the true color icon
		 * 
		 * @param icon new icon data or null to remove the old icon
		 */
		public void setTrueColorIcon(TrueColorIcon icon) {
			checkRecycled();
			
			int objId = getTrueColorIconObjectId();
			if (icon==null) {
				//icon should be removed
				if (objId!=0) {
					//we need to remove the current icon
					setTrueColorIconObjectId(0);
					//remember to dispose this after workspace save
					m_dbObjectsToFreeOnSave.add(objId);
				}
			}
			else {
				short version = icon.getVersion();
				ByteBuffer iconData = icon.getData();
				if (iconData.remaining()==0) {
					throw new IllegalArgumentException("Remaining ByteBuffer size for icon data is 0");
				}
				
				ByteBuffer iconVersionAndData = ByteBuffer.allocate(2 + iconData.remaining()).order(ByteOrder.nativeOrder());
				iconVersionAndData.putShort(version);
				iconVersionAndData.put(iconData);
				iconVersionAndData.position(0);
				
				NotesDatabaseObject dbObj;
				if (objId==0) {
					dbObj = new NotesDatabaseObject(m_dbDesktop, iconVersionAndData.remaining(), EnumSet.of(NoteClass.DATA),
							(short) 0, NotesConstants.OBJECT_FILE);
				}
				else {
					dbObj = new NotesDatabaseObject(m_dbDesktop, objId);
					dbObj.reallocate(iconVersionAndData.remaining(), false);
				}
				dbObj.put(0, iconVersionAndData);
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> clazz) {
			if (ByteBuffer.class.equals(clazz)) {
				return (T) m_iconBuf;
			}
			return null;
		}

		@Override
		public String toString() {
			return "WorkspaceIcon [path=" + getNetPath() + ", title="
					+ getTitle() + ", pageindex=" + getPageIndex() + ", x=" + getPosX() + ", y="
					+ getPosY() + ", topofstack="+isOnTopOfStack() + ", timestamp=" + getTimestamp()
					+ ", flags=0x" + (Integer.toHexString(Short.toUnsignedInt(getFlags())))
					+ ", flags2=0x" + (Integer.toHexString(Short.toUnsignedInt(getFlags2())))
							+ ", datarrv=" + getAdditionalDataRRV() + ", truecoloriconobjid=" + getTrueColorIconObjectId() + "]";
		}
		
	}

	/**
	 * Removes an icon from the workspace
	 * 
	 * @param icon icon to remove
	 * @return this workspace
	 */
	public NotesWorkspace removeIcon(WorkspaceIcon icon) {
		if (m_icons.contains(icon)) {
			m_icons.remove(icon);
			
			//clean up referenced resources on next save
			int objId = icon.getAdditionalDataRRV();
			if (objId!=0) {
				m_dbObjectsToFreeOnSave.add(objId);
				icon.setAdditionalDataRRV(0);
			}
			objId = icon.getTrueColorIconObjectId();
			if (objId!=0) {
				m_dbObjectsToFreeOnSave.add(objId);
				icon.setTrueColorIconObjectId(0);
			}
			m_modified = true;
		}
		return this;
	}
	
	/**
	 * Removes a page and all its icons
	 * 
	 * @param page page to remove
	 * @return this workspace
	 */
	public NotesWorkspace removePage(WorkspacePage page) {
		int idx = m_pages.indexOf(page);
		if (idx==-1) {
			return this;
		}
		
		//remove all icons on the page
		//and fix the pageindex of icons on later pages
		Iterator<WorkspaceIcon> iconsIt = m_icons.iterator();
		while (iconsIt.hasNext()) {
			WorkspaceIcon currIcon = iconsIt.next();
			if (currIcon.getPageIndex() == idx) {
				iconsIt.remove();
				m_modified = true;
			}
			else if (currIcon.getPageIndex() > idx) {
				currIcon.moveSafe(currIcon.getPageIndex()-1, currIcon.getPosX(), currIcon.getPosY());
				m_modified = true;
			}
		}
		
		m_pages.remove(idx);
		
		return this;
	}
	
	/**
	 * Adds a new workspace page at the end of the page list
	 * 
	 * @param title page title
	 * @return new page
	 */
	public WorkspacePage addPage(String title) {
		ByteBuffer pageDataBuf = ByteBuffer.allocate(PAGESIZE).order(ByteOrder.nativeOrder());
		WorkspacePage newPage = new WorkspacePage(pageDataBuf);
		newPage.setTitle(title);
		m_pages.add(newPage);
		m_modified = true;
		return newPage;
	}
	
	/**
	 * Inserts a new workspace page before the specified target page
	 * 
	 * @param beforePage target page
	 * @param title page title
	 * @return new page
	 */
	public WorkspacePage insertPage(WorkspacePage beforePage, String title) {
		int targetIdx = m_pages.indexOf(beforePage);
		if (targetIdx==-1) {
			throw new IllegalArgumentException(MessageFormat.format("Target page is not part of the workspace: {0}",beforePage));
		}
		
		return insertPage(targetIdx, title);
	}

	/**
	 * Inserts a new workspace page before the specified target page
	 * 
	 * @param targetIdx insert index
	 * @param title page title
	 * @return new page
	 */
	public WorkspacePage insertPage(int targetIdx, String title) {
		if (targetIdx >= m_pages.size()) {
			return addPage(title);
		}
		
		//fix pageIndex of all icons to the right
		m_icons
		.stream()
		.filter((icon) -> { return icon.getPageIndex() >= targetIdx; })
		.forEach((icon) -> {
			icon.moveSafe(icon.getPageIndex()+1, icon.getPosX(), icon.getPosY());
			m_modified = true;
		});
		
		ByteBuffer pageDataBuf = ByteBuffer.allocate(PAGESIZE).order(ByteOrder.nativeOrder());
		WorkspacePage newPage = new WorkspacePage(pageDataBuf);
		newPage.setTitle(title);
		m_pages.add(targetIdx, newPage);
		m_modified = true;
		return newPage;
	}
	
	/**
	 * Moves a page within the workspace with all its icons
	 * 
	 * @param pageToMove page to move
	 * @param targetPage new page position
	 * @param addBefore true to add before targetPage
	 * @return this workspace
	 */
	public NotesWorkspace movePage(WorkspacePage pageToMove, WorkspacePage targetPage,
			boolean addBefore) {

		if (!m_pages.contains(targetPage)) {
			throw new IllegalArgumentException(MessageFormat.format("Target page is not part of the workspace: {0}",targetPage));
		}
		
		//remember the current icon parent pages to restore the right index
		Map<WorkspaceIcon,WorkspacePage> iconParentPages = new HashMap<>();
		m_icons
		.stream()
		.forEach((icon) -> {
			iconParentPages.put(icon, icon.getPage().get());
		});
		
		m_pages.remove(pageToMove);
		
		int targetIdx = m_pages.indexOf(targetPage);
		
		if (!addBefore) {
			targetIdx++;
		}

		if (targetIdx==m_pages.size()) {
			m_pages.add(pageToMove);
		}
		else {
			m_pages.add(targetIdx, pageToMove);
		}

		//fix icon page index
		m_icons
		.stream()
		.forEach((icon) -> {
			WorkspacePage page = iconParentPages.get(icon);
			int pageIdx = m_pages.indexOf(page);
			icon.moveSafe(pageIdx, icon.getPosX(), icon.getPosY());
		});
		
		return this;
	}
	
	/**
	 * Finds a free slot on the specified page
	 * 
	 * @param page page
	 * @param maxWidth max icons in x direction
	 * @return x/y position of free slot
	 */
	public Pair<Integer,Integer> findFreeSlot(WorkspacePage page, int maxWidth) {
		int pageIdx = m_pages.indexOf(page);
		
		if (pageIdx==-1) {
			throw new IllegalArgumentException(MessageFormat.format("Page is not part of the workspace: {0}",page));
		}
		
		//collect all icons on that page
		List<WorkspaceIcon> iconsOnPage = getIcons()
				.stream()
				.filter((icon) -> { return pageIdx == icon.getPageIndex(); })
				.collect(Collectors.toList());
		
		AtomicInteger y = new AtomicInteger();
		do {
			for (AtomicInteger x=new AtomicInteger(); x.get()<maxWidth; x.incrementAndGet()) {
				boolean hasIconsAtPos = iconsOnPage
						.stream()
						.anyMatch((icon) -> {
							return icon.getPosX() == x.get() && icon.getPosY() == y.get();
						});
				
				if (!hasIconsAtPos) {
					return new Pair<>(x.get(), y.get());
				}
			}
			y.incrementAndGet();
		}
		while (true);
	}
	
	/**
	 * Swaps the icons at the specified positions
	 * 
	 * @param page1 page 1
	 * @param pos1 position 1
	 * @param page2 page 2
	 * @param pos2 position 2
	 * @return this icon
	 */
	public NotesWorkspace swapIcons(WorkspacePage page1, Pair<Integer,Integer> pos1,
			WorkspacePage page2, Pair<Integer,Integer> pos2) {
		
		int pageIdx1 = m_pages.indexOf(page1);
		
		if (pageIdx1==-1) {
			throw new IllegalArgumentException(MessageFormat.format("First page is not part of the workspace: {0}",page1));
		}

		int pageIdx2 = m_pages.indexOf(page2);
		
		if (pageIdx2==-1) {
			throw new IllegalArgumentException(MessageFormat.format("Second page is not part of the workspace: {0}",page1));
		}

		List<WorkspaceIcon> iconsAtPos1 = getIcons()
				.stream()
				.filter((icon) -> {
					return pageIdx1 == icon.getPageIndex() &&
							pos1.getValue1() == icon.getPosX() &&
							pos1.getValue2() == icon.getPosY();
					})
				.collect(Collectors.toList());


		List<WorkspaceIcon> iconsAtPos2 = getIcons()
				.stream()
				.filter((icon) -> {
					return pageIdx2 == icon.getPageIndex() &&
							pos2.getValue1() == icon.getPosX() &&
							pos2.getValue2() == icon.getPosY();
					})
				.collect(Collectors.toList());

		iconsAtPos1.forEach((icon) -> {
			icon.moveSafe(pageIdx2, pos2.getValue1(), pos2.getValue2());
		});
		
		iconsAtPos2.forEach((icon) -> {
			icon.moveSafe(pageIdx1, pos1.getValue1(), pos1.getValue2());
		});

		return this;
	}
	
	/**
	 * Searches for icons with the same replica id
	 * 
	 * @return replicaId DB replica id
	 */
	public List<WorkspaceIcon> getIconsByReplicaID(String replicaId) {
		return getIcons()
				.stream()
				.filter((icon) -> {
					return replicaId.equals(icon.getReplicaID());
				})
				.collect(Collectors.toList());
	}
	
	/**
	 * Sorts icons by pageindex / y / x
	 * 
	 * @author Karsten Lehmann
	 */
	public static class IconComparator implements Comparator<WorkspaceIcon> {

		@Override
		public int compare(WorkspaceIcon o1, WorkspaceIcon o2) {
			int pageIndex1 = o1.getPageIndex();
			int pageIndex2 = o2.getPageIndex();
			
			if (pageIndex1 < pageIndex2) {
				return -1;
			}
			else if (pageIndex1 > pageIndex2) {
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
			
			String fileName1 = o1.getFilePath();
			String fileName2 = o2.getFilePath();
			c = fileName1.compareToIgnoreCase(fileName2);
			if (c!=0) {
				return c;
			}
			
			return System.identityHashCode(o1) - System.identityHashCode(o2);
		}
		
	}
	
	/**
	 * Exception is thrown when {@link WorkspaceIcon#move(WorkspacePage, int, int)} is called with
	 * a position that already contains an icon
	 * 
	 * @author Karsten Lehmann
	 */
	public static class IconPositionNotAvailable extends NotesError {
		private static final long serialVersionUID = -3503334506578782454L;

		public IconPositionNotAvailable(String msg) {
			super(msg);
		}
	}
	
	/**
	 * Container for the binary data of the true color icon and its version
	 * 
	 * @author Karsten Lehmann
	 */
	public static class TrueColorIcon {
		private short version;
		private ByteBuffer iconData;
		
		/**
		 * Creates a new true color icon
		 * 
		 * @param version up to Notes 12.0.1, this version number is set to 8
		 * @param iconData binary PNG data
		 */
		public TrueColorIcon(short version, ByteBuffer iconData) {
			this.version = version;
			this.iconData = iconData;
		}
		
		/**
		 * Returns the version of the icon
		 * 
		 * @return up to Notes 12.0.1, this version number is set to 8
		 */
		public short getVersion() {
			return version;
		}
		
		/**
		 * Returns the binary PNG data copied from the NSF icon
		 * 
		 * @return icon data
		 */
		public ByteBuffer getData() {
			return iconData;
		}
	}

	/**
	 * Available colors in the old 32x32 database icon (4 bit)
	 * 
	 * @author Karsten Lehmann
	 */
	public static enum IconColor {
		BLACK(0, 0, 0, 0),
		WHITE(1, 0xff, 0xff, 0xff),
		RED(2, 0xEA, 0x33, 0x23),
		GREEN(3, 0x75, 0xFA, 0x4C),
		BLUE(4, 0x00, 0x1C, 0xF5),
		MAGENTA(5, 0xEA, 0x3B, 0xF7),
		YELLOW(6, 0xFF, 0xFE, 0x54),
		CYAN(7, 0x74, 0xFB, 0xFD),
		DARKRED(8, 0x75, 0x14, 0x0C),
		DARKGREEN(9, 0x37, 0x7D, 0x22),
		DARKBLUE(10, 0x00, 0x08, 0x7A),
		DARKMAGENTA(11, 0x75, 0x19, 0x7C),
		DARKYELLOW(12, 0x80, 0x7F, 0x26),
		DARKCYAN(13, 0x36, 0x7E, 0x7F),
		DARKGREY(14, 0x80, 0x80, 0x80),
		LIGHTGREY(15, 0xC0, 0xC0, 0xC0);

		private final int idx;
		private final int red;
		private final int green;
		private final int blue;
		
		private IconColor(int idx, int red, int green, int blue) {
			this.idx = idx;
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
		
		public int getIndex() {
			return idx;
		}
		
		public int getRed() {
			return red;
		}
		
		public int getGreen() {
			return green;
		}
		
		public int getBlue() {
			return blue;
		}
		
		public static Optional<IconColor> forIndex(int idx) {
			for (IconColor currColor : values()) {
				if (currColor.getIndex() == idx) {
					return Optional.of(currColor);
				}
			}
			return Optional.empty();
		}
	}

	//convenience methods to work with workspace page colors
	
	/**
	 * Returns all available colors for the workspace pages
	 * 
	 * @return readonly list of colors
	 */
	public static List<Color> getAvailablePageColors() {
		return Collections.unmodifiableList(Arrays.asList(pagePaletteColors));
	}
	
	/**
	 * Scans through the palette colors and finds the best match for
	 * the given {@link Color} value
	 * 
	 * @param color color
	 * @return index of nearest palette color
	 */
	public static int findNearestTabColor(Color color) {
		return findNearestTabColor(color.getRed(), color.getGreen(), color.getBlue());
	}

	/**
	 * Scans through the palette colors and finds the best match for
	 * the given {@link StandardColors} value
	 * 
	 * @param stdColor standard color
	 * @return index of nearest palette color
	 */
	public static int findNearestPageColor(StandardColors stdColor) {
		return findNearestTabColor(stdColor.getRed(), stdColor.getGreen(), stdColor.getBlue());
	}

	/**
	 * Scans through the palette colors and finds the best match for
	 * the given RGB values
	 * 
	 * @param red red value of color
	 * @param green green value of color
	 * @param blue blue value of color
	 * @return index of nearest palette color
	 */
	public static int findNearestTabColor(int red, int green, int blue) {
		int nearestColorIndex = -1;
		double nearestDistance = Double.MAX_VALUE;
		
		for (int i=0; i<pagePaletteColors.length; i++) {
			Color currColor = pagePaletteColors[i];
			
			//uses algorithm described here: https://www.compuphase.com/cmetric.htm
		    int red2 = currColor.getRed();
		    int rmean = (red + red2) >> 1;
		    int r = red - red2;
		    int g = green - currColor.getGreen();
		    int b = blue - currColor.getBlue();
		    double currDistance = Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));

		    if (currDistance < nearestDistance) {
		    	nearestColorIndex = i;
		    	nearestDistance = currDistance;
		    }
		}

		return nearestColorIndex;
	}

	/**
	 * Manually extracted list of available workspace page colors;
	 * unfortunately not corresponding to {@link StandardColors},
	 * so we have to provide convenience functions to find the
	 * best match.
	 */
	private static final Color[] pagePaletteColors = new Color[240];
	static {
		pagePaletteColors[0]=new Color(0x80, 0x80, 0x80);
		pagePaletteColors[1]=new Color(0x00, 0x00, 0x00);
		pagePaletteColors[2]=new Color(0xFF, 0xFF, 0xFF);
		pagePaletteColors[3]=new Color(0xEA, 0x32, 0x24);
		pagePaletteColors[4]=new Color(0x75, 0xFA, 0x4C);
		pagePaletteColors[5]=new Color(0x00, 0x1C, 0xF5);
		pagePaletteColors[6]=new Color(0xEB, 0x3B, 0xF6);
		pagePaletteColors[7]=new Color(0xFF, 0xFE, 0x54);
		pagePaletteColors[8]=new Color(0x73, 0xFB, 0xFD);
		pagePaletteColors[9]=new Color(0x75, 0x15, 0x0C);
		pagePaletteColors[10]=new Color(0x38, 0x7E, 0x22);
		pagePaletteColors[11]=new Color(0x00, 0x09, 0x7B);
		pagePaletteColors[12]=new Color(0x75, 0x19, 0x7C);
		pagePaletteColors[13]=new Color(0x80, 0x7F, 0x26);
		pagePaletteColors[14]=new Color(0x36, 0x7E, 0x7F);
		pagePaletteColors[15]=new Color(0x81, 0x80, 0x80);
		pagePaletteColors[16]=new Color(0xBF, 0xBF, 0xBF);
		pagePaletteColors[17]=new Color(0xFF, 0xFF, 0xFF);
		pagePaletteColors[18]=new Color(0xFD, 0xEF, 0xD1);
		pagePaletteColors[19]=new Color(0xFF, 0xFE, 0xC9);
		pagePaletteColors[20]=new Color(0xFF, 0xFF, 0xD5);
		pagePaletteColors[21]=new Color(0xE6, 0xFD, 0xC6);
		pagePaletteColors[22]=new Color(0xE6, 0xFF, 0xE1);
		pagePaletteColors[23]=new Color(0xE5, 0xFE, 0xFE);
		pagePaletteColors[24]=new Color(0xCA, 0xEF, 0xFD);
		pagePaletteColors[25]=new Color(0xE3, 0xF1, 0xFE);
		pagePaletteColors[26]=new Color(0xE0, 0xE1, 0xFC);
		pagePaletteColors[27]=new Color(0xE7, 0xE1, 0xFD);
		pagePaletteColors[28]=new Color(0xEE, 0xE1, 0xFD);
		pagePaletteColors[29]=new Color(0xFA, 0xE2, 0xFD);
		pagePaletteColors[30]=new Color(0xFA, 0xE1, 0xF4);
		pagePaletteColors[31]=new Color(0xFA, 0xE2, 0xE6);
		pagePaletteColors[32]=new Color(0xFF, 0xFF, 0xFF);
		pagePaletteColors[33]=new Color(0xFB, 0xE3, 0xDD);
		pagePaletteColors[34]=new Color(0xFB, 0xE2, 0xB6);
		pagePaletteColors[35]=new Color(0xFF, 0xFF, 0xAF);
		pagePaletteColors[36]=new Color(0xF1, 0xF1, 0xBB);
		pagePaletteColors[37]=new Color(0xCF, 0xFD, 0x9E);
		pagePaletteColors[38]=new Color(0xCE, 0xFC, 0xD9);
		pagePaletteColors[39]=new Color(0xB8, 0xFD, 0xFE);
		pagePaletteColors[40]=new Color(0xAF, 0xE1, 0xFC);
		pagePaletteColors[41]=new Color(0xC6, 0xE0, 0xFC);
		pagePaletteColors[42]=new Color(0xBE, 0xC0, 0xFA);
		pagePaletteColors[43]=new Color(0xCF, 0xC1, 0xFA);
		pagePaletteColors[44]=new Color(0xDB, 0xC1, 0xFA);
		pagePaletteColors[45]=new Color(0xF6, 0xC4, 0xF9);
		pagePaletteColors[46]=new Color(0xF5, 0xC3, 0xE2);
		pagePaletteColors[47]=new Color(0xF6, 0xC3, 0xCE);
		pagePaletteColors[48]=new Color(0xF7, 0xF7, 0xF7);
		pagePaletteColors[49]=new Color(0xF5, 0xC3, 0xB9);
		pagePaletteColors[50]=new Color(0xF6, 0xC4, 0x8A);
		pagePaletteColors[51]=new Color(0xFF, 0xFE, 0x62);
		pagePaletteColors[52]=new Color(0xF1, 0xF1, 0x8F);
		pagePaletteColors[53]=new Color(0xA0, 0xFB, 0x8E);
		pagePaletteColors[54]=new Color(0xA2, 0xFC, 0xCE);
		pagePaletteColors[55]=new Color(0xA0, 0xFC, 0xFE);
		pagePaletteColors[56]=new Color(0x98, 0xDE, 0xFC);
		pagePaletteColors[57]=new Color(0x8F, 0xBF, 0xFA);
		pagePaletteColors[58]=new Color(0x9E, 0xA0, 0xF9);
		pagePaletteColors[59]=new Color(0xBC, 0xA2, 0xF9);
		pagePaletteColors[60]=new Color(0xD9, 0xA3, 0xF9);
		pagePaletteColors[61]=new Color(0xF2, 0xA5, 0xF9);
		pagePaletteColors[62]=new Color(0xF2, 0xA4, 0xCD);
		pagePaletteColors[63]=new Color(0xF2, 0xA3, 0xAA);
		pagePaletteColors[64]=new Color(0xEF, 0xEE, 0xEF);
		pagePaletteColors[65]=new Color(0xF1, 0xA3, 0xA1);
		pagePaletteColors[66]=new Color(0xF2, 0xA2, 0x79);
		pagePaletteColors[67]=new Color(0xFF, 0xFE, 0x54);
		pagePaletteColors[68]=new Color(0xE0, 0xDF, 0x83);
		pagePaletteColors[69]=new Color(0x82, 0xFA, 0x5B);
		pagePaletteColors[70]=new Color(0x81, 0xFB, 0xCB);
		pagePaletteColors[71]=new Color(0x80, 0xFC, 0xFD);
		pagePaletteColors[72]=new Color(0x54, 0xBD, 0xFA);
		pagePaletteColors[73]=new Color(0x60, 0x92, 0xE8);
		pagePaletteColors[74]=new Color(0x7F, 0x82, 0xF7);
		pagePaletteColors[75]=new Color(0xB7, 0x87, 0xF8);
		pagePaletteColors[76]=new Color(0xD3, 0x88, 0xF7);
		pagePaletteColors[77]=new Color(0xEF, 0x88, 0xF8);
		pagePaletteColors[78]=new Color(0xEF, 0x8A, 0xBF);
		pagePaletteColors[79]=new Color(0xEF, 0x89, 0xA0);
		pagePaletteColors[80]=new Color(0xE1, 0xE1, 0xE0);
		pagePaletteColors[81]=new Color(0xEF, 0x87, 0x84);
		pagePaletteColors[82]=new Color(0xF0, 0x88, 0x50);
		pagePaletteColors[83]=new Color(0xFA, 0xE1, 0x50);
		pagePaletteColors[84]=new Color(0xE1, 0xE0, 0x5F);
		pagePaletteColors[85]=new Color(0x75, 0xFA, 0x4C);
		pagePaletteColors[86]=new Color(0x74, 0xFA, 0xB7);
		pagePaletteColors[87]=new Color(0x74, 0xFB, 0xFD);
		pagePaletteColors[88]=new Color(0x45, 0x9F, 0xDA);
		pagePaletteColors[89]=new Color(0x3D, 0x82, 0xF7);
		pagePaletteColors[90]=new Color(0x66, 0x83, 0xF7);
		pagePaletteColors[91]=new Color(0x97, 0x67, 0xF6);
		pagePaletteColors[92]=new Color(0xB4, 0x6A, 0xF7);
		pagePaletteColors[93]=new Color(0xEC, 0x6D, 0xF8);
		pagePaletteColors[94]=new Color(0xED, 0x6B, 0xAC);
		pagePaletteColors[95]=new Color(0xED, 0x6B, 0x88);
		pagePaletteColors[96]=new Color(0xD2, 0xD2, 0xD2);
		pagePaletteColors[97]=new Color(0xEC, 0x51, 0x4A);
		pagePaletteColors[98]=new Color(0xEB, 0x52, 0x33);
		pagePaletteColors[99]=new Color(0xF6, 0xC1, 0x47);
		pagePaletteColors[100]=new Color(0xE1, 0xE0, 0x4A);
		pagePaletteColors[101]=new Color(0x66, 0xDD, 0x42);
		pagePaletteColors[102]=new Color(0x65, 0xDD, 0xB0);
		pagePaletteColors[103]=new Color(0x64, 0xDD, 0xDE);
		pagePaletteColors[104]=new Color(0x36, 0x81, 0xB9);
		pagePaletteColors[105]=new Color(0x33, 0x81, 0xF7);
		pagePaletteColors[106]=new Color(0x4F, 0x82, 0xF7);
		pagePaletteColors[107]=new Color(0x79, 0x4B, 0xF5);
		pagePaletteColors[108]=new Color(0xB2, 0x4F, 0xF6);
		pagePaletteColors[109]=new Color(0xEB, 0x57, 0xF1);
		pagePaletteColors[110]=new Color(0xEC, 0x53, 0x9D);
		pagePaletteColors[111]=new Color(0xEB, 0x52, 0x71);
		pagePaletteColors[112]=new Color(0xC0, 0xC0, 0xC0);
		pagePaletteColors[113]=new Color(0xEA, 0x3C, 0x40);
		pagePaletteColors[114]=new Color(0xEA, 0x3D, 0x2A);
		pagePaletteColors[115]=new Color(0xEF, 0x87, 0x33);
		pagePaletteColors[116]=new Color(0xC0, 0xBE, 0x3D);
		pagePaletteColors[117]=new Color(0x58, 0xBE, 0x38);
		pagePaletteColors[118]=new Color(0x57, 0xBE, 0x99);
		pagePaletteColors[119]=new Color(0x55, 0xBE, 0xC0);
		pagePaletteColors[120]=new Color(0x51, 0x80, 0xBB);
		pagePaletteColors[121]=new Color(0x25, 0x63, 0xD9);
		pagePaletteColors[122]=new Color(0x3E, 0x48, 0xF5);
		pagePaletteColors[123]=new Color(0x39, 0x1E, 0xF4);
		pagePaletteColors[124]=new Color(0xB1, 0x30, 0xF6);
		pagePaletteColors[125]=new Color(0xEB, 0x45, 0xF6);
		pagePaletteColors[126]=new Color(0xE1, 0x43, 0x94);
		pagePaletteColors[127]=new Color(0xEB, 0x3F, 0x5D);
		pagePaletteColors[128]=new Color(0xB2, 0xB2, 0xB2);
		pagePaletteColors[129]=new Color(0xCE, 0x37, 0x31);
		pagePaletteColors[130]=new Color(0xCF, 0x37, 0x1F);
		pagePaletteColors[131]=new Color(0xD3, 0x69, 0x29);
		pagePaletteColors[132]=new Color(0xA1, 0xA0, 0x32);
		pagePaletteColors[133]=new Color(0x47, 0x9D, 0x2D);
		pagePaletteColors[134]=new Color(0x45, 0x9D, 0x83);
		pagePaletteColors[135]=new Color(0x4F, 0x7E, 0x7F);
		pagePaletteColors[136]=new Color(0x26, 0x60, 0x9B);
		pagePaletteColors[137]=new Color(0x13, 0x43, 0xBB);
		pagePaletteColors[138]=new Color(0x00, 0x27, 0xB7);
		pagePaletteColors[139]=new Color(0x3A, 0x16, 0xBA);
		pagePaletteColors[140]=new Color(0x74, 0x25, 0xF6);
		pagePaletteColors[141]=new Color(0xEA, 0x3B, 0xF7);
		pagePaletteColors[142]=new Color(0xEB, 0x34, 0x7F);
		pagePaletteColors[143]=new Color(0xEA, 0x33, 0x48);
		pagePaletteColors[144]=new Color(0xA2, 0xA2, 0xA2);
		pagePaletteColors[145]=new Color(0xB2, 0x24, 0x18);
		pagePaletteColors[146]=new Color(0xEA, 0x32, 0x23);
		pagePaletteColors[147]=new Color(0xB1, 0x49, 0x1E);
		pagePaletteColors[148]=new Color(0x81, 0x7F, 0x48);
		pagePaletteColors[149]=new Color(0x4F, 0x7E, 0x46);
		pagePaletteColors[150]=new Color(0x38, 0x80, 0x54);
		pagePaletteColors[151]=new Color(0x27, 0x5F, 0x62);
		pagePaletteColors[152]=new Color(0x16, 0x40, 0x7C);
		pagePaletteColors[153]=new Color(0x00, 0x29, 0xD9);
		pagePaletteColors[154]=new Color(0x3F, 0x43, 0xBB);
		pagePaletteColors[155]=new Color(0x39, 0x12, 0x9C);
		pagePaletteColors[156]=new Color(0x57, 0x16, 0x9B);
		pagePaletteColors[157]=new Color(0xCD, 0x32, 0xD8);
		pagePaletteColors[158]=new Color(0xCC, 0x2D, 0x7D);
		pagePaletteColors[159]=new Color(0xB2, 0x24, 0x43);
		pagePaletteColors[160]=new Color(0x8F, 0x8F, 0x8F);
		pagePaletteColors[161]=new Color(0x93, 0x1C, 0x12);
		pagePaletteColors[162]=new Color(0xCE, 0x2B, 0x1E);
		pagePaletteColors[163]=new Color(0x96, 0x45, 0x19);
		pagePaletteColors[164]=new Color(0x63, 0x62, 0x1B);
		pagePaletteColors[165]=new Color(0x27, 0x5E, 0x16);
		pagePaletteColors[166]=new Color(0x28, 0x5E, 0x3F);
		pagePaletteColors[167]=new Color(0x18, 0x3E, 0x40);
		pagePaletteColors[168]=new Color(0x0D, 0x30, 0x7A);
		pagePaletteColors[169]=new Color(0x00, 0x1C, 0xF4);
		pagePaletteColors[170]=new Color(0x1E, 0x24, 0x99);
		pagePaletteColors[171]=new Color(0x1C, 0x0E, 0x9B);
		pagePaletteColors[172]=new Color(0x39, 0x0D, 0x7A);
		pagePaletteColors[173]=new Color(0x94, 0x21, 0x9A);
		pagePaletteColors[174]=new Color(0xB0, 0x26, 0x7C);
		pagePaletteColors[175]=new Color(0x92, 0x1B, 0x1A);
		pagePaletteColors[176]=new Color(0x80, 0x80, 0x80);
		pagePaletteColors[177]=new Color(0x57, 0x0D, 0x06);
		pagePaletteColors[178]=new Color(0xB2, 0x2A, 0x21);
		pagePaletteColors[179]=new Color(0x7A, 0x45, 0x15);
		pagePaletteColors[180]=new Color(0x42, 0x42, 0x0F);
		pagePaletteColors[181]=new Color(0x19, 0x41, 0x0D);
		pagePaletteColors[182]=new Color(0x18, 0x3F, 0x26);
		pagePaletteColors[183]=new Color(0x10, 0x31, 0x3E);
		pagePaletteColors[184]=new Color(0x06, 0x21, 0x5C);
		pagePaletteColors[185]=new Color(0x00, 0x27, 0xBA);
		pagePaletteColors[186]=new Color(0x1F, 0x28, 0xB9);
		pagePaletteColors[187]=new Color(0x00, 0x08, 0x7A);
		pagePaletteColors[188]=new Color(0x1A, 0x09, 0x7A);
		pagePaletteColors[189]=new Color(0x75, 0x18, 0x7B);
		pagePaletteColors[190]=new Color(0x77, 0x16, 0x40);
		pagePaletteColors[191]=new Color(0x75, 0x15, 0x0C);
		pagePaletteColors[192]=new Color(0x5F, 0x5F, 0x5F);
		pagePaletteColors[193]=new Color(0x3A, 0x06, 0x03);
		pagePaletteColors[194]=new Color(0x95, 0x2B, 0x1E);
		pagePaletteColors[195]=new Color(0x5C, 0x43, 0x11);
		pagePaletteColors[196]=new Color(0x21, 0x21, 0x04);
		pagePaletteColors[197]=new Color(0x09, 0x20, 0x04);
		pagePaletteColors[198]=new Color(0x08, 0x20, 0x1E);
		pagePaletteColors[199]=new Color(0x07, 0x20, 0x3F);
		pagePaletteColors[200]=new Color(0x07, 0x20, 0x4B);
		pagePaletteColors[201]=new Color(0x00, 0x17, 0xD7);
		pagePaletteColors[202]=new Color(0x00, 0x0E, 0x9A);
		pagePaletteColors[203]=new Color(0x00, 0x05, 0x5C);
		pagePaletteColors[204]=new Color(0x1A, 0x06, 0x5E);
		pagePaletteColors[205]=new Color(0x3A, 0x0A, 0x5B);
		pagePaletteColors[206]=new Color(0x59, 0x0F, 0x40);
		pagePaletteColors[207]=new Color(0x59, 0x0D, 0x15);
		pagePaletteColors[208]=new Color(0x4F, 0x4F, 0x4F);
		pagePaletteColors[209]=new Color(0xCC, 0xB2, 0xA3);
		pagePaletteColors[210]=new Color(0xD7, 0xA3, 0x7B);
		pagePaletteColors[211]=new Color(0xCD, 0xB1, 0x74);
		pagePaletteColors[212]=new Color(0xC0, 0xC1, 0x85);
		pagePaletteColors[213]=new Color(0x90, 0xBE, 0x72);
		pagePaletteColors[214]=new Color(0x8F, 0xBE, 0x9A);
		pagePaletteColors[215]=new Color(0x8E, 0xC1, 0xBC);
		pagePaletteColors[216]=new Color(0x7F, 0xB1, 0xCC);
		pagePaletteColors[217]=new Color(0xB1, 0xB1, 0xCF);
		pagePaletteColors[218]=new Color(0x9F, 0xA0, 0xDB);
		pagePaletteColors[219]=new Color(0xBB, 0xA3, 0xDB);
		pagePaletteColors[220]=new Color(0xD9, 0xA2, 0xDA);
		pagePaletteColors[221]=new Color(0xE2, 0x96, 0xE6);
		pagePaletteColors[222]=new Color(0xD8, 0xA2, 0xC7);
		pagePaletteColors[223]=new Color(0xE4, 0x95, 0xBA);
		pagePaletteColors[224]=new Color(0x2F, 0x2F, 0x2F);
		pagePaletteColors[225]=new Color(0x7A, 0x61, 0x52);
		pagePaletteColors[226]=new Color(0x98, 0x64, 0x56);
		pagePaletteColors[227]=new Color(0x7B, 0x62, 0x24);
		pagePaletteColors[228]=new Color(0x82, 0x82, 0x49);
		pagePaletteColors[229]=new Color(0x46, 0x60, 0x29);
		pagePaletteColors[230]=new Color(0x44, 0x60, 0x41);
		pagePaletteColors[231]=new Color(0x41, 0x5E, 0x5E);
		pagePaletteColors[232]=new Color(0x1E, 0x41, 0x5D);
		pagePaletteColors[233]=new Color(0x41, 0x43, 0x7E);
		pagePaletteColors[234]=new Color(0x61, 0x61, 0x9C);
		pagePaletteColors[235]=new Color(0x5D, 0x43, 0x7E);
		pagePaletteColors[236]=new Color(0x5A, 0x35, 0x7C);
		pagePaletteColors[237]=new Color(0x59, 0x27, 0x5E);
		pagePaletteColors[238]=new Color(0x5A, 0x26, 0x4F);
		pagePaletteColors[239]=new Color(0x79, 0x43, 0x60);

	}
	
}