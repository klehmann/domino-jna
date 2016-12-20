package com.mindoo.domino.jna.xsp.http;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;

import com.ibm.designer.runtime.domino.adapter.ComponentModule;
import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletRequestAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletResponseAdapter;
import com.ibm.designer.runtime.domino.bootstrap.adapter.HttpSessionAdapter;
import com.mindoo.domino.jna.gc.NotesGC;

/**
 * {@link HttpService} that enables code processing Domino HTTP requests (like XPages) to use the
 * Domino JNA API.
 * 
 * @author Karsten Lehmann
 */
public class DominoJNAHttpService extends HttpService {
	private List<HttpService> services;

	private ThreadLocal<Boolean> doServiceEntered = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}

	};

	public DominoJNAHttpService(final LCDEnvironment lcdEnv) {
		super(lcdEnv);
		this.services = lcdEnv.getServices();

//		SecurityManager oldSecurityMgr = System.getSecurityManager();
//		System.out.println("Current SecurityManager: "+(oldSecurityMgr==null ? "null" : oldSecurityMgr.getClass().getName()));
//		if (oldSecurityMgr instanceof AgentSecurityManager) {
//			try {
//				System.setSecurityManager(new DominoJNASecurityManager((AgentSecurityManager) oldSecurityMgr));
				
//				AccessController.doPrivileged(new PrivilegedAction<Object>() {

//					@Override
//					public Object run() {
//						Enhancer enhancer = new Enhancer();
//						enhancer.setSuperclass(AgentSecurityManager.class);
//						enhancer.setCallback(new MethodInterceptor() {
//
//							@Override
//							public Object intercept(Object obj, Method method, Object[] args, final MethodProxy proxy) throws Throwable {
//								AccessController.doPrivileged(new PrivilegedAction<Object>() {
//
//									@Override
//									public Object run() {
//										//call a method via AccessController block that invokes MethodProxy.init()
//										proxy.getSuperIndex();
//										return null;
//									}
//								});
//								try {
//									return proxy.invokeSuper(obj, args);
//								}
//								catch (SecurityException secEx) {
//									System.out.println("Denied by SecurityManager:");
//									System.out.println(method.getName()+"("+(args==null ? "" : new Vector<Object>(Arrays.asList(args)))+")");
//									secEx.printStackTrace();
//									throw secEx;
//								}
//							}
//						});
//						enhancer.setNamingPolicy(new DominoJNANamingPolicy());
//						enhancer.setClassLoader(DominoJNAActivator.class.getClassLoader());
//						SecurityManager newSecurityMgr = (SecurityManager) enhancer.create();
//						System.setSecurityManager(newSecurityMgr);
//						System.out.println("Registered new SecurityManager");

//						return null;
//					}
//				});
//			}
//			catch (Throwable t) {
//				t.printStackTrace();
//			}
//		}
	}

	@Override
	public void destroyService() {
		// here you can put code that runs when the Http-Task will shut down.
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.designer.runtime.domino.adapter.HttpService#doService(java.lang.String, java.lang.String,
	 * com.ibm.designer.runtime.domino.bootstrap.adapter.HttpSessionAdapter,
	 * com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletRequestAdapter,
	 * com.ibm.designer.runtime.domino.bootstrap.adapter.HttpServletResponseAdapter)
	 */
	@Override
	public boolean doService(final String contextPath, final String path, final HttpSessionAdapter httpSession,
			final HttpServletRequestAdapter httpRequest, final HttpServletResponseAdapter httpResponse) throws ServletException,
	IOException {

		if (doServiceEntered.get().booleanValue()) {
			// prevent recursion (if someone does the same trick)
			return false;
		}

		doServiceEntered.set(Boolean.TRUE);
		final Throwable[] exception = new Throwable[1];

		try {
			System.out.println("doService, path="+contextPath);

			//wrap doService call in NotesGC.runWithAutoGC to automatically clean up allocated handles and memory
			//after the request has been processed
			NotesGC.runWithAutoGC(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					try {
						boolean behindUs = false;
						for (HttpService service : DominoJNAHttpService.this.services) {
							if (behindUs) {
								if (service.doService(contextPath, path, httpSession, httpRequest, httpResponse)) {
									return true;
								}
							}
							if (service == DominoJNAHttpService.this) {
								behindUs = true;
							}
						}
					}
					catch (Throwable t) {
						exception[0] = t;
					}
					return null;
				}

			});
			return false;
		} catch (Throwable e) {
			//should not happen since we have a try/catch in the Callable; only possible if Domino JNA cleanup code is wrong
			e.printStackTrace();
		} finally {
			doServiceEntered.set(Boolean.FALSE);
		}

		if (exception[0] != null) {
			if (exception[0] instanceof RuntimeException) {
				throw (RuntimeException) exception[0];
			}
			else {
				throw new ServletException(exception[0]);
			}
		}
		return false;
	}

	@Override
	public int getPriority() {
		return 0; // NSFService has 99, this must be lower
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.designer.runtime.domino.adapter.HttpService#getModules(java.util.List)
	 */
	@Override
	public void getModules(final List<ComponentModule> paramList) {

	}
}
