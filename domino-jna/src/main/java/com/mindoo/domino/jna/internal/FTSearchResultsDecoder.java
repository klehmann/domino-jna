package com.mindoo.domino.jna.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.mindoo.domino.jna.NoteIdWithScore;
import com.mindoo.domino.jna.constants.FTSearch;
import com.sun.jna.Pointer;

public class FTSearchResultsDecoder {

	public static List<NoteIdWithScore> decodeNoteIdsWithStoreSearchResult(Pointer ptr, 
			EnumSet<FTSearch> searchOptions) {
		
		int numHits = ptr.getInt(0);
		ptr = ptr.share(4);
		
		short flags = ptr.getShort(0);
		ptr = ptr.share(2);

		if ((flags & NotesConstants.FT_RESULTS_EXPANDED) == NotesConstants.FT_RESULTS_EXPANDED) {
			throw new IllegalArgumentException("Domain searches are currently unsupported");
		}
		if ((flags & NotesConstants.FT_RESULTS_URL) == NotesConstants.FT_RESULTS_URL) {
			throw new IllegalArgumentException("FT results with URL are currently unsupported");
		}

		short varLength = ptr.getShort(0);
		ptr = ptr.share(2);
		
		List<Integer> noteIds = new ArrayList<Integer>();
		List<Integer> scores = new ArrayList<Integer>();
		
		for (int i=0; i<numHits; i++) {
			int currNoteId = ptr.getInt(0);
			noteIds.add(currNoteId);
			ptr = ptr.share(4);
		}
		
		if ((flags & NotesConstants.FT_RESULTS_SCORES) == NotesConstants.FT_RESULTS_SCORES) {
			for (int i=0; i<numHits; i++) {
				byte currScore = ptr.getByte(0);
				scores.add((int) (currScore & 0xff));
				ptr = ptr.share(1);
			}
		}

		List<NoteIdWithScore> noteIdsWithScore = new ArrayList<NoteIdWithScore>();
		for (int i=0; i<numHits; i++) {
			int currNoteId = noteIds.get(i);
			int currScore = (i < scores.size()) ? scores.get(i) : 0;
			
			noteIdsWithScore.add(new NoteIdWithScore(currNoteId, currScore));
		}
		
		return noteIdsWithScore;
	}
}
