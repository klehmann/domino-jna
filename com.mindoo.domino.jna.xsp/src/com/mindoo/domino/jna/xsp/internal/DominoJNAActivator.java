package com.mindoo.domino.jna.xsp.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

import com.mindoo.domino.jna.virtualviews.VirtualViewFactory;

public class DominoJNAActivator extends Plugin {
	public static final String PLUGIN_ID = "com.mindoo.domino.jna.xsp";
	
	private static DominoJNAActivator plugin;
	private static Class m_jnaNativeClazz;
	
	private CleanupVirtualViewsJob cleanupVirtualViewJob;
	
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		if (m_jnaNativeClazz==null) {
			m_jnaNativeClazz = AccessController.doPrivileged(new PrivilegedAction<Class>() {

				@Override
				public Class run() {
					//enforce using the extracted JNA .dll/.so file instead of what we find on the PATH
					System.setProperty("jna.nosys", "true");
					//change the library name from the default "jnidispatch" to our own name, so that
					//JNA does not load an jnidispatch.dll from the Server's program directory
					String oldLibName = System.getProperty("jna.boot.library.name", "jnidispatch");
					System.setProperty("jna.boot.library.name", "dominojnadispatch");
					try {
						//loading the Native class runs its static code that extracts and loads the jna dll
						return DominoJNAActivator.class.forName("com.sun.jna.Native");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					finally {
						System.setProperty("jna.boot.library.name",oldLibName);
					}
					return null;
				}
			});
		}
		
		startVirtualViewCleanupJob();
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		stopVirtualViewCleanupJob();
		
		plugin = null;
		super.stop(context);
	}
	
	public static DominoJNAActivator getDefault() {
		return plugin;
	}

	public void startVirtualViewCleanupJob() {
		if (cleanupVirtualViewJob == null) {
			cleanupVirtualViewJob = new CleanupVirtualViewsJob();
			cleanupVirtualViewJob.schedule();
		}
	}
	
	public void stopVirtualViewCleanupJob() {
		if (cleanupVirtualViewJob != null) {
			cleanupVirtualViewJob.cancel();
			cleanupVirtualViewJob = null;
		}
	}
	
	public class CleanupVirtualViewsJob extends Job {

	    public CleanupVirtualViewsJob() {
	        super("Cleanup Domino JNA Virtual Views");
	        setSystem(true);
	    }

	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
	        VirtualViewFactory.INSTANCE.cleanupExpiredViews();

	        // Reschedule the job to run again in 1 minute
	        schedule(60000);
	        
	        return Status.OK_STATUS;
	    }
	}

}
