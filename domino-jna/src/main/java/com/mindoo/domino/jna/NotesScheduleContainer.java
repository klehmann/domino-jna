package com.mindoo.domino.jna;

import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.errors.NotesErrorUtils;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;
import com.mindoo.domino.jna.gc.NotesGC;
import com.mindoo.domino.jna.internal.NotesCAPI;
import com.mindoo.domino.jna.internal.NotesJNAContext;
import com.mindoo.domino.jna.structs.NotesScheduleStruct;
import com.mindoo.domino.jna.utils.NotesBusyTimeUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * Container for one or more {@link NotesSchedule}'s retrieved via
 * {@link NotesBusyTimeUtils#retrieveSchedules(String, java.util.EnumSet, NotesTimeDate, NotesTimeDate, java.util.List)}
 * 
 * @author Karsten Lehmann
 */
public class NotesScheduleContainer implements IRecyclableNotesObject {
	private long m_hCntnr64;
	private int m_hCntnr32;
	private boolean m_noRecycle;
	
	public NotesScheduleContainer(long hCntnr64) {
		if (!NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 64bit only");
		m_hCntnr64 = hCntnr64;
	}

	public NotesScheduleContainer(int hCntnr32) {
		if (NotesJNAContext.is64Bit())
			throw new IllegalStateException("Constructor is 32bit only");
		m_hCntnr32 = hCntnr32;
	}

	public void setNoRecycle() {
		m_noRecycle=true;
	}

	@Override
	public void recycle() {
		if (m_noRecycle || isRecycled())
			return;

		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		if (NotesJNAContext.is64Bit()) {
			notesAPI.b64_SchContainer_Free(m_hCntnr64);
			NotesGC.__objectBeeingBeRecycled(NotesScheduleContainer.class, this);
			m_hCntnr64=0;
		}
		else {
			notesAPI.b32_SchContainer_Free(m_hCntnr32);
			NotesGC.__objectBeeingBeRecycled(NotesScheduleContainer.class, this);
			m_hCntnr32=0;
		}
	}

	@Override
	public boolean isRecycled() {
		if (NotesJNAContext.is64Bit()) {
			return m_hCntnr64==0;
		}
		else {
			return m_hCntnr32==0;
		}
	}

	@Override
	public int getHandle32() {
		return m_hCntnr32;
	}

	@Override
	public long getHandle64() {
		return m_hCntnr64;
	}

	void checkHandle() {
		if (NotesJNAContext.is64Bit()) {
			if (m_hCntnr64==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b64_checkValidObjectHandle(NotesNote.class, m_hCntnr64);
		}
		else {
			if (m_hCntnr32==0)
				throw new NotesError(0, "Note already recycled");
			NotesGC.__b32_checkValidObjectHandle(NotesNote.class, m_hCntnr32);
		}
	}

	/**
	 * This function is used to get a handle to the first schedule object in a container.
	 * 
	 * @return schedule
	 */
	public NotesSchedule getFirstSchedule() {
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		IntByReference rethObj = new IntByReference();
		if (NotesJNAContext.is64Bit()) {
			Memory schedulePtrMem = new Memory(Pointer.SIZE);
			result = notesAPI.b64_SchContainer_GetFirstSchedule(m_hCntnr64, rethObj, schedulePtrMem);
			NotesErrorUtils.checkResult(result);
			
			if (rethObj.getValue()==0) {
				return null;
			}
			
			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;
			Pointer schedulePtr = new Pointer(peer);
			NotesScheduleStruct retpSchedule = NotesScheduleStruct.newInstance(schedulePtr);
			retpSchedule.read();
			
			String owner = NotesStringUtils.fromLMBCS(retpSchedule.getPointer().share(NotesCAPI.scheduleSize), (retpSchedule.wOwnerNameSize-1) & 0xffff);
			
			NotesSchedule schedule=new NotesSchedule(this, retpSchedule, owner, (long) rethObj.getValue());
			NotesGC.__objectCreated(NotesSchedule.class, schedule);
			return schedule;
		}
		else {
			Memory schedulePtrMem = new Memory(Pointer.SIZE);
			result = notesAPI.b32_SchContainer_GetFirstSchedule(m_hCntnr32, rethObj, schedulePtrMem);
			NotesErrorUtils.checkResult(result);

			if (rethObj.getValue()==0) {
				return null;
			}
			
			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;
			Pointer schedulePtr = new Pointer(peer);
			NotesScheduleStruct retpSchedule = NotesScheduleStruct.newInstance(schedulePtr);
			retpSchedule.read();
			
			String owner = NotesStringUtils.fromLMBCS(retpSchedule.getPointer().share(NotesCAPI.scheduleSize), (retpSchedule.wOwnerNameSize-1) & 0xffff);

			NotesSchedule schedule=new NotesSchedule(this, retpSchedule, owner, (int) rethObj.getValue());
			NotesGC.__objectCreated(NotesSchedule.class, schedule);
			return schedule;
		}
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
		
		NotesCAPI notesAPI = NotesJNAContext.getNotesAPI();
		short result;
		
		IntByReference rethNextSchedule = new IntByReference();

		if (NotesJNAContext.is64Bit()) {
			int hCurSchedule = (int) schedule.getHandle64();
			Memory schedulePtrMem = new Memory(Pointer.SIZE);
			result = notesAPI.b64_SchContainer_GetNextSchedule(m_hCntnr64, hCurSchedule, rethNextSchedule,
					schedulePtrMem);
			NotesErrorUtils.checkResult(result);

			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;
			
			Pointer schedulePtr = new Pointer(peer);
			NotesScheduleStruct retpNextSchedule = NotesScheduleStruct.newInstance(schedulePtr);
			retpNextSchedule.read();
			
			String owner = NotesStringUtils.fromLMBCS(retpNextSchedule.getPointer().share(NotesCAPI.scheduleSize), (retpNextSchedule.wOwnerNameSize-1) & 0xffff);

			NotesSchedule nextSchedule=new NotesSchedule(this, retpNextSchedule, owner, (long) rethNextSchedule.getValue());
			NotesGC.__objectCreated(NotesSchedule.class, nextSchedule);
			return nextSchedule;
		}
		else {
			int hCurSchedule = (int) schedule.getHandle64();
			Memory schedulePtrMem = new Memory(Pointer.SIZE);
			result = notesAPI.b32_SchContainer_GetNextSchedule(m_hCntnr32, hCurSchedule, rethNextSchedule,
					schedulePtrMem);
			NotesErrorUtils.checkResult(result);

			long peer = schedulePtrMem.getLong(0);
			if (peer==0)
				return null;

			Pointer schedulePtr = new Pointer(peer);
			NotesScheduleStruct retpNextSchedule = NotesScheduleStruct.newInstance(schedulePtr);
			retpNextSchedule.read();
			
			String owner = NotesStringUtils.fromLMBCS(retpNextSchedule.getPointer().share(NotesCAPI.scheduleSize), (retpNextSchedule.wOwnerNameSize-1) & 0xffff);

			NotesSchedule nextSchedule=new NotesSchedule(this, retpNextSchedule, owner, rethNextSchedule.getValue());
			NotesGC.__objectCreated(NotesSchedule.class, nextSchedule);
			return nextSchedule;
		}
	}
	
	@Override
	public String toString() {
		if (isRecycled()) {
			return "NotesScheduleContainer [recycled]";
		}
		else {
			return "NotesScheduleContainer [handle="+(NotesJNAContext.is64Bit() ? m_hCntnr64 : m_hCntnr32)+"]";
		}
	}
}
