/*
 * Copyright 2009-2011 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.ObjBase;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Ole32Util;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 *
 * @author scott.palmer@digital-rapids.com
 */
public class ComObject implements InvocationHandler {
    static final Logger logger = Logger.getLogger(ComObject.class.getName());
    static Ole32 OLE32 = Ole32.INSTANCE;
    static ThreadLocal<Boolean> comInitialized = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    static public boolean isComInitialized() {
        return comInitialized.get();
    }

    static public HRESULT initializeCOM() {
        comInitialized.set(true);
        return Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, 0);
    }

    static private int threadNum = 0;
    static final public ThreadFactory comThreadFactory = new ThreadFactory() {

                int num;

                @Override
                public Thread newThread(final Runnable r) {
                    comThread = createComThread(r);
                    return comThread;
                }
            };
    static boolean singleThreaded = Boolean.getBoolean("jnacom.singleThreaded");
    /** For single threaded mode this is the one and only COM thread */
    static Thread comThread = null;
    static final ExecutorService comExecutor = Executors.newFixedThreadPool(
            1,
            new ThreadFactory() {
                @Override
                public Thread newThread(final Runnable r) {
                    comThread = createComThread(r);
                    return comThread;
                }
            });
    /**
     * Creates a thread that insures COM is initialized prior to calling the runnable.
     * @param r
     * @return a thread wrapping the given Runnable
     */
    static public Thread createComThread(final Runnable r) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                HRESULT hresult = ComObject.initializeCOM();
                r.run();
                if (hresult.intValue() >= 0) {
                    Ole32.INSTANCE.CoUninitialize();
                }
            }
        });
        t.setName("COM-" + threadNum++);
        return t;
    }

    static final int ptrSize = Pointer.SIZE;

    private static ThreadLocal<Integer> lastHRESULT = new ThreadLocal<Integer>();
    public static final Guid.GUID IID_IUnknown = Ole32Util.getGUIDFromString("{00000000-0000-0000-C000-000000000046}");

    /*
     * The only real data that this object holds... the interface pointer
     */
    private Pointer _InterfacePtr = null;

    private ComObject(Pointer interfacePointer) {
        _InterfacePtr = interfacePointer;
    }

    public static<T extends IUnknown> T createInstance(final Class<T> primaryInterface, final String clsid) {

        if (singleThreaded && Thread.currentThread() != comThread) {
            Future<T> retVal = comExecutor.submit(new Callable<T>() {
                public T call() throws Exception {
                    return createInstance(primaryInterface, clsid);
                }
            });
            try {
                return retVal.get();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            return null;
        }

        assert isComInitialized() : "COM not initialized when calling createInstance on "+Thread.currentThread();

        Guid.GUID refclsid = Ole32Util.getGUIDFromString(clsid);
        Guid.GUID refiid = IID_IUnknown;
        try {
            String iid = (String) primaryInterface.getAnnotation(IID.class).value();
            refiid = Ole32Util.getGUIDFromString(iid);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        PointerByReference punkown = new PointerByReference();
        HRESULT hresult = OLE32.CoCreateInstance(refclsid, Pointer.NULL, ObjBase.CLSCTX_ALL, refiid, punkown);
        lastHRESULT.set(hresult.intValue());
        if (hresult.intValue() < 0)
            throw new ComException("CoCreateInstance returned 0x"+Integer.toHexString(hresult.intValue()),hresult.intValue());
        Pointer interfacePointer = punkown.getValue();
        return wrapNativeInterface(interfacePointer, primaryInterface);
    }

    public static int getLastHRESULT() {
        return lastHRESULT.get();
    }

    public static <T extends IUnknown> T copy(T theInterface) {
        assert isComInitialized() : "COM not initialized when calling ComObject.copy on "+Thread.currentThread();
        ComObject comObj = (ComObject) Proxy.getInvocationHandler((Proxy)theInterface);
        theInterface.addRef();
        Class [] clazz = theInterface.getClass().getInterfaces();
        return (T) createProxy(new ComObject(comObj._InterfacePtr), clazz[0]);
    }

    public static <T extends IUnknown> T wrapNativeInterface(Pointer interfacePointer, Class<T> intrface) {
        assert isComInitialized() : "COM not initialized when calling wrapNativeInterface on "+Thread.currentThread();
        return createProxy(new ComObject(interfacePointer), intrface);
    }

    private static<T> T createProxy(ComObject object, Class<T> intrface) {
        T p = (T) Proxy.newProxyInstance(ComObject.class.getClassLoader(), new Class<?>[] {intrface}, object);
        return p;
    }

    /*
     * QueryInterface(REFIID, void **ppvObject)
     * AddRef(void)
     * Release(void)
     */
    private Pointer queryInterface(Class<?> comInterface) {
        try {
            String iid = (String) comInterface.getAnnotation(IID.class).value();
            Pointer vptr = _InterfacePtr.getPointer(0);
            Function func = Function.getFunction(vptr.getPointer(0));
            PointerByReference ppvObject = new PointerByReference();
            Guid.GUID refiid = Ole32Util.getGUIDFromString(iid);
            int hresult = func.invokeInt(new Object[]{_InterfacePtr, refiid, ppvObject});
            lastHRESULT.set(hresult);
            if (hresult >= 0) {
                return ppvObject.getValue();
            }
            throw new ComException("queryInterface failed. HRESULT = 0x"+Integer.toHexString(hresult),hresult);
        } catch (IllegalArgumentException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException("queryInterface failed",ex);
        }
    }


    //
    // When preparing args, if a parameter is a COM interface replace it with
    // its _InterfacePtr pointer
    //
    //
    Object [] prepareArgs(Method method, Object [] args) {
        int asize = 1 + (args != null ? args.length : 0);
        Object[] aarg = new Object[asize];
        for (int i = 1; i < aarg.length; i++) {
            Object givenArg = args[i - 1];
            if (givenArg instanceof IUnknown) {
                aarg[i] = ((ComObject)Proxy.getInvocationHandler(givenArg))._InterfacePtr;
            } else {
                aarg[i] = givenArg;
            }
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    /**
     * Add the "this" pointer to the argument list and reserve a spot for the
     * "return" value.
     * @param method
     * @param args
     * @return the new arguments
     */
    Object[] prepareArgsPlusRetVal(Method method, Object[] args) {
        Object[] aarg;
        if (args != null) {
            // add two, one for the 'this' pointer, the other for the retVal
            aarg = new Object[2 + args.length];
            for (int i = 0; i < args.length; i++) {
                Object givenArg = args[i];
                if (givenArg instanceof IUnknown) {
                    aarg[i+1] = ((ComObject) Proxy.getInvocationHandler(givenArg))._InterfacePtr;
                } else {
                    aarg[i+1] = givenArg;
                }
            }
        } else {
            // Even though the method was declared with no arguments, we need
            // two.  One for the 'this' pointer, the other for the retVal.
            aarg = new Object[2];
        }
        // 'this' pointer is taked on to the "front", retVal will be last
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    /**
     * Augment the parameter list for a COM API call that returns an integer
     * by adding the "this" pointer and the return value placeholder as a
     * byReference parameter.
     * @param method
     * @param args
     * @param retVal
     * @return
     */
    Object [] prepareArgs(Method method, Object [] args, IntByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue(((Number) args[rv.index()]).intValue());
            aarg[rv.index()] = retVal;
        } else {
            // default is to add the return value at the end of the parameter
            // list if it is not explicite
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    Object[] prepareArgs(Method method, Object[] args, LongByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue(((Number) args[rv.index()]).longValue());
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    Object[] prepareArgs(Method method, Object[] args, DoubleByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue(((Number) args[rv.index()]).doubleValue());
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    Object[] prepareArgs(Method method, Object[] args, ByteByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue(((Number) args[rv.index()]).byteValue());
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    Object[] prepareArgs(Method method, Object[] args, HANDLEByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue(((HANDLE) args[rv.index()]));
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr;
        return aarg;
    }

    Object[] prepareArgs(Method method, Object[] args, PointerByReference retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            retVal.setValue((Pointer) args[rv.index()]);
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr; // pass the "this" pointer
        return aarg;
    }
    
    Object[] prepareArgsObjOut(Method method, Object[] args, Object retVal) {
        Object[] aarg;
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if (rv != null && rv.inout()) {
            aarg = prepareArgs(method, args);
            // replace the return value (in/out) reference
            //retVal.setValue((Pointer) args[rv.index()]);
            aarg[rv.index()] = retVal;
        } else {
            aarg = prepareArgsPlusRetVal(method, args);
            aarg[aarg.length-1] = retVal;
        }
        aarg[0] = _InterfacePtr; // pass the "this" pointer
        return aarg;
    }

    void invokeVoidCom(Method method, Object... args) {
        int offset = method.getAnnotation(VTID.class).value();
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        Object[] aarg = prepareArgs(method, args);
        int hresult = func.invokeInt(aarg);
        lastHRESULT.set(hresult);
        if (hresult < 0)
            throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
    }

    /** Invokes a standard COM method that returns an integer
     * The actual COM method is assumed to always return an HRESULT. The "real"
     * return value is added to the end of the parameters list via pass by
     * reference so it can be filled in.
     * @param method
     * @param args
     * @return The integer return value (NOT the HRESULT)
     */
    int invokeIntCom(Method method, Object... args){
        int offset = method.getAnnotation(VTID.class).value();
        NoHResult nohresult = method.getAnnotation(NoHResult.class);
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        if(nohresult == null) {
            IntByReference retVal = new com.sun.jna.ptr.IntByReference();
            Object[] aarg = prepareArgs(method, args, retVal);
            int hresult = func.invokeInt(aarg);
            lastHRESULT.set(hresult);
            if (hresult < 0)
                throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult),hresult);
            return retVal.getValue();
        } else { //Method simply returns the value
            Object[] aarg = prepareArgs(method, args);
            return func.invokeInt(aarg);
        }
    }

    long invokeLongCom(Method method, Object... args){
        int offset = method.getAnnotation(VTID.class).value();
        NoHResult nohresult = method.getAnnotation(NoHResult.class);
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        if(nohresult == null) {
            LongByReference retVal = new com.sun.jna.ptr.LongByReference();
            Object[] aarg = prepareArgs(method, args, retVal);
            int hresult = func.invokeInt(aarg);
            lastHRESULT.set(hresult);
            if (hresult < 0)
                throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
            return retVal.getValue();
        } else {
            Object[] aarg = prepareArgs(method, args);
            return func.invokeLong(aarg);
        }
    }

    double invokeDoubleCom(Method method, Object... args){
        int offset = method.getAnnotation(VTID.class).value();
        NoHResult nohresult = method.getAnnotation(NoHResult.class);
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        if(nohresult == null) {
            DoubleByReference retVal = new com.sun.jna.ptr.DoubleByReference();
            Object[] aarg = prepareArgs(method, args, retVal);
            int hresult = func.invokeInt(aarg);
            lastHRESULT.set(hresult);
            if (hresult < 0)
                throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
            return retVal.getValue();
        } else {
            Object[] aarg = prepareArgs(method, args);
            return func.invokeDouble(aarg);
        }
    }

    byte invokeByteCom(Method method, Object... args){
        int offset = method.getAnnotation(VTID.class).value();
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        ByteByReference retVal = new com.sun.jna.ptr.ByteByReference();
        Object[] aarg = prepareArgs(method, args, retVal);
        int hresult = func.invokeInt(aarg);
        lastHRESULT.set(hresult);
        if (hresult < 0)
            throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
        return retVal.getValue();
    }

    HANDLE invokeHandleCom(Method method, Object... args){
        int offset = method.getAnnotation(VTID.class).value();
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        HANDLEByReference retVal = new HANDLEByReference();
        Object[] aarg = prepareArgs(method, args, retVal);
        int hresult = func.invokeInt(aarg);
        lastHRESULT.set(hresult);
        if (hresult < 0)
            throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
        return retVal.getValue();
    }

    Object invokeObjectCom(Method method, Object... args) {
        int offset = method.getAnnotation(VTID.class).value();
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(offset * ptrSize));
        if (method.getReturnType().isInterface()) {
            // Hack the return parameter on to the args
            PointerByReference p = new PointerByReference();   
            Object[] aarg = prepareArgs(method, args, p);
            int hresult = (Integer) func.invoke(Integer.class, aarg);
            lastHRESULT.set(hresult);
            if (hresult < 0)
                throw new ComException("Invocation of \""+method.getName()+"\" failed, hresult=0x"+Integer.toHexString(hresult), hresult);
            return createProxy(new ComObject(p.getValue()), method.getReturnType());
        } else {
            boolean returnsBSTR = false;
            Object retVal = null;
            ReturnValue rv = method.getAnnotation(ReturnValue.class);
            if (rv != null && rv.inout()) {
                // one of the args is the return value
                retVal = (Structure) args[rv.index()];
            } else {
                // retval wasn't an explicit parameter - so we will make one
                if (method.getReturnType() == String.class) {
                    // String in COM should mean BSTR
                    returnsBSTR = true;
                    retVal = new PointerByReference();
                } else try {
                    // just a normal structure
                    retVal = method.getReturnType().newInstance();
                    assert retVal instanceof Structure;
                } catch (InstantiationException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    throw new RuntimeException("Invocation of \"" + method.getName() + "\" failed.",ex);
                } catch (IllegalAccessException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    throw new RuntimeException("Invocation of \"" + method.getName() + "\" failed.", ex);
                }
            }
            Object[] aarg = prepareArgsObjOut(method, args, retVal);
            int hresult = (Integer) func.invoke(Integer.class, aarg);
            lastHRESULT.set(hresult);
            if (hresult < 0)
                throw new ComException("Invocation of \"" + method.getName() + "\" failed, hresult=0x" + Integer.toHexString(hresult), hresult);
            if (returnsBSTR) {
                WTypes.BSTR bstr = new  WTypes.BSTR();
                
                bstr.setPointer( ((PointerByReference) retVal).getValue() );

                retVal = bstr.toString();
                
                OleAuto.INSTANCE.SysFreeString(bstr);
            }
            return retVal;
        }
    }

    int addRef() {
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(1 * ptrSize));
        return func.invokeInt(new Object[] {_InterfacePtr});
    }
    
    int release() {
        Pointer vptr = _InterfacePtr.getPointer(0);
        Function func = Function.getFunction(vptr.getPointer(2 * ptrSize));
        return func.invokeInt(new Object[]{_InterfacePtr});
    }

    /**
     * The "Java" way to free resources is to call a dispose() method, after
     * which, the object should not be used.  Since Java passes only object
     * references by value, the need for explicite addRef and release calls
     * is rare.
     */
    public synchronized void dispose() {
        if (_InterfacePtr != null) {
            release();
            // because we share the native pointer amoung interface copies
            // we can't assume that the reference count should be zero here
            _InterfacePtr = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        logger.log(Level.FINEST, "Finalizing: {0}", getClass().getName());
        dispose();
        super.finalize();
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (singleThreaded && Thread.currentThread() != comThread) {
            Future<Object> retVal = comExecutor.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        return invoke(proxy, method, args);
                    } catch (Throwable ex) {
                        throw new Exception("COM invoke failed",ex);
                    }
                }
            });
            try {
                return retVal.get();
            } catch (InterruptedException ex) {
                Logger.getLogger(ComObject.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(ComObject.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        assert isComInitialized() : "COM not initialized when calling "+method.getName()+" on "+Thread.currentThread();

        String mname = method.getName();
        if (mname.equals("queryInterface")) {
            // if the native interface pointer is the same, return a new proxy to
            // this same ComObject that implements the interface required interface
            // otherwise make a new ComObject to wrap the returned interface pointer
            if (args[0] instanceof Class<?>) {
                Class<?> c = (Class<?>) args[0];
                if (c.isInterface()) {
                    Pointer interfacePointer = queryInterface((Class<?>)args[0]);
                    return createProxy(new ComObject(interfacePointer), c);
                } else {
                    throw new RuntimeException("Argument to queryInterface must be a Java interface class annotated with an interface ID.");
                }
            }
        } else if (mname.equals("dispose")) {
            //System.out.println("dispose() ->"+proxy);
            dispose();
            return null;
        } else if (mname.equals("toString")) {
            StringBuilder sb = new StringBuilder();
            sb.append(_InterfacePtr);
            sb.append("):");

            boolean notFirst = false;
            for (Class<?> cc : proxy.getClass().getInterfaces()) {
                if (notFirst)
                    sb.append(", ");
                else
                    notFirst = true;
                sb.append(cc.getName());
            }

            return sb.toString();
        } else if (mname.equals("addRef")) {
            return addRef();
        } else if (mname.equals("release")) {
            return release();
        }
       
        //Examine the args and wrap String objects as WString
        if(args != null) {
            for(int i=0;i<args.length;i++) {
                if(args[i] instanceof String) {
                    String s = (String)(args[i]);
                    WString wstr = new WString(s);
                    args[i] = wstr;
                }
            }
        }

        if (method.getReturnType() == Void.TYPE) {
            invokeVoidCom(method, args);
            return null;
        } else if (method.getReturnType() == Integer.TYPE) {
            return invokeIntCom(method, args);
        } else if (method.getReturnType() == Long.TYPE) {
            return invokeLongCom(method, args);
        } else if (method.getReturnType() == Double.TYPE) {
            return invokeDoubleCom(method, args);
        } else if (method.getReturnType() == Byte.TYPE) {
            return invokeByteCom(method, args);
        } else if (method.getReturnType() == Boolean.TYPE) {
            return invokeByteCom(method, args) != 0;
        } else if (method.getReturnType() == HANDLE.class) {
            return invokeHandleCom(method, args);
        } else {
            return invokeObjectCom(method, args);
        }
    }
}
