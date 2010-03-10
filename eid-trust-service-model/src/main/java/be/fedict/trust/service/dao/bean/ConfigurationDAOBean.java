/*
 * eID Trust Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.trust.service.dao.bean;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.trust.NetworkConfig;
import be.fedict.trust.service.TrustServiceConstants;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.entity.ClockDriftConfigEntity;
import be.fedict.trust.service.entity.NetworkConfigEntity;
import be.fedict.trust.service.entity.TimeProtocol;

/**
 * Configuration DAO Bean implementation.
 * 
 * @author wvdhaute
 * 
 */
@Stateless
public class ConfigurationDAOBean implements ConfigurationDAO {

	private static final Log LOG = LogFactory
			.getLog(ConfigurationDAOBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfigEntity getNetworkConfigEntity() {

		LOG.debug("get network config entity");
		NetworkConfigEntity networkConfig = this.entityManager
				.find(NetworkConfigEntity.class,
						TrustServiceConstants.NETWORK_CONFIG);
		if (null == networkConfig) {
			networkConfig = new NetworkConfigEntity(
					TrustServiceConstants.NETWORK_CONFIG, null, 0);
			this.entityManager.persist(networkConfig);
		}
		return networkConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfig getNetworkConfig() {
		LOG.debug("get network config entity");
		NetworkConfigEntity networkConfig = getNetworkConfigEntity();
		if (networkConfig.isEnabled()) {
			return new NetworkConfig(networkConfig.getProxyHost(),
					networkConfig.getProxyPort());
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNetworkConfigEnabled(boolean enabled) {

		LOG.debug("set network config enabled: " + enabled);
		NetworkConfigEntity networkConfig = getNetworkConfigEntity();
		networkConfig.setEnabled(enabled);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetworkConfigEntity setNetworkConfig(String proxyHost, int proxyPort) {

		LOG.debug("set network config: proxyHost=" + proxyHost + " proxyPort="
				+ proxyPort);
		NetworkConfigEntity networkConfigEntity = getNetworkConfigEntity();
		networkConfigEntity.setProxyHost(proxyHost);
		networkConfigEntity.setProxyPort(proxyPort);
		return networkConfigEntity;
	}

	/**
	 * {@inheritDoc}
	 */
	public ClockDriftConfigEntity getClockDriftConfig() {

		LOG.debug("get clock drift configuration");
		ClockDriftConfigEntity clockDriftConfig = this.entityManager.find(
				ClockDriftConfigEntity.class,
				TrustServiceConstants.CLOCK_DRIFT_CONFIG);
		if (null == clockDriftConfig) {
			clockDriftConfig = new ClockDriftConfigEntity(
					TrustServiceConstants.CLOCK_DRIFT_CONFIG, TimeProtocol.NTP,
					TrustServiceConstants.CLOCK_DRIFT_NTP_SERVER,
					TrustServiceConstants.CLOCK_DRIFT_TIMEOUT,
					TrustServiceConstants.CLOCK_DRIFT_MAX_CLOCK_OFFSET,
					TrustServiceConstants.DEFAULT_CRON);
			this.entityManager.persist(clockDriftConfig);
		}
		return clockDriftConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	public ClockDriftConfigEntity setClockDriftConfig(
			TimeProtocol timeProtocol, String server, int timeout,
			int maxClockOffset, String cron) {

		LOG.debug("set clock drift detection config: protocol="
				+ timeProtocol.name() + " server=" + server + " timeout="
				+ timeout + " maxClockOffset=" + maxClockOffset);
		ClockDriftConfigEntity clockDriftConfig = getClockDriftConfig();
		clockDriftConfig.setTimeProtocol(timeProtocol);
		clockDriftConfig.setServer(server);
		clockDriftConfig.setTimeout(timeout);
		clockDriftConfig.setMaxClockOffset(maxClockOffset);
		clockDriftConfig.setCron(cron);
		return clockDriftConfig;
	}
}