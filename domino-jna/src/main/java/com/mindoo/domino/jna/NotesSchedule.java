package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.ItemDecoder;
import com.mindoo.domino.jna.internal.Mem;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
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

/**
 * Schedule object to read busy and free time info for a single Domino user
 * 
 * @author Karsten Lehmann
 */
public class NotesSchedule implements IRecyclableNotesObject {
	private NotesScheduleContainer m_parent;
	private int m_hSched;
	private boolean m_noRecycle;
	private NotesScheduleStruct m_scheduleData;
	private String m_owner;
	
	NotesSchedule(NotesScheduleContainer parent, IAdaptable scheduleData, String owner, int hSchedule) {
		m_parent = parent;
		m_hSched = hSchedule;
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

		NotesNativeAPI.get().Schedule_Free(m_parent.getHandle().getByValue(), (int) m_hSched);
		NotesGC.__objectBeeingBeRecycled(NotesSchedule.class, this);
		m_hSched=0;
	}

	void checkHandle() {
		if (m_parent.isRecycled())
			throw new NotesError(0, "Parent schedule container already recycled");
		
		if (m_hSched==0)
			throw new NotesError(0, "Schedule already recycled");
		
		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidObjectHandle(NotesSchedule.class, m_hSched);
		}
		else {
			NotesGC.__b32_checkValidObjectHandle(NotesSchedule.class, m_hSched);
		}
	}
	
	@Override
	public boolean isRecycled() {
		if (PlatformUtils.is64Bit()) {
			return m_hSched==0;
		}
		else {
			return m_hSched==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hSched;
	}

	@Override
	public long getHandle64() {
		return m_hSched;
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

		DHANDLE.ByReference rethRange = DHANDLE.newInstanceByReference();
		result = NotesNativeAPI.get().Schedule_ExtractBusyTimeRange(m_parent.getHandle().getByValue(),
				(int) m_hSched,
				unidStruct, intervalPair,
				retdwSize, rethRange, rethMoreCtx);
		NotesErrorUtils.checkResult(result);
		
		hasMoreData = rethMoreCtx.getValue()!=0;

		if (!rethRange.isNull()) {
			Pointer rangePtr = Mem.OSLockObject(rethRange);
			try {
				List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
				for (Object currObj : currentRange) {
					if (currObj instanceof Calendar[]) {
						allRanges.add((Calendar[]) currObj);
					}
				}
			}
			finally {
				Mem.OSUnlockObject(rethRange);
				result = Mem.OSMemFree(rethRange.getByValue());
				NotesErrorUtils.checkResult(result);
			}
		}
		
		while (hasMoreData) {
			//read more data
			rethRange = DHANDLE.newInstanceByReference();
			result = NotesNativeAPI.get().Schedule_ExtractMoreBusyTimeRange(m_parent.getHandle().getByValue(),
					rethMoreCtx.getValue(), unidStruct,
					intervalPair,
					retdwSize, rethRange, rethMoreCtx);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMoreCtx.getValue()!=0;

			if (!rethRange.isNull()) {
				Pointer rangePtr = Mem.OSLockObject(rethRange);
				try {
					List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
					for (Object currObj : currentRange) {
						if (currObj instanceof Calendar[]) {
							allRanges.add((Calendar[]) currObj);
						}
					}
				}
				finally {
					Mem.OSUnlockObject(rethRange);
					result = Mem.OSMemFree(rethRange.getByValue());
					NotesErrorUtils.checkResult(result);
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

		DHANDLE.ByReference rethRange = DHANDLE.newInstanceByReference();
		result = NotesNativeAPI.get().Schedule_ExtractFreeTimeRange(m_parent.getHandle().getByValue(),
				m_hSched,
				unidStruct, (short) (findFirstFit ? 1 : 0), (short) (duration & 0xffff),
				intervalPair, retdwSize, rethRange);
		NotesErrorUtils.checkResult(result);

		if (!rethRange.isNull()) {
			Pointer rangePtr = Mem.OSLockObject(rethRange);
			try {
				List<Object> currentRange = ItemDecoder.decodeTimeDateList(rangePtr);
				for (Object currObj : currentRange) {
					if (currObj instanceof Calendar[]) {
						allRanges.add((Calendar[]) currObj);
					}
				}
			}
			finally {
				Mem.OSUnlockObject(rethRange);
				result = Mem.OSMemFree(rethRange.getByValue());
				NotesErrorUtils.checkResult(result);
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
		DHANDLE.ByReference rethSchedList = DHANDLE.newInstanceByReference();
		result = NotesNativeAPI.get().Schedule_ExtractSchedList(m_parent.getHandle().getByValue(),
				m_hSched,
				intervalPair, retdwSize, rethSchedList, rethMore);
		NotesErrorUtils.checkResult(result);
		
		hasMoreData = rethMore.getValue()!=0;

		if (!rethSchedList.isNull()) {
			Pointer schedListPtr = Mem.OSLockObject(rethSchedList);
			try {
				List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
				allSchedEntries.addAll(currSchedList);
			}
			finally {
				Mem.OSUnlockObject(rethSchedList);
				result = Mem.OSMemFree(rethSchedList.getByValue());
				NotesErrorUtils.checkResult(result);
			}
		}
		
		while (hasMoreData) {
			//read more data
			rethSchedList = DHANDLE.newInstanceByReference();
			result = NotesNativeAPI.get().Schedule_ExtractMoreSchedList(m_parent.getHandle().getByValue(),
					rethMore.getValue(),
					intervalPair, retdwSize, rethSchedList, rethMore);
			NotesErrorUtils.checkResult(result);
			
			hasMoreData = rethMore.getValue()!=0;

			if (!rethSchedList.isNull()) {
				Pointer schedListPtr = Mem.OSLockObject(rethSchedList);
				try {
					List<NotesScheduleEntry> currSchedList = readSchedList(schedListPtr);
					allSchedEntries.addAll(currSchedList);
				}
				finally {
					Mem.OSUnlockObject(rethSchedList);
					result = Mem.OSMemFree(rethSchedList.getByValue());
					NotesErrorUtils.checkResult(result);
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
			return "NotesSchedule [handle="+m_hSched+", owner="+m_owner+"]";
		}
	}
}
