/*
 * eID Trust Service Project.
 * Copyright (C) 2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.trust.admin.portal;

import java.util.List;

import javax.ejb.Local;
import javax.faces.model.SelectItem;

@Local
public interface Configuration {

	/*
	 * Lifecycle.
	 */
	void destroyCallback();

	void initialize();

	/*
	 * Factory
	 */
	List<SelectItem> clockDriftProtocolFactory();

	/*
	 * Accessors
	 */
	String getProxyHost();

	void setProxyHost(String proxyHost);

	int getProxyPort();

	void setProxyPort(int proxyPort);

	boolean isEnabled();

	void setEnabled(boolean enabled);

	String getClockDriftProtocol();

	void setClockDriftProtocol(String clockDriftProtocol);

	String getClockDriftServer();

	void setClockDriftServer(String clockDriftServer);

	int getClockDriftTimeout();

	void setClockDriftTimeout(int clockDriftTimeout);

	int getClockDriftMaxClockOffset();

	void setClockDriftMaxClockOffset(int clockDriftMaxClockOffset);

	String getClockDriftCron();

	void setClockDriftCron(String clockDriftCron);

	/*
	 * Actions
	 */
	String saveNetworkConfig();

	String saveClockDriftConfig();

}
