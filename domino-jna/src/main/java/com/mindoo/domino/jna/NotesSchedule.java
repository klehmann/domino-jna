package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem32;
import com.mindoo.domino.jna.internal.Mem64;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI32;
import com.mindoo.domino.jna.internal.NotesNativeAPI64;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryExtStruct;
import com.mindoo.domino.jna.internal.structs.NotesSchedEntryStruct;
import com.mindoo.domino.jna.internal.structs.NotesScheduleListStruct;
import com.mindoo.domino.jna.internal.structs.NotesScheduleStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDatePairStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.internal.structs.NotesUniversalNoteIdStruct;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

/**
 * Schedule object to read busy and free time info for a single Domino user
 * 
 * @author Karsten Lehmann
 */
public class NotesSchedule implements IRecyclableNotesObject {
	private NotesScheduleContainer m_parent;
	private long m_hSched64;
	private int m_hSched32;
	private boolean m_noRecycle;
	private NotesScheduleStruct m_scheduleData;
	private String m_owner;
	
	public NotesSchedule(NotesScheduleContainer parent, IAdaptable scheduleData, String owner, long hSchedule64) {
		if (!PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_parent = parent;
		m_hSched64 = hSchedule64;
		m_scheduleData = scheduleData.getAdapter(NotesScheduleStruct.class);
		m_owner = owner;
	}

	public NotesSchedule(NotesScheduleContainer parent, IAdaptable scheduleData, String owner, int hSchedule32) {
		if (PlatformUtils.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_parent = parent;
		m_hSched32 = hSchedule32;
		m_scheduleData = scheduleData.getAdapter(NotesScheduleStruct.class);
		m_owner = owner;
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}

	@Override
	public boolean isNoRecycle() {
		return m_noRecycle;
	}
	
	@Override
	public void recycle() {
		if (m_noRecycle || isRecycled())
			return;

		if (PlatformUtils.is64Bit()) {
			NotesNativeAPI64.get().Schedule_Free(m_parent.getHandle64(), (int) m_hSched64);
			NotesGC.__objectBeeingBeRecycled(NotesSchedule.class, this);
			m_hSched64=0;
		}
		else {
			NotesNativeAPI32.get().Schedule_Free(m_parent.getHandle32(), m_hSched32);
			NotesGC.__objectBeeingBeRecycled(NotesSchedule.class, this);
			m_hSched32=0;
		}
	}

	void checkHandle() {
		if (m_parent.isRecycled())
			throw new NotesError(0, "Parent schedule container already recycled");
		
		if (PlatformUtils.is64Bit()) {
			if (m_hSched64==0)
				throw new NotesError(0, "Schedule already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesSchedule.class, m_hSched64);
		}
		else {
			if (m_hSched32==0)
				throw new NotesError(0, "Schedule already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesSchedule.class, m_hSched32);
		}
	}
	
	@Override
	public boolean isRecycled() {
		if (PlatformUtils.is64Bit()) {
			return m_hSched64==0;
		}
		else {
			return m_hSched32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hSched32;
	}

	@Override
	public long getHandle64() {
		return m_hSched64;
	}

	/**
	 * Returns the owner of this schedule in canonical format
	 * 
	 * @return owner
	 */
	public String getOwner() {
		return m_owner;
	}
	
	/**
	 * Returns the owner's mail file replica ID
	 * 
	 * @return replica id
	 */
	public String getDbReplicaId() {
		NotesTimeDateStruct replicaId = m_scheduleData==null ? null : m_scheduleData.dbReplicaID;
		String replicaIdStr = replicaId==null ? null : NotesStringUtils.innardsToReplicaId(replicaId.Innards);
		return replicaIdStr;
	}
	
	/**
	 * Lower bound of the interval
	 * 
	 * @return lower bound
	 */
	public Calendar getFrom() {
		NotesTimeDatePairStruct tdPair = m_scheduleData==null ? null : m_scheduleData.Interval;
		NotesTimeDateStruct lower = tdPair==null ? null : tdPair.Lower;
		return lower==null ? null : lower.toCalendar();
	}
	
	/**
	 * Upper bound of the interval
	 * 
	 * @return upper bound
	 */
	public Calendar getUntil() {
		NotesTimeDatePairStruct tdPair = m_scheduleData==null ? null : m_scheduleData.Interval;
		NotesTimeDateStruct upper = tdPair==null ? null : tdPair.Upper;
		return upper==null ? null : upper.toCalendar();
	}

	/**
	 * Returns an exception if loading the schedule failed
	 * 
	 * @return exception or null
	 */
	public NotesError getError() {
		short err = m_scheduleData==null ? 0 : m_scheduleData.error;
		return NotesErrorUtils.toNotesError(err);
	}
	
	/**
	 * Retrieves a user's busy times stored in this schedule
	 *  
	 * @param unidIgnore UNID to ignore in busy time calculations or null
	 * @param from specifies the start of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @return busy times
	 */
	public List<Calendar[]> extractBusyTimeRange(String unidIgnore, NotesTimeDate from, NotesTimeDate until) {
		checkHandle();
		
		NotesUniversalNoteIdStruct unidStruct = unidIgnore==null ? null : NotesUniversalNoteIdStruct.fromString(unidIgnore);
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDateStruct fromStruct = NotesTimeDateStruct.newInstance(from.getInnards());
		NotesTimeDateStruct untilStruct = NotesTimeDateStruct.newInstance(until.getInnards());
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = fromStruct;
		intervalPair.Upper = untilStruct;
		intervalPair.write();

		short result;

		List<Calendar[]> allRanges = new ArrayList<Calendar[]>();
		
		IntByReference retdwSize = new IntByReference();
		IntByReference rethMoreCtx = new IntByReference();
		
		boolean hasMoreData;
		
		//read first piece of busy time
		if (PlatformUtils.is64Bit()) {
			LongByReference rethRange = new LongByReference();
			result = NotesNativeAPI64.get().Schedule_ExtractBusyTimeRange(m_parent.getHandle64(), (int) m_hSched64,
					unidStruct, intervalPair,
					retdwSize, rethRange, rethMoreCtx);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMoreCtx.getValue()!=0;
			long hRange = rethRange.getValue();
			if (hRange!=0) {
				Pointer rangePtr = Mem64.OSLockObject(hRange);
				try {
					List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
					for (Object currObj : currentRange) {
						if (currObj instanceof Calendar[]) {
							allRanges.add((Calendar[]) currObj);
						}
					}
				}
				finally {
					Mem64.OSUnlockObject(hRange);
					result = Mem64.OSMemFree(hRange);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		else {
			IntByReference rethRange = new IntByReference();
			result = NotesNativeAPI32.get().Schedule_ExtractBusyTimeRange(m_parent.getHandle32(), (int) m_hSched32,
					unidStruct, intervalPair,
					retdwSize, rethRange, rethMoreCtx);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMoreCtx.getValue()!=0;
			int hRange = rethRange.getValue();
			if (hRange!=0) {
				Pointer rangePtr = Mem32.OSLockObject(hRange);
				try {
					List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
					for (Object currObj : currentRange) {
						if (currObj instanceof Calendar[]) {
							allRanges.add((Calendar[]) currObj);
						}
					}
				}
				finally {
					Mem32.OSUnlockObject(hRange);
					result = Mem32.OSMemFree(hRange);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		
		while (hasMoreData) {
			//read more data
			if (PlatformUtils.is64Bit()) {
				LongByReference rethRange = new LongByReference();
				result = NotesNativeAPI64.get().Schedule_ExtractMoreBusyTimeRange(m_parent.getHandle64(), rethMoreCtx.getValue(), unidStruct,
						intervalPair,
						retdwSize, rethRange, rethMoreCtx);
				NotesErrorUtils.checkResult(result);
				
				hasMoreData = rethMoreCtx.getValue()!=0;
				long hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = Mem64.OSLockObject(hRange);
					try {
						List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
						for (Object currObj : currentRange) {
							if (currObj instanceof Calendar[]) {
								allRanges.add((Calendar[]) currObj);
							}
						}
					}
					finally {
						Mem64.OSUnlockObject(hRange);
						result = Mem64.OSMemFree(hRange);
						NotesErrorUtils.checkResult(result);
					}
				}
			}
			else {
				IntByReference rethRange = new IntByReference();
				result = NotesNativeAPI32.get().Schedule_ExtractMoreBusyTimeRange(m_parent.getHandle32(), rethMoreCtx.getValue(), unidStruct,
						intervalPair, retdwSize, rethRange, rethMoreCtx);
				NotesErrorUtils.checkResult(result);
				
				hasMoreData = rethMoreCtx.getValue()!=0;
				int hRange = rethRange.getValue();
				if (hRange!=0) {
					Pointer rangePtr = Mem32.OSLockObject(hRange);
					try {
						List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
						for (Object currObj : currentRange) {
							if (currObj instanceof Calendar[]) {
								allRanges.add((Calendar[]) currObj);
							}
						}
					}
					finally {
						Mem32.OSUnlockObject(hRange);
						result = Mem32.OSMemFree(hRange);
						NotesErrorUtils.checkResult(result);
					}
				}
			}
		}
		
		return allRanges;
	}
	
	/**
	 * This routine retrieves one or more free time ranges from a schedule.<br>
	 * It will only return 64k of free time ranges.<br>
	 * Note: submitting a range or time that is in the past is not supported.
	 * 
	 * @param unidIgnore UNID to ignore in busy time calculation or null
	 * @param findFirstFit  If true then only the first fit is used
	 * @param from specifies the start of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param duration How much free time you are looking for, in minutes (max 65535).
	 * @return timedate pairs indicating runs of free time
	 */
	public List<Calendar[]> extractFreeTimeRange(String unidIgnore,
			boolean findFirstFit, NotesTimeDate from, NotesTimeDate until, int duration) {

		checkHandle();
		
		NotesUniversalNoteIdStruct unidStruct = unidIgnore==null ? null : NotesUniversalNoteIdStruct.fromString(unidIgnore);
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDateStruct fromStruct = NotesTimeDateStruct.newInstance(from.getInnards());
		NotesTimeDateStruct untilStruct = NotesTimeDateStruct.newInstance(until.getInnards());
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = fromStruct;
		intervalPair.Upper = untilStruct;
		intervalPair.write();

		if (duration > 65535) {
			throw new IllegalArgumentException("Duration can only have a short value ("+duration+">65535)");
		}

		short result;

		List<Calendar[]> allRanges = new ArrayList<Calendar[]>();
		
		IntByReference retdwSize = new IntByReference();
		
		//read first piece of busy time
		if (PlatformUtils.is64Bit()) {
			LongByReference rethRange = new LongByReference();
			result = NotesNativeAPI64.get().Schedule_ExtractFreeTimeRange(m_parent.getHandle64(), (int) m_hSched64,
					unidStruct, (short) (findFirstFit ? 1 : 0), (short) (duration & 0xffff),
					intervalPair, retdwSize, rethRange);
			NotesErrorUtils.checkResult(result);
			
			long hRange = rethRange.getValue();
			if (hRange!=0) {
				Pointer rangePtr = Mem64.OSLockObject(hRange);
				try {
					List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
					for (Object currObj : currentRange) {
						if (currObj instanceof Calendar[]) {
							allRanges.add((Calendar[]) currObj);
						}
					}
				}
				finally {
					Mem64.OSUnlockObject(hRange);
					result = Mem64.OSMemFree(hRange);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		else {
			IntByReference rethRange = new IntByReference();
			result = NotesNativeAPI32.get().Schedule_ExtractFreeTimeRange(m_parent.getHandle32(), (int) m_hSched32,
					unidStruct, (short) (findFirstFit ? 1 : 0), (short) (duration & 0xffff),
					intervalPair, retdwSize, rethRange);
			NotesErrorUtils.checkResult(result);
			
			int hRange = rethRange.getValue();
			if (hRange!=0) {
				Pointer rangePtr = Mem32.OSLockObject(hRange);
				try {
					List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
					for (Object currObj : currentRange) {
						if (currObj instanceof Calendar[]) {
							allRanges.add((Calendar[]) currObj);
						}
					}
				}
				finally {
					Mem32.OSUnlockObject(hRange);
					result = Mem32.OSMemFree(hRange);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		
		return allRanges;
	}
	
	/**
	 * Internal method to read schedule list entries
	 * 
	 * @param listPtr memory pointer
	 * @return entries
	 */
	private List<NotesScheduleEntry> readSchedList(Pointer listPtr) {
		List<NotesScheduleEntry> decodedEntries = new ArrayList<NotesScheduleEntry>();
		
		NotesScheduleListStruct schedList = NotesScheduleListStruct.newInstance(listPtr);
		schedList.read();
		
		Pointer entriesPtr = listPtr.share(NotesConstants.schedListSize);
		for (int i=0; i<schedList.NumEntries; i++) {
			
			if (schedList.Spare==0) {
				//pre-R6
				NotesSchedEntryStruct entryStruct = NotesSchedEntryStruct.newInstance(entriesPtr);
				entryStruct.read();
				
				NotesScheduleEntry entry = new NotesScheduleEntry(entryStruct);
				decodedEntries.add(entry);
				
				entriesPtr = entriesPtr.share(NotesConstants.schedEntrySize);
			}
			else {
				//extended data structure
				NotesSchedEntryExtStruct entryStruct = NotesSchedEntryExtStruct.newInstance(entriesPtr);
				entryStruct.read();
				
				NotesScheduleEntry entry = new NotesScheduleEntry(entryStruct);
				decodedEntries.add(entry);
				
				entriesPtr = entriesPtr.share(NotesConstants.schedEntryExtSize);
			}
		}
		return decodedEntries;
	}
	
	/**
	 * This retrieves the schedule list from a schedule. A schedule list contains more
	 * appointment details than just from/until times that can be read via {@link #extractBusyTimeRange(String, NotesTimeDate, NotesTimeDate)},
	 * e.g. the UNID/ApptUNID of the appointments.
	 * 
	 * @param from specifies the start of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @param until specifies the end of the range over which the free time search should be performed. In typical scheduling applications, this might be a range of 1 day or 5 days
	 * @return schedule list
	 */
	public List<NotesScheduleEntry> extractScheduleList(NotesTimeDate from, NotesTimeDate until) {
		checkHandle();
		
		if (from==null)
			throw new IllegalArgumentException("from date cannot be null");
		if (until==null)
			throw new IllegalArgumentException("until date cannot be null");
		
		NotesTimeDateStruct fromStruct = NotesTimeDateStruct.newInstance(from.getInnards());
		NotesTimeDateStruct untilStruct = NotesTimeDateStruct.newInstance(until.getInnards());
		
		NotesTimeDatePairStruct intervalPair = NotesTimeDatePairStruct.newInstance();
		intervalPair.Lower = fromStruct;
		intervalPair.Upper = untilStruct;
		intervalPair.write();

		short result;

		List<NotesScheduleEntry> allSchedEntries = new ArrayList<NotesScheduleEntry>();
		
		IntByReference retdwSize = new IntByReference();
		IntByReference rethMore = new IntByReference();
		
		boolean hasMoreData;
		
		//read first piece of busy time
		if (PlatformUtils.is64Bit()) {
			LongByReference rethSchedList = new LongByReference();
			result = NotesNativeAPI64.get().Schedule_ExtractSchedList(m_parent.getHandle64(), (int) m_hSched64,
					intervalPair, retdwSize, rethSchedList, rethMore);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMore.getValue()!=0;
			long hSchedList = rethSchedList.getValue();
			if (hSchedList!=0) {
				Pointer schedListPtr = Mem64.OSLockObject(hSchedList);
				try {
					List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
					allSchedEntries.addAll(currSchedList);
				}
				finally {
					Mem64.OSUnlockObject(hSchedList);
					result = Mem64.OSMemFree(hSchedList);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		else {
			IntByReference rethSchedList = new IntByReference();
			result = NotesNativeAPI32.get().Schedule_ExtractSchedList(m_parent.getHandle32(), (int) m_hSched32,
					intervalPair, retdwSize, rethSchedList, rethMore);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMore.getValue()!=0;
			int hSchedList = rethSchedList.getValue();
			if (hSchedList!=0) {
				Pointer schedListPtr = Mem32.OSLockObject(hSchedList);
				try {
					List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
					allSchedEntries.addAll(currSchedList);
				}
				finally {
					Mem32.OSUnlockObject(hSchedList);
					result = Mem32.OSMemFree(hSchedList);
					NotesErrorUtils.checkResult(result);
				}
			}
		}
		
		while (hasMoreData) {
			//read more data
			if (PlatformUtils.is64Bit()) {
				LongByReference rethSchedList = new LongByReference();
				result = NotesNativeAPI64.get().Schedule_ExtractMoreSchedList(m_parent.getHandle64(), rethMore.getValue(),
						intervalPair, retdwSize, rethSchedList, rethMore);
				NotesErrorUtils.checkResult(result);
				
				hasMoreData = rethMore.getValue()!=0;
				long hSchedList = rethSchedList.getValue();
				if (hSchedList!=0) {
					Pointer schedListPtr = Mem64.OSLockObject(hSchedList);
					try {
						List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
						allSchedEntries.addAll(currSchedList);
					}
					finally {
						Mem64.OSUnlockObject(hSchedList);
						result = Mem64.OSMemFree(hSchedList);
						NotesErrorUtils.checkResult(result);
					}
				}
			}
			else {
				IntByReference rethSchedList = new IntByReference();
				result = NotesNativeAPI32.get().Schedule_ExtractMoreSchedList(m_parent.getHandle32(), rethMore.getValue(),
						intervalPair, retdwSize, rethSchedList, rethMore);
				NotesErrorUtils.checkResult(result);
				
				hasMoreData = rethMore.getValue()!=0;
				int hSchedList = rethSchedList.getValue();
				if (hSchedList!=0) {
					Pointer schedListPtr = Mem32.OSLockObject(hSchedList);
					try {
						List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
						allSchedEntries.addAll(currSchedList);
					}
					finally {
						Mem32.OSUnlockObject(hSchedList);
						result = Mem32.OSMemFree(hSchedList);
						NotesErrorUtils.checkResult(result);
					}
				}
			}
		}
		
		return allSchedEntries;
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesSchedule [recycled]";
		}
		else {
			return "NotesSchedule [handle="+(PlatformUtils.is64Bit() ? m_hSched64 : m_hSched32)+", owner="+m_owner+"]";
		}
	}
}
