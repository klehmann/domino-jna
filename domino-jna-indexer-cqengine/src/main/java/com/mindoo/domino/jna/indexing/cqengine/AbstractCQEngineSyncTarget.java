package com.mindoo.domino.jna.indexing.cqengine;

import static com.googlecode.cqengine.query.QueryFactory.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.index.Index;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;
import com.mindoo.domino.jna.IItemTableData;
import com.mindoo.domino.jna.NotesIDTable;
import com.mindoo.domino.jna.NotesNote;
import com.mindoo.domino.jna.NotesTimeDate;
import com.mindoo.domino.jna.sync.ISyncTarget;
import com.mindoo.domino.jna.sync.NotesOriginatorIdData;

/**
 * Generic base class that implements {@link ISyncTarget} and syncs data with
 * CQEngine.
 * 
 * @author Karsten Lehmann
 * @param <T> data object type
 * @param <CTX> sync context type
 */
public abstract class AbstractCQEngineSyncTarget<T extends BaseIndexObject, CTX> implements ISyncTarget<CTX> {
	public final Attribute<BaseIndexObject, String> OBJ_UNID = new SimpleAttribute<BaseIndexObject, String>("unid") {
		public String getValue(BaseIndexObject obj, QueryOptions queryOptions) {
			return obj.getUNID();
		}
	};

	public final Attribute<BaseIndexObject, Integer> OBJ_SEQ = new SimpleAttribute<BaseIndexObject, Integer>("seq") {
		public Integer getValue(BaseIndexObject obj, QueryOptions queryOptions) {
			return obj.getSequence();
		}
	};
	
	private IndexedCollection<T> m_indexCollection;

	//some data we need to the sync process
	private String m_lastSyncDbReplicaId;
	private String m_lastSyncSelectionFormula;
	private Map<String,NotesTimeDate> m_lastSyncEndDates;
	
	//pending updates for current sync run
	private ThreadLocal<List<T>> m_objectsToAdd = new ThreadLocal<List<T>>();
	private ThreadLocal<List<T>> m_objectsToRemove = new ThreadLocal<List<T>>();
	private ThreadLocal<Boolean> m_wiped = new ThreadLocal<Boolean>();
	
	//use lock to prevent parallel indexing in multiple threads
	private ReentrantLock m_indexLock = new ReentrantLock();
	private volatile boolean m_initialSync = true;
	
	public AbstractCQEngineSyncTarget() {
		m_lastSyncEndDates = new HashMap<String, NotesTimeDate>();
		
		m_indexCollection = createCollection();
		
		//make sure we have an index for the UNID
		m_indexCollection.addIndex((Index<T>) HashIndex.onAttribute(OBJ_UNID));
		addIndices(m_indexCollection);
	}

	/**
	 * Override this method and return a different collection implementation
	 * in case you want the collection to be persistent.
	 * 
	 * @return indexed collection
	 */
	protected IndexedCollection<T> createCollection() {
		return new ConcurrentIndexedCollection<T>();
	}
	
	/**
	 * Override this method to add your own indices that CQEngine uses
	 * to process queries fast. The default implementation does nothing, but we
	 * already add an index for {@link #OBJ_UNID} before calling this method.
	 * 
	 * @param collection collection
	 */
	protected void addIndices(IndexedCollection<T> collection) {
		//
	}
	
	@Override
	public NotesIDTable getInitialNoteIdFilter() {
		return null;
	}
	
	@Override
	public String getLastSyncDbReplicaId() {
		return m_lastSyncDbReplicaId;
	}

	/**
	 * Override this method and store the db replica id permanently when the index
	 * is a persistent one.
	 * 
	 * @param dbReplicaId new replica id
	 */
	protected void setLastSyncDbReplicaId(String dbReplicaId) {
		m_lastSyncDbReplicaId = dbReplicaId;
	}

	@Override
	public String getLastSyncSelectionFormula() {
		return m_lastSyncSelectionFormula;
	}

	/**
	 * Override this method and store the formula permanently when the index
	 * is a persistent one.
	 * 
	 * @param formula new formula
	 */
	protected void setLastSyncSelectionFormula(String formula) {
		m_lastSyncSelectionFormula = formula;
	}
	
	@Override
	public NotesTimeDate getLastSyncEndDate(String dbInstanceId) {
		return m_lastSyncEndDates.get(dbInstanceId);
	}

	/**
	 * Override this method and store the sync end date permanently when the index
	 * is a persistent one.
	 * 
	 * @param dbInstanceId db instance id
	 * @param date new sync end date
	 */
	protected void setLastSyncEndDate(String dbInstanceId, NotesTimeDate date) {
		m_lastSyncEndDates.put(dbInstanceId, date);
	}
	
	@Override
	public CTX startingSync(String dbReplicaId) {
		m_indexLock.lock();
		
		setLastSyncDbReplicaId(dbReplicaId);
		
		m_objectsToAdd.set(new ArrayList<T>());
		m_objectsToRemove.set(new ArrayList<T>());
		m_wiped.set(null);
		
		//optional context object not used
		return null;
	}
	
	@Override
	public void clear(Object ctx) {
		//remember to wipe the collection on sync end before adding data
		m_wiped.set(Boolean.TRUE);
	}

	@Override
	public Collection<NotesOriginatorIdData> scanTargetData(Object ctx) {
		List<NotesOriginatorIdData> oids = new ArrayList<NotesOriginatorIdData>();
		for (BaseIndexObject currObj : m_indexCollection) {
			oids.add(new NotesOriginatorIdData(currObj.getUNID(), currObj.getSequence(), currObj.getSequenceTimeInnards()));
		}
		return oids;
	}

	@Override
	public abstract EnumSet<DataToRead> getWhichDataToRead();

	private T findObject(NotesOriginatorIdData oid) {
		if (m_initialSync)
			return null;
		if (Boolean.TRUE.equals(m_wiped.get()))
			return null;
		
		Query<T> query = (Query<T>) equal(OBJ_UNID, oid.getUNID());

		ResultSet<T> objectWithUNID = m_indexCollection.retrieve(query);
		if (objectWithUNID.isNotEmpty()) {
			T obj = objectWithUNID.iterator().next();
			return obj;
		}
		return null;
	}

	@Override
	public TargetResult noteChangedMatchingFormula(Object ctx, NotesOriginatorIdData oid, IItemTableData summaryBufferData,
			NotesNote note) {
		
		T obj = findObject(oid);
		
		boolean oldRemoved = false;
		boolean add = true;

		if (obj!=null) {
			if (obj.getSequence() < oid.getSequence()) {
				//remove existing older object
				m_objectsToRemove.get().add(obj);
				if (isLoggable(Level.FINE))
					log(Level.FINE, "Removing entry: "+obj);
				oldRemoved=true;
			}
			else {
				//our version is the same or newer (maybe coming from another DB replica), so keep it
				add = false;
			}
		}
		
		if (add) {
			T newObj = toObject(oid, summaryBufferData, note);
			if (newObj!=null) {
				m_objectsToAdd.get().add(newObj);
				if (isLoggable(Level.FINE))
					log(Level.FINE, "Adding entry: "+newObj);
				return oldRemoved ? TargetResult.Updated : TargetResult.Added;
			}
			else {
				return oldRemoved ? TargetResult.Removed : TargetResult.None;
			}
		}
		else {
			return TargetResult.None;
		}
	}

	/**
	 * Implement this method to convert summary buffer or {@link NotesNote} data
	 * to an index object.
	 * 
	 * @param oid originator id of Domino data
	 * @param summaryBufferData summary buffer if {@link #getWhichDataToRead()} returned {@link DataToRead#SummaryBufferAllItems} or {@link DataToRead#SummaryBufferSelectedItems}, null otherwise
	 * @param note note if {@link #getWhichDataToRead()} returned {@link DataToRead#NoteWithAllItems} or {@link DataToRead#NoteWithSummaryItems}, null otherwise
	 * @return index object or null if unsupported / irrelevant data
	 */
	protected abstract T toObject(NotesOriginatorIdData oid, IItemTableData summaryBufferData, NotesNote note);
	
	
	@Override
	public TargetResult noteChangedNotMatchingFormula(Object ctx, NotesOriginatorIdData oid) {
		//check if an older version of this note is already in the index and
		//needs to be removed since it does not match the selection formula anymore
		T obj = findObject(oid);
		if (obj!=null) {
			m_objectsToRemove.get().add(obj);
			if (isLoggable(Level.FINE))
				log(Level.FINE, "Removing entry: "+obj);
			return TargetResult.Removed;
		}
		return TargetResult.None;
	}

	@Override
	public TargetResult noteDeleted(Object ctx, NotesOriginatorIdData oid) {
		//check if an older version of this note is already in the index and
		//needs to be removed because the note got deleted
		T obj = findObject(oid);
		if (obj!=null) {
			m_objectsToRemove.get().add(obj);
			if (isLoggable(Level.FINE))
				log(Level.FINE, "Removing entry: "+obj);
			return TargetResult.Removed;
		}
		return TargetResult.None;
	}

	@Override
	public boolean isLoggable(Level level) {
		return level.intValue() >= Level.WARNING.intValue();
	}

	@Override
	public void log(Level level, String msg) {
		if (!isLoggable(level))
			return;
		System.out.println(level.getLocalizedName()+": "+msg);
	}

	@Override
	public void log(Level level, String msg, Throwable t) {
		if (!isLoggable(level))
			return;
		System.out.println(level.getLocalizedName()+": "+msg);
		if (t!=null) {
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			t.printStackTrace(pWriter);
			System.out.println(sWriter.toString());
		}
	}

	@Override
	public void abort(Object ctx, Throwable t) {
		m_objectsToAdd.set(null);
		m_objectsToRemove.set(null);
		m_wiped.set(null);
		
		log(Level.SEVERE, "Sync error occurred in CQEngine sync target.", t);
		
		m_indexLock.unlock();
	}

	/**
	 * Method to check whether this sync target has received the first set of
	 * sync data
	 * 
	 * @return true if done
	 */
	public boolean isInitialSyncDone() {
		return !m_initialSync;
	}
	
	@Override
	public void endingSync(Object ctx, String selectionFormulaForNextSync, String dbInstanceId,
			NotesTimeDate startingDateForNextSync) {
		
		setLastSyncSelectionFormula(selectionFormulaForNextSync);
		setLastSyncEndDate(dbInstanceId, startingDateForNextSync);
		
		if (Boolean.TRUE.equals(m_wiped.get())) {
			m_indexCollection.clear();
		}
		m_indexCollection.update(m_objectsToRemove.get(), m_objectsToAdd.get());

		m_objectsToAdd.set(null);
		m_objectsToRemove.set(null);
		m_wiped.set(null);
		m_initialSync = false;
		
		log(Level.FINE, "Sync done in CQEngine sync target");
		
		m_indexLock.unlock();
	}

	/**
	 * Method to filter the internal CQEngine index collection using any indices
	 * you defined in {@link #addIndices(IndexedCollection)}.
	 * 
	 * @param query CQEngine query
	 * @return result set
	 */
	public ResultSet<T> retrieve(Query<T> query) {
		return m_indexCollection.retrieve(query);
	}

	/**
	 * Method to filter the internal CQEngine index collection using any indices
	 * you defined in {@link #addIndices(IndexedCollection)}.
	 * 
	 * @param query CQEngine query
	 * @param queryOptions query options
	 * @return result set
	 */
	public ResultSet<T> retrieve(Query<T> query, QueryOptions queryOptions) {
		return m_indexCollection.retrieve(query, queryOptions);
	}

	/**
	 * Returns the an unmodifiable version of the internal CQEngine index collection
	 * 
	 * @return index collection
	 */
	@SuppressWarnings("unchecked")
	public Collection<T> getContent() {
		return Collections.unmodifiableCollection(m_indexCollection);
	}
}
