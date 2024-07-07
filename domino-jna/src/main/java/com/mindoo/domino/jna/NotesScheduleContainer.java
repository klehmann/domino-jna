package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.INotesErrorConstants;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.NotesNativeAPI;
import com.mindoo.domino.jna.internal.handles.DHANDLE;
import com.mindoo.domino.jna.internal.handles.DHANDLE32;
import com.mindoo.domino.jna.internal.handles.DHANDLE64;
import com.mindoo.domino.jna.internal.structs.NotesScheduleStruct;
import com.mindoo.domino.jna.utils.NotesBusyTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.mindoo.domino.jna.utils.PlatformUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Container for one or more {@link NotesSchedule}'s retrieved via
 * {@link NotesBusyTimeUtils#retrieveSchedules(String, java.util.EnumSet, NotesTimeDate, NotesTimeDate, java.util.List)}
 * 
 * @author Karsten Lehmann
 */
public class NotesScheduleContainer implements IRecyclableNotesObject {
	private DHANDLE m_hCntnr;
	private boolean m_noRecycle;
	
	public NotesScheduleContainer(IAdaptable adaptable) {
		DHANDLE hdl = adaptable.getAdapter(DHANDLE.class);
		if (hdl!=null) {
			m_hCntnr = hdl;
			return;
		}
		throw new NotesError(0, "Unsupported adaptable parameter");
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

		NotesNativeAPI.get().SchContainer_Free(m_hCntnr.getByValue());
		NotesGC.__objectBeeingBeRecycled(NotesScheduleContainer.class, this);
		m_hCntnr = null;
	}

	@Override
	public boolean isRecycled() {
		return m_hCntnr==null || m_hCntnr.isNull();
	}

	public DHANDLE getHandle() {
		return m_hCntnr;
	}
	
	@Override
	public int getHandle32() {
		return m_hCntnr instanceof DHANDLE32 ? ((DHANDLE32)m_hCntnr).hdl : 0;
	}

	@Override
	public long getHandle64() {
		return m_hCntnr instanceof DHANDLE64 ? ((DHANDLE64)m_hCntnr).hdl : 0;
	}

	void checkHandle() {
		if (m_hCntnr==null || m_hCntnr.isNull())
			throw new NotesError(0, "Note already recycled");
		
		if (PlatformUtils.is64Bit()) {
			NotesGC.__b64_checkValidObjectHandle(NotesScheduleContainer.class, getHandle64());
		}
		else {
			NotesGC.__b32_checkValidObjectHandle(NotesScheduleContainer.class, getHandle32());
		}
	}

	/**
	 * This function is used to get a handle to the first schedule object in a container.
	 * 
	 * @return schedule
	 */
	public NotesSchedule getFirstSchedule() {
		checkHandle();
		
		short result;
		
		IntByReference rethObj = new IntByReference();
		
		Memory schedulePtrMem = new Memory(Native.POINTER_SIZE);
		result = NotesNativeAPI.get().SchContainer_GetFirstSchedule(m_hCntnr.getByValue(), rethObj, schedulePtrMem);
		if (result==INotesErrorConstants.ERR_SCHOBJ_NOTEXIST) {
			return null;
		}
		NotesErrorUtils.checkResult(result);
		
		if (rethObj.getValue()==0) {
			return null;
		}
		
		NotesScheduleStruct retpSchedule;
		
		if (PlatformUtils.is64Bit()) {
			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;
			
			Pointer schedulePtr = new Pointer(peer);
			retpSchedule = NotesScheduleStruct.newInstance(schedulePtr);
		}
		else {
			int peer = schedulePtrMem.getInt(0);
			if (peer==0)
				return null;
			
			Pointer schedulePtr = new Pointer(peer);
			retpSchedule = NotesScheduleStruct.newInstance(schedulePtr);
		}
		
		retpSchedule.read();
		
		int scheduleSize = NotesConstants.scheduleSize;
		if (PlatformUtils.isMac() && PlatformUtils.is64Bit()) {
			//on Mac/64, this structure is 4 byte aligned, other's are not
			int remainder = scheduleSize % 4;
			if (remainder > 0) {
				scheduleSize = 4 * (scheduleSize / 4) + 4;
			}
		}
		
		String owner = NotesStringUtils.fromLMBCS(retpSchedule.getPointer().share(scheduleSize), (retpSchedule.wOwnerNameSize-1) & 0xffff);
		
		NotesSchedule schedule=new NotesSchedule(this, retpSchedule, owner, rethObj.getValue());
		NotesGC.__objectCreated(NotesSchedule.class, schedule);
		return schedule;
	}
	
	/**
	 * This routine is used to get a handle to the next schedule object in a container.
	 * 
	 * @param schedule the current schedule
	 * @return next schedule
	 */
	public NotesSchedule getNextSchedule(NotesSchedule schedule) {
		if (schedule.isRecycled())
			throw new NotesError(0, "Specified schedule is recycled");
		
		short result;
		
		IntByReference rethNextSchedule = new IntByReference();

		int hCurSchedule = PlatformUtils.is64Bit() ? (int) schedule.getHandle64() : schedule.getHandle32();
		Memory schedulePtrMem = new Memory(Native.POINTER_SIZE);
		result = NotesNativeAPI.get().SchContainer_GetNextSchedule(m_hCntnr.getByValue(), hCurSchedule, rethNextSchedule,
				schedulePtrMem);
		if (result==INotesErrorConstants.ERR_SCHOBJ_NOTEXIST) {
			return null;
		}
		NotesErrorUtils.checkResult(result);

		NotesScheduleStruct retpNextSchedule;
		
		if (PlatformUtils.is64Bit()) {
			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;
			
			Pointer schedulePtr = new Pointer(peer);
			retpNextSchedule = NotesScheduleStruct.newInstance(schedulePtr);
		}
		else {
			int peer = schedulePtrMem.getInt(0);
			if (peer==0)
				return null;
			
			Pointer schedulePtr = new Pointer(peer);
			retpNextSchedule = NotesScheduleStruct.newInstance(schedulePtr);
		}
		
		retpNextSchedule.read();
		
		int scheduleSize = NotesConstants.scheduleSize;
		if (PlatformUtils.isMac() && PlatformUtils.is64Bit()) {
			//on Mac/64, this structure is 4 byte aligned, other's are not
			int remainder = scheduleSize % 4;
			if (remainder > 0) {
				scheduleSize = 4 * (scheduleSize / 4) + 4;
			}
		}
		
		String owner = NotesStringUtils.fromLMBCS(retpNextSchedule.getPointer().share(scheduleSize), (retpNextSchedule.wOwnerNameSize-1) & 0xffff);

		NotesSchedule nextSchedule=new NotesSchedule(this, retpNextSchedule, owner, rethNextSchedule.getValue());
		NotesGC.__objectCreated(NotesSchedule.class, nextSchedule);
		return nextSchedule;
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesScheduleContainer [recycled]";
		}
		else {
			return "NotesScheduleContainer [handle="+m_hCntnr+"]";
		}
	}
}
