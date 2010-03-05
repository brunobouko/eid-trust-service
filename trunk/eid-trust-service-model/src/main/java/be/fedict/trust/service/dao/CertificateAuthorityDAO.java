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

package be.fedict.trust.service.dao;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import be.fedict.trust.service.entity.CertificateAuthorityEntity;
import be.fedict.trust.service.entity.RevokedCertificateEntity;
import be.fedict.trust.service.entity.TrustPointEntity;

/**
 * Certificate Authority DAO.
 * 
 * @author wvdhaute
 * 
 */
@Local
public interface CertificateAuthorityDAO {

	/**
	 * Return {@link CertificateAuthorityEntity} from specified name. Returns
	 * <code>null</code> if not found.
	 * 
	 * @param name
	 */
	CertificateAuthorityEntity findCertificateAuthority(String name);

	/**
	 * Create a new {@link CertificateAuthorityEntity}.
	 * 
	 * @param rootCaCertificate
	 */
	CertificateAuthorityEntity addCertificateAuthority(
			X509Certificate certificate);

	/**
	 * Returns {@link CertificateAuthorityEntity} from the specified
	 * {@link X509Certificate}. Returns <code>null</code> if not found.
	 * 
	 * @param certificate
	 * @return
	 */
	CertificateAuthorityEntity findCertificateAuthority(
			X509Certificate certificate);

	/**
	 * Remove {@link CertificateAuthorityEntity}'s related to the specified
	 * {@link TrustPointEntity}.
	 * 
	 * @param trustPoint
	 */
	void removeCertificateAuthorities(TrustPointEntity trustPoint);

	/**
	 * Add a {@link RevokedCertificateEntity} entry.
	 * 
	 * @param issuerName
	 * @param serialNumber
	 * @param revocationDate
	 * @param crlNumber
	 */
	RevokedCertificateEntity addRevokedCertificate(String issuerName,
			BigInteger serialNumber, Date revocationDate, BigInteger crlNumber);

	/**
	 * List {@link RevokedCertificateEntity}'s for the specified crl number and
	 * issuer.
	 * 
	 * @param crlNumber
	 * @param issuerName
	 */
	List<RevokedCertificateEntity> listRevokedCertificates(
			BigInteger crlNumber, String issuerName);

	/**
	 * Remove {@link RevokedCertificateEntity}'s for the specified issuer that
	 * are older then the specified crl number.
	 * 
	 * @param crlNumber
	 * @param issuerName
	 * @return # of {@link RevokedCertificateEntity}'s removed.
	 */
	int removeOldRevokedCertificates(BigInteger crlNumber, String issuerName);
}