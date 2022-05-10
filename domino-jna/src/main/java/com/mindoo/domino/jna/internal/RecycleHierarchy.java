package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.List;

import com.mindoo.domino.jna.IAdaptable;
import com.mindoo.domino.jna.errors.NotesError;
import com.mindoo.domino.jna.gc.IRecyclableNotesObject;

/**
 * Utility class to manage recycling hierarchies, e.g. to recycle all open NotesCollection
 * and NotesNote objects when a NotesDatabase is recycled.
 * 
 * @author Karsten Lehmann
 */
public class RecycleHierarchy {
	
	public static void addChild(IAdaptable parent, IRecyclableNotesObject child) {
		if (parent instanceof IRecyclableNotesObject && ((IRecyclableNotesObject)parent).isRecycled()) {
			throw new NotesError("Parent is already recycled: "+parent);
		}
		
		RecycleHierarchy recHierarchy = parent.getAdapter(RecycleHierarchy.class);
		if (recHierarchy!=null) {
			recHierarchy.addChild(child);
		}
	}
	
	public static void removeChild(IAdaptable parent, IRecyclableNotesObject child) {
		RecycleHierarchy recHierarchy = parent.getAdapter(RecycleHierarchy.class);
		if (recHierarchy!=null) {
			recHierarchy.removeChild(child);
		}
	}
	
	private List<IRecyclableNotesObject> children = new ArrayList<>();

	private void addChild(IRecyclableNotesObject obj) {
		children.add(obj);
	}

	private void removeChild(IRecyclableNotesObject obj) {
		children.remove(obj);
	}

	public void recycleChildren() {
		if (children.isEmpty()) {
			return;
		}
		
		IRecyclableNotesObject[] childrenArr = children.toArray(new IRecyclableNotesObject[children.size()]);

		for (IRecyclableNotesObject currChild : childrenArr) {
			if (!currChild.isRecycled()) {
				currChild.recycle();
			}
		}
		children.clear();
	}

}
