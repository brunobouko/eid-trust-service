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

package be.fedict.trust.xkms2;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Logging JAX-WS SOAP handler.
 * 
 * @author fcorneli
 * 
 */
public class LoggingSoapHandler implements SOAPHandler<SOAPMessageContext> {

	private static final Log LOG = LogFactory.getLog(LoggingSoapHandler.class);

	public Set<QName> getHeaders() {
		return null;
	}

	public void close(MessageContext context) {
		LOG.debug("close");
	}

	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	public boolean handleMessage(SOAPMessageContext context) {
		LOG.debug("handle message");
		Boolean outboundProperty = (Boolean) context
				.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		LOG.debug("outbound message: " + outboundProperty);
		SOAPMessage soapMessage = context.getMessage();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			soapMessage.writeTo(output);
		} catch (Exception e) {
			LOG.error("SOAP error: " + e.getMessage());
		}
		LOG.debug("SOAP message: " + output.toString());
		return true;
	}
}
