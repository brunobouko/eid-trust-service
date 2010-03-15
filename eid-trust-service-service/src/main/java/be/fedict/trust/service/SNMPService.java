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

package be.fedict.trust.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

public class SNMPService implements SNMPServiceMBean {

	private static final Log LOG = LogFactory.getLog(SNMPService.class);

	private String address;

	private Thread snmpThread;
	private SNMPAgent snmpAgent;

	private Map<String, Long> snmpValues;

	/**
	 * {@inheritDoc}
	 */
	public void increment(String oid) {

		if (null == this.snmpAgent) {
			LOG.debug("Address=" + this.address);
			try {
				this.snmpAgent = new SNMPAgent(this.address, new File(
						"performance.snmp.agent.boot.counter.cfg"), new File(
						"performance.snmp.agent.cfg"));
			} catch (IOException e) {
				LOG.error("Failed to start SNMP agent: " + e.getMessage(), e);
				throw new RuntimeException(e);
			}
			this.snmpThread = new Thread(this.snmpAgent);
			this.snmpThread.start();

			this.snmpValues = new HashMap<String, Long>();
		}

		Long value = snmpValues.get(oid);
		if (null == value) {
			snmpValues.put(oid, 1L);
			try {
				addSNMPCounter(oid);
			} catch (DuplicateRegistrationException e) {
				LOG.error("Counter with oid=" + oid + " already registered.");
			}
		} else {
			snmpValues.put(oid, value + 1);
		}
		LOG.debug("oid=" + oid + " value=" + snmpValues.get(oid));
	}

	/**
	 * Register a new {@link Counter} for the specified oid in the
	 * {@link SNMPAgent}.
	 * 
	 * @param oid
	 * @throws DuplicateRegistrationException
	 */
	private void addSNMPCounter(final String oid)
			throws DuplicateRegistrationException {

		this.snmpAgent.registerCounter(new Counter(new OID(oid),
				MOAccessImpl.ACCESS_READ_ONLY) {

			@Override
			protected Variable getCounterValue() {

				return new org.snmp4j.smi.Counter64(SNMPService.this.snmpValues
						.get(oid));
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public String getAddress() {

		return this.address;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setAddress(String address) {

		this.address = address;
	}
}