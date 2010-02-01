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

package be.fedict.trust.service;

import java.security.cert.X509Certificate;

import javax.ejb.Local;

import be.fedict.eid.applet.service.spi.AuthenticationService;
import be.fedict.trust.service.entity.AdminEntity;

/**
 * Admin authorization service.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface AdminAuthorizationService extends AuthenticationService {

	public static final String JNDI_BINDING = TrustServiceConstants.JNDI_CONTEXT
			+ "/AdminAuthorizationServiceBean";

	/**
	 * Authenticate the specified authentication certificate chain. Does a basic
	 * public key verification and looks up if an {@link AdminEntity} matching
	 * the public key.
	 * 
	 * @param authnCert
	 * @return id The {@link AdminEntity}'s id.
	 */
	String authenticate(X509Certificate authnCert);
}