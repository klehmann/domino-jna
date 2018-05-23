package com.mindoo.domino.jna;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.internal.NotesConstants;
import com.mindoo.domino.jna.internal.structs.NotesCalendarActionDataStruct;
import com.mindoo.domino.jna.internal.structs.NotesTimeDateStruct;
import com.mindoo.domino.jna.utils.NotesCalendarUtils;
import com.mindoo.domino.jna.utils.NotesNamingUtils;
import com.mindoo.domino.jna.utils.NotesStringUtils;
import com.sun.jna.Memory;

/**
 * Additional data required to perform some actions in {@link NotesCalendarUtils},
 * e.g. adding names to meetings and sending counter proposals.
 * 
 * @author Karsten Lehmann
 */
public class NotesCalendarActionData implements IAdaptable {
	private String delegateTo;
	private NotesTimeDate changeToStart;
	private NotesTimeDate changeToEnd;
	private boolean keepInformed;
	private List<String> addNamesReq;
	private List<String> addNamesOpt;
	private List<String> addNamesFYI;
	private List<String> removeNames;

	@Override
	public <T> T getAdapter(Class<T> clazz) {
		if (clazz==NotesCalendarActionDataStruct.ByValue.class || clazz==NotesCalendarActionDataStruct.class) {
			NotesCalendarActionDataStruct struct;
			if (clazz==NotesCalendarActionDataStruct.ByValue.class) {
				struct = NotesCalendarActionDataStruct.ByValue.newInstance();
			}
			else {
				struct = NotesCalendarActionDataStruct.newInstance();
			}

			if (this.delegateTo!=null) {
				struct.pszDelegateTo = NotesStringUtils.toLMBCS(NotesNamingUtils.toCanonicalName(this.delegateTo), true);
			}
			else {
				struct.pszDelegateTo = null;
			}

			if (this.changeToStart!=null) {
				struct.ptdChangeToStart = new Memory(NotesConstants.timeDateSize);
				NotesTimeDateStruct tdStruct = NotesTimeDateStruct.newInstance(struct.ptdChangeToStart);
				tdStruct.Innards = this.changeToStart.getInnards();
				tdStruct.write();
			}

			if (this.changeToEnd!=null) {
				struct.ptdChangeToEnd = new Memory(NotesConstants.timeDateSize);
				NotesTimeDateStruct tdStruct = NotesTimeDateStruct.newInstance(struct.ptdChangeToEnd);
				tdStruct.Innards = this.changeToEnd.getInnards();
				tdStruct.write();
			}

			Memory keepInformed = new Memory(4);
			keepInformed.setInt(0, (this.keepInformed ? 1 : 0));
			struct.pfKeepInformed = keepInformed;

			if (this.addNamesReq!=null && !this.addNamesReq.isEmpty()) {
				struct.pAddNamesReq = createNamesList(this.addNamesReq);
			}
			else {
				struct.pAddNamesReq = null;
			}
			if (this.addNamesOpt!=null && !this.addNamesOpt.isEmpty()) {
				struct.pAddNamesOpt = createNamesList(this.addNamesOpt);
			}
			else {
				struct.pAddNamesOpt = null;
			}
			if (this.addNamesFYI!=null && !this.addNamesFYI.isEmpty()) {
				struct.pAddNamesFYI = createNamesList(this.addNamesFYI);
			}
			else {
				struct.pAddNamesFYI = null;
			}
			if (this.removeNames!=null && !this.removeNames.isEmpty()) {
				struct.pRemoveNames = createNamesList(this.removeNames);
			}
			else {
				struct.pRemoveNames = null;
			}
			struct.write();
			return (T) struct;
		}
		return null;
	}

	private static Memory createNamesList(List<String> names) {
		if (names.size()> 65535) {
			throw new IllegalArgumentException("Max 65535 entries are allowed");
		}

		int totalSize = 2; //WORD for entries in the list
		List<Memory> namesMemList = new ArrayList<Memory>();

		totalSize += 2*names.size(); //WORD indicating the name length for each entry

		for (String currName : names) {
			String currNameCanonical = NotesNamingUtils.toCanonicalName(currName);
			Memory currNameCanonicalMem = NotesStringUtils.toLMBCS(currNameCanonical, false);
			long currNameCanonicalMemSize = currNameCanonicalMem.size();
			if (currNameCanonicalMemSize>65535) {
				throw new IllegalArgumentException("List entry can only be max 65535 chars long: "+currName);
			}
			totalSize += currNameCanonicalMem.size(); //length of actual data
			namesMemList.add(currNameCanonicalMem);
		}

		Memory retMem = new Memory(totalSize);
		int offset = 0;
		retMem.setShort(offset, (short) (names.size() & 0xffff));
		offset+= 2;

		for (Memory currNameMem : namesMemList) {
			retMem.setShort(offset, (short) (currNameMem.size() & 0xffff));
			offset += 2;
		}

		for (Memory currNameMem : namesMemList) {
			byte[] currNameData = currNameMem.getByteArray(0, (int) currNameMem.size());
			retMem.write(offset, currNameData, 0, currNameData.length);
			offset += currNameData.length;
		}
		return retMem;
	}

	public String getDelegateTo() {
		return delegateTo;
	}

	public void setDelegateTo(String delegateTo) {
		this.delegateTo = delegateTo;
	}

	public NotesTimeDate getChangeToStart() {
		return changeToStart;
	}

	public void setChangeToStart(NotesTimeDate changeToStart) {
		this.changeToStart = changeToStart;
	}

	public NotesTimeDate getChangeToEnd() {
		return changeToEnd;
	}

	public void setChangeToEnd(NotesTimeDate changeToEnd) {
		this.changeToEnd = changeToEnd;
	}

	public boolean isKeepInformed() {
		return keepInformed;
	}

	public void setKeepInformed(boolean keepInformed) {
		this.keepInformed = keepInformed;
	}

	public List<String> getAddNamesReq() {
		return addNamesReq;
	}

	public void setAddNamesReq(List<String> addNamesReq) {
		this.addNamesReq = addNamesReq;
	}

	public List<String> getAddNamesOpt() {
		return addNamesOpt;
	}

	public void setAddNamesOpt(List<String> addNamesOpt) {
		this.addNamesOpt = addNamesOpt;
	}

	public List<String> getAddNamesFYI() {
		return addNamesFYI;
	}

	public void setAddNamesFYI(List<String> addNamesFYI) {
		this.addNamesFYI = addNamesFYI;
	}

	public List<String> getRemoveNames() {
		return removeNames;
	}

	public void setRemoveNames(List<String> removeNames) {
		this.removeNames = removeNames;
	}

}
