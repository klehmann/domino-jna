package com.mindoo.domino.jna.xsp.http;

import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.IServiceFactory;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;

public class DominoJNAServiceFactory implements IServiceFactory {

	public HttpService[] getServices(final LCDEnvironment paramLCDEnvironment) {
		HttpService[] ret = new HttpService[1];
		ret[0] = new DominoJNAHttpService(paramLCDEnvironment);
		return ret;
	}

}
