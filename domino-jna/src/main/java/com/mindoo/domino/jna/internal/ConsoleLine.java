package com.mindoo.domino.jna.internal;

import com.mindoo.domino.jna.utils.IConsoleLine;

/**
 * Container for a single Domino console line with the text content and
 * additional properties like the pid, tod, process name, type and priority.
 */
public class ConsoleLine implements IConsoleLine {
    public static final String sSeqNumber = "sq=\"";
    public static final String sPassword = "pw=\"";
    public static final String sPrompt = "pr=\"";
    public static final String sTime = "ti=\"";
    public static final String sExecName = "ex=\"";
    public static final String sProcId = "pi=\"";
    public static final String sThreadId = "tr=\"";
    public static final String sStatus = "st=\"";
    public static final String sType = "ty=\"";
    public static final String sSev = "sv=\"";
    public static final String sColor = "co=\"";
    public static final String sAddin = "ad=\"";
    public static final String sArgsEnd = ">";
    public static final String sConsoleTextStart = "<ct ";
    public static final String sConsoleTextEnd = "</ct>";
    public static final String sTokenEnd = "\"";
    public static final String sTrue = "t";
    public static final String sServerStatus = "ss=\"";

    private int seqNum;
    private String timeStamp;
    private String execName;
    private int pid;
    private long tid;
    private long vTid;
    private int status;
    private int type;
    private int severity;
    private int color;
    private String addName;
    private String data;
    private boolean isPasswdReq;
    private boolean isPrompt;

    private ConsoleLine() {
        this.init();
    }

    private void init() {
        this.setSeqNum(0);
        this.setTimeStamp("");
        this.setExecName("");
        this.setPid(0);
        this.setTid(0L);
        this.setVTid(0L);
        this.setStatus(0);
        this.setType(0);
        this.setSeverity(0);
        this.setColor(-1);
        this.setAddName("");
        this.setData("");
        this.setPasswordString(false);
        this.setPromptString(false);
    }

    private static String getStringToken(String string, String string2, String string3) {
        String string4 = null;
        int n = 0;
        int n2 = 0;
        n = string.indexOf(string2);
        n2 = string.indexOf(string3, n + string2.length());
        if (n > 0 && n2 > 0) {
            string4 = string.substring(n + string2.length(), n2);
        }
        return string4;
    }

    private static int getIntToken(String string, int n, String string2, String string3) {
        String string4 = getStringToken(string, string2, string3);
        if (string4 != null) {
            return Integer.parseInt(string4, n);
        }
        return 0;
    }

    private static boolean getBoolToken(String string, String string2, String string3) {
        String string4 = getStringToken(string, string2, string3);
        if (string4 != null) {
            return string4.equalsIgnoreCase(sTrue);
        }
        return false;
    }

    public static ConsoleLine parseConsoleLine(String encodedConsoleLine, int srvType) {
    	ConsoleLine line = new ConsoleLine();
    	
        if (!encodedConsoleLine.startsWith(sConsoleTextStart) || !encodedConsoleLine.endsWith(sConsoleTextEnd)) {
        	line.setData(encodedConsoleLine.equals("\n") ? "" : encodedConsoleLine);
        	line.setColor(-1);
            return line;
        }
        
        int n2 = srvType == 0 ? 16 : 10;
        int n3 = encodedConsoleLine.indexOf(sArgsEnd);
        String string2 = encodedConsoleLine.substring(0, n3 + 1);
        try {
        	line.setSeqNum(getIntToken(string2, 16, sSeqNumber, sTokenEnd));
        	line.setPasswordString(getBoolToken(string2, sPassword, sTokenEnd));
        	line.setPromptString(getBoolToken(string2, sPrompt, sTokenEnd));
        	line.setTimeStamp(getStringToken(string2, sTime, sTokenEnd));
        	line.setExecName(getStringToken(string2, sExecName, sTokenEnd));
        	line.setPid(getIntToken(string2, n2, sProcId, sTokenEnd));
            int n4 = string2.indexOf(sThreadId);
            int n5 = string2.indexOf("-", n4);
            line.setTid(Long.parseLong(string2.substring(n4 + sThreadId.length(), n5), n2));
            n4 = n5;
            n5 = string2.indexOf(sTokenEnd, n4);
            if (n4 > 0 && n5 > 0) {
                String string3 = string2.substring(n4 + 1, n5);
                int n6 = string3.indexOf(":", 0);
                if (n6 > 0) {
                    string3 = string3.substring(0, n6);
                }
                n2 = srvType == 1 ? 16 : n2;
                line.setVTid(Long.parseLong(string3, n2));
            }
            line.setStatus(getIntToken(string2, 16, sStatus, sTokenEnd));
            line.setType(getIntToken(string2, 10, sType, sTokenEnd));
            line.setSeverity(getIntToken(string2, 10, sSev, sTokenEnd));
            line.setColor(getIntToken(string2, 10, sColor, sTokenEnd));
            line.setAddName(getStringToken(string2, sAddin, sTokenEnd));
            line.setData(getStringToken(encodedConsoleLine, sArgsEnd, sConsoleTextEnd));
        }
        catch (Exception exception) {
            System.out.println("msg=" + exception.getMessage());
            System.out.println("Exception:parseMS::" + encodedConsoleLine + ":,srvType=" + srvType);
            
            line.setData(encodedConsoleLine);
            line.setColor(-1);
            return line;
        }
        
        return line;
    }

    @Override
	public int getMsgSeqNum() {
		return seqNum;
	}

	private void setSeqNum(int msgSeqNum) {
		this.seqNum = msgSeqNum;
	}

    @Override
	public String getTimeStamp() {
		return timeStamp;
	}

	private void setTimeStamp(String msgTimeStamp) {
		this.timeStamp = msgTimeStamp;
	}

    @Override
	public String getExecName() {
		return execName;
	}

	private void setExecName(String msgExecName) {
		this.execName = msgExecName;
	}

    @Override
	public int getPid() {
		return pid;
	}

	private void setPid(int msgPid) {
		this.pid = msgPid;
	}

    @Override
	public long getTid() {
		return tid;
	}

	private void setTid(long msgTid) {
		this.tid = msgTid;
	}

    @Override
	public long getVTid() {
		return vTid;
	}

	private void setVTid(long msgVTid) {
		this.vTid = msgVTid;
	}

    @Override
	public int getStatus() {
		return status;
	}

	private void setStatus(int msgStatus) {
		this.status = msgStatus;
	}

    @Override
	public int getType() {
		return type;
	}

	private void setType(int msgType) {
		this.type = msgType;
	}

    @Override
	public int getSeverity() {
		return severity;
	}

	private void setSeverity(int msgSeverity) {
		this.severity = msgSeverity;
	}

    @Override
	public int getColor() {
		return color;
	}

	private void setColor(int msgColor) {
		this.color = msgColor;
	}

    @Override
	public String getAddName() {
		return addName;
	}

	private void setAddName(String msgAddName) {
		this.addName = msgAddName;
	}

    @Override
	public String getData() {
		return data;
	}

	private void setData(String msgData) {
		this.data = msgData;
	}

    @Override
	public boolean isPasswordString() {
		return isPasswdReq;
	}

	private void setPasswordString(boolean b) {
		this.isPasswdReq = b;
	}

    @Override
	public boolean isPromptString() {
		return isPrompt;
	}

	private void setPromptString(boolean b) {
		this.isPrompt = b;
	}

	@Override
	public String toString() {
		return "ConsoleLine [seqNum=" + seqNum + ", timeStamp=" + timeStamp + ", execName=" + execName + ", pid=" + pid
				+ ", tid=" + tid + ", vTid=" + vTid + ", status=" + status + ", type=" + type + ", severity=" + severity
				+ ", color=" + color + ", addName=" + addName + ", isPwd=" + isPasswdReq + ", isPrompt="
				+ isPrompt + ", data=" + data + "]";
	}
	
	
}

