/*
 * Copyright 2010 Digital Rapids Corporation.
 */

package com.sun.jna.platform.win32.jnacom.interfaces;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.jnacom.IID;
import com.sun.jna.platform.win32.jnacom.IUnknown;
import com.sun.jna.platform.win32.jnacom.VTID;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;


/* THIS INTERFACE IS DEFINED HERE SIMPLY FOR UNIT TESTING OF THE FRAMEWORK
 * IT HAS NOT BEEN DEBUGGED AND CALLING THE METHODS AS THEY ARE DEFINED HERE
 * MAY NOT WORK (IT IS NOT REQUIRED FOR THE UNIT TESTS THAT USE THIS.)
 */

/**
 * Exposed by all Shell namespace folder objects, its methods are used to manage folders.
 *
 * @author scott.palmer
 */
// Interface ID annotation is really only required for ComObject.createInstance.
// This interface is obtained via Shell32.SHGetDesktopFolder.
@IID("{000214E6-0000-0000-C000-000000000046}")
public interface IShellFolder_ extends IUnknown {

    /**
     * Translates the display name of a file object or a folder into an item
     * identifier list.
     *
     * @param hwnd A window handle. The client should provide a window handle if
     * it displays a dialog or message box. Otherwise set hwnd to NULL.
     *
     * @param pbc Optional. A pointer to a bind context (IBindCtx) used to pass parameters
     * as inputs and outputs to the parsing function. These passed parameters
     * are often specific to the data source and are documented by the data
     * source owners. For example, the file system data source accepts the name
     * being parsed (as a WIN32_FIND_DATA structure), using the
     * STR_FILE_SYS_BIND_DATA bind context parameter.
     * STR_PARSE_PREFER_FOLDER_BROWSING can be passed to indicate that URLs are
     * parsed using the file system data source when possible. Construct a bind
     * context object using CreateBindCtx and populate the values using
     * IBindCtx::RegisterObjectParam. See Bind Context String Keys for a
     * complete list of these.
     * 
     * If no data is being passed to or received from the parsing function, this
     * value can be NULL.
     *
     * @param pszDisplayName A null-terminated Unicode string with the display
     * name. Because each Shell folder defines its own parsing syntax, the form
     * this string can take may vary. The desktop folder, for instance, accepts
     * paths such as "C:\My Docs\My File.txt". It also will accept references to
     * items in the namespace that have a GUID associated with them using the
     * "::{GUID}" syntax. For example, to retrieve a fully qualified identifier
     * list for the control panel from the desktop folder, you can use the following:
     * <code>::{CLSID for Control Panel}\::{CLSID for printers folder}</code>
     *
     * @param pchEaten A pointer to a ULONG value that receives the number of
     * characters of the display name that was parsed. If your application does
     * not need this information, set pchEaten to NULL, and no value will be
     * returned.
     *
     * @param ppidl When this method returns, contains a pointer to the PIDL for
     * the object. The returned item identifier list specifies the item relative
     * to the parsing folder. If the object associated with pszDisplayName is
     * within the parsing folder, the returned item identifier list will contain
     * only one SHITEMID structure. If the object is in a subfolder of the
     * parsing folder, the returned item identifier list will contain multiple
     * SHITEMID structures. If an error occurs, NULL is returned in this
     * address.
     *
     * @param pdwAttributes The value used to query for file attributes. If not
     * used, it should be set to NULL. To query for one or more attributes,
     * initialize this parameter with the SFGAO flags that represent the
     * attributes of interest. On return, those attributes that are true and
     * were requested will be set.
     *
     * @return
     */
    @VTID(3)
    HRESULT parseDisplayName(
            HWND hwnd,
            Pointer pbc, // IBindCtx pbc
            String pszDisplayName,
            /* [annotation][unique][out][in] */
            NativeLongByReference pchEaten,
            /* [out] */ PointerByReference /*PIDLIST_RELATIVE **/ ppidl,
            /* [unique][out][in] */ NativeLongByReference pdwAttributes);

    /** Enables a client to determine the contents of a folder by creating an
     * item identifier enumeration object and returning its IEnumIDList
     * interface. The methods supported by that interface can then be used to
     * enumerate the folder's contents.
     *
     * @param hwndOwner If user input is required to perform the enumeration,
     * this window handle should be used by the enumeration object as the parent
     * window to take user input. An example would be a dialog box to ask for a
     * password or prompt the user to insert a CD or floppy disk. If hwndOwner
     * is set to NULL, the enumerator should not post any messages, and if user
     * input is required, it should silently fail.
     *
     * @param grfFlags Flags indicating which items to include in the
     * enumeration. For a list of possible values, see the SHCONTF enumerated
     * type.
     *
     * @param ppenumIDList - Use ComObject.wrapNativeInterface on the returned
     * pointer to get access to the IEnumIDList interface.
     *
     * @return Returns S_OK if successful, or an error value otherwise. Some
     * implementations may also return S_FALSE, indicating that there are no
     * children matching the grfFlags that were passed in. If S_FALSE is
     * returned, ppenumIDList is set to NULL.
     */
    @VTID(4)
    HRESULT enumObjects(
            HWND hwndOwner,
            /* [in]  SHCONTF*/ int grfFlags,
            /* [out] IEnumIDList */ PointerByReference ppenumIDList);

    @VTID(5)
    HRESULT  BindToObject(
            /* [in] __RPC__in PCUIDLIST_RELATIVE */ Pointer pidl,
            /* [unique][in] IBindCtx*/ IUnknown pbc,
            /* [in] __RPC__in REFIID*/ Guid.GUID riid,
            /* [iid_is][out] __RPC__deref_out_opt*/ PointerByReference ppv);

    HRESULT  BindToStorage(
            /* [in] __RPC__in PCUIDLIST_RELATIVE */ Pointer pidl,
            /* [unique][in]  __RPC__in_opt IBindCtx * */ IUnknown pbc,
            /* [in] __RPC__in REFIID*/ Guid.GUID riid,
            /* [iid_is][out]  __RPC__deref_out_opt*/ PointerByReference ppv);

    HRESULT  CompareIDs(
            /* [in]  LPARAM*/ NativeLong lParam,
            /* [in] __RPC__in PCUIDLIST_RELATIVE*/ Pointer pidl1,
            /* [in] __RPC__in PCUIDLIST_RELATIVE*/ Pointer pidl2);

    HRESULT  CreateViewObject(
            /* [unique][in] __RPC__in_opt*/ HWND hwndOwner,
            /* [in] __RPC__in REFIID*/ Guid.GUID riid,
            /* [iid_is][out] __RPC__deref_out_opt*/ PointerByReference ppv);

    HRESULT  GetAttributesOf(
            /* [in] UINT*/ int cidl,
            /* [unique][size_is][in] __RPC__in_ecount_full_opt(cidl) PCUITEMID_CHILD_ARRAY*/ Pointer apidl,
            /* [out][in] __RPC__inout SFGAOF * */ IntByReference rgfInOut);

    HRESULT  GetUIObjectOf(
            /* [unique][in] __RPC__in_opt*/ HWND hwndOwner,
            /* [in] UINT*/ int cidl,
            /* [unique][size_is][in] __RPC__in_ecount_full_opt(cidl) PCUITEMID_CHILD_ARRAY*/ Pointer apidl,
            /* [in] __RPC__in REFIID*/ Guid.GUID riid,
            /* [annotation][unique][out][in] */
            /*__reserved*/  IntByReference rgfReserved,
            /* [iid_is][out] __RPC__deref_out_opt*/ PointerByReference ppv);

    HRESULT  GetDisplayNameOf(
            /* [unique][in] __RPC__in_opt PCUITEMID_CHILD*/ Pointer pidl,
            /* [in]  SHGDNF*/ int uFlags,
            /* [out] __RPC__out*/ STRRET.ByReference pName);

    /* [local] */
    HRESULT  SetNameOf(
            /* [annotation][unique][in] */
            /*__in_opt*/ HWND hwnd,
            /* [annotation][in] 
            __in  PCUITEMID_CHILD */ Pointer pidl,
            /* [annotation][string][in] 
            __in*/  String pszName,
            /* [annotation][in] 
            __in  SHGDNF*/ int uFlags,
            /* [annotation][out] 
            __deref_opt_out PITEMID_CHILD * */ PointerByReference ppidlOut);
}

class STRRET extends Structure {
    public static class ByValue extends STRRET implements Structure.ByValue { }
    public static class ByReference extends STRRET implements Structure.ByReference { }
    int uType;
    // need a union
    STRRET_U u;
	@Override
	protected List getFieldOrder() {
		return Arrays.asList(new String[] { "uType", "u" });
	}
}

class STRRET_U {
    String pOleStr;
    int uOffset;
    char [] cStr = new char[256];
}