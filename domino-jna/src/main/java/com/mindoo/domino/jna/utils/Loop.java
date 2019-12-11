/* ========================================================================== *
 * Copyright (C) 2019, 2020 HCL ( http://www.hcl.com/ )                       *
 *                            All rights reserved.                            *
 * ========================================================================== *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 * ========================================================================== */

 package com.mindoo.domino.jna.utils;

/**
 * Helper method to control a forEach loop, e.g. to stop the loop or get the
 * current offset
 */
public class Loop {
	private boolean m_isStopped = false;
	private boolean m_isLast = false;
	private int m_index;
	
	/**
	 * Call this method to stop a forEach loop
	 */
	public void stop() {
		m_isStopped = true;
	}

	/**
	 * Returns true if forEach loop should be stopped
	 * 
	 * @return true if stopped
	 */
	public boolean isStopped() {
		return m_isStopped;
	}
	
	/**
	 * Returns true if the current element is the last of the collection
	 * 
	 * @return true if last
	 */
	public boolean isLast() {
		return m_isLast;
	}

	/**
	 * Returns true if the current element is the first of the collection
	 * 
	 * @return true if first
	 */
	public boolean isFirst() {
		return m_index == 0;
	}

	/**
	 * Returns the current loop index. If skip(int) is used in the forEach loop,
	 * the first loop index is the skip offset value
	 * 
	 * @return index
	 */
	public int getIndex() {
		return m_index;
	}
	
	/**
	 * Internal method to change isLast flag
	 */
	protected void setIsLast() {
		m_isLast = true;
	}
	
	/**
	 * Internal method to change current index
	 * 
	 * @param index new index
	 */
	protected void setIndex(int index) {
		m_index = index;
	}
}