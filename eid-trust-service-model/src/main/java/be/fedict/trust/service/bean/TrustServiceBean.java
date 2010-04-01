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

package be.fedict.trust.service.bean;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CRLException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.Interceptors;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TSPException;

import be.fedict.trust.FallbackTrustLinker;
import be.fedict.trust.NetworkConfig;
import be.fedict.trust.PublicKeyTrustLinker;
import be.fedict.trust.RevocationData;
import be.fedict.trust.TrustLinker;
import be.fedict.trust.TrustLinkerResult;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.constraints.CertificatePoliciesCertificateConstraint;
import be.fedict.trust.constraints.DistinguishedNameCertificateConstraint;
import be.fedict.trust.constraints.EndEntityCertificateConstraint;
import be.fedict.trust.constraints.KeyUsageCertificateConstraint;
import be.fedict.trust.constraints.QCStatementsCertificateConstraint;
import be.fedict.trust.crl.CachedCrlRepository;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.crl.OfflineCrlRepository;
import be.fedict.trust.crl.OnlineCrlRepository;
import be.fedict.trust.ocsp.OcspTrustLinker;
import be.fedict.trust.ocsp.OfflineOcspRepository;
import be.fedict.trust.ocsp.OnlineOcspRepository;
import be.fedict.trust.service.SnmpConstants;
import be.fedict.trust.service.TrustService;
import be.fedict.trust.service.ValidationResult;
import be.fedict.trust.service.dao.ConfigurationDAO;
import be.fedict.trust.service.dao.TrustDomainDAO;
import be.fedict.trust.service.entity.TrustDomainEntity;
import be.fedict.trust.service.entity.VirtualTrustDomainEntity;
import be.fedict.trust.service.entity.WSSecurityConfigEntity;
import be.fedict.trust.service.entity.constraints.CertificateConstraintEntity;
import be.fedict.trust.service.entity.constraints.DNConstraintEntity;
import be.fedict.trust.service.entity.constraints.EndEntityConstraintEntity;
import be.fedict.trust.service.entity.constraints.KeyUsageConstraintEntity;
import be.fedict.trust.service.entity.constraints.PolicyConstraintEntity;
import be.fedict.trust.service.entity.constraints.QCStatementsConstraintEntity;
import be.fedict.trust.service.exception.TrustDomainNotFoundException;
import be.fedict.trust.service.snmp.SNMP;
import be.fedict.trust.service.snmp.SNMPCounter;
import be.fedict.trust.service.snmp.SNMPInterceptor;

/**
 * Trust Service Bean implementation.
 * 
 * @author fcorneli
 * 
 */
@Stateless
@Interceptors(SNMPInterceptor.class)
public class TrustServiceBean implements TrustService {

	private static final Log LOG = LogFactory.getLog(TrustServiceBean.class);

	@EJB
	private ConfigurationDAO configurationDAO;

	@PersistenceContext
	private EntityManager entityManager;

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = HarvesterMDB.HARVESTER_QUEUE_NAME)
	private Queue queue;

	@EJB
	private TrustDomainDAO trustDomainDAO;

	@SNMP(oid = SnmpConstants.CACHE_HITS)
	private Long cacheHits;

	@SNMP(oid = SnmpConstants.CACHE_MISSES)
	private Long cacheMisses;

	@SNMP(oid = SnmpConstants.CACHE_HIT_PERCENTAGE, derived = true)
	private Long cacheHitPercentage;

	/**
	 * {@inheritDoc}
	 */
	public ValidationResult validate(List<X509Certificate> certificateChain) {

		try {
			return validate(null, certificateChain, false);
		} catch (TrustDomainNotFoundException e) {
			LOG.error("Default trust domain not set ?!");
			return new ValidationResult(new TrustLinkerResult(false), null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@SNMP(oid = SnmpConstants.VALIDATE)
	public ValidationResult validate(String trustDomainName,
			List<X509Certificate> certificateChain, boolean returnRevocationData)
			throws TrustDomainNotFoundException {

		LOG.debug("isValid: "
				+ certificateChain.get(0).getSubjectX500Principal());

		TrustLinkerResult lastResult = null;
		RevocationData lastRevocationData = null;
		for (TrustDomainEntity trustDomain : getTrustDomains(trustDomainName)) {

			TrustValidator trustValidator = getTrustValidator(trustDomain,
					returnRevocationData);
			try {
				trustValidator.isTrusted(certificateChain);
			} catch (CertPathValidatorException e) {
			}

			if (trustValidator.getResult().isValid()) {
				LOG.debug("valid for trust domain: " + trustDomain.getName());
				return new ValidationResult(trustValidator.getResult(),
						trustValidator.getRevocationData());
			}

			lastResult = trustValidator.getResult();
			lastRevocationData = trustValidator.getRevocationData();
		}

		return new ValidationResult(lastResult, lastRevocationData);
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@SNMP(oid = SnmpConstants.VALIDATE)
	public ValidationResult validate(String trustDomainName,
			List<X509Certificate> certificateChain, Date validationDate,
			List<byte[]> ocspResponses, List<byte[]> crls)
			throws TrustDomainNotFoundException, CertificateException,
			NoSuchProviderException, CRLException, IOException {

		LOG.debug("isValid: "
				+ certificateChain.get(0).getSubjectX500Principal());

		TrustLinkerResult lastResult = null;
		RevocationData lastRevocationData = null;
		for (TrustDomainEntity trustDomain : getTrustDomains(trustDomainName)) {

			TrustValidator trustValidator = getTrustValidator(trustDomain,
					ocspResponses, crls);

			try {
				trustValidator.isTrusted(certificateChain);
			} catch (CertPathValidatorException e) {
			}

			if (trustValidator.getResult().isValid()) {
				LOG.debug("valid for trust domain: " + trustDomain.getName());
				return new ValidationResult(trustValidator.getResult(),
						trustValidator.getRevocationData());
			}

			lastResult = trustValidator.getResult();
			lastRevocationData = trustValidator.getRevocationData();
		}

		return new ValidationResult(lastResult, lastRevocationData);
	}

	/**
	 * {@inheritDoc}
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ValidationResult validateTimestamp(String trustDomainName,
			byte[] encodedTimestampToken) throws TSPException, IOException,
			CMSException, NoSuchAlgorithmException, NoSuchProviderException,
			CertStoreException, TrustDomainNotFoundException {

		LOG.debug("validate timestamp token");

		TrustLinkerResult lastResult = null;
		RevocationData lastRevocationData = null;
		for (TrustDomainEntity trustDomain : getTrustDomains(trustDomainName)) {

			TrustValidator trustValidator = getTSATrustValidator(trustDomain);

			try {
				trustValidator.isTrusted(encodedTimestampToken);
			} catch (CertPathValidatorException e) {
			}

			if (trustValidator.getResult().isValid()) {
				LOG.debug("valid for trust domain: " + trustDomain.getName());
				return new ValidationResult(trustValidator.getResult(),
						trustValidator.getRevocationData());
			}

			lastResult = trustValidator.getResult();
			lastRevocationData = trustValidator.getRevocationData();
		}

		return new ValidationResult(lastResult, lastRevocationData);
	}

	/**
	 * Returns {@link Set} of {@link TrustDomainEntity}'s. Multiple are possible
	 * if the specified trustDomainName is a {@link VirtualTrustDomainEntity}.
	 */
	private Set<TrustDomainEntity> getTrustDomains(String name)
			throws TrustDomainNotFoundException {

		if (null == name) {
			return Collections.singleton(this.trustDomainDAO
					.getDefaultTrustDomain());
		} else {
			TrustDomainEntity trustDomain = this.trustDomainDAO
					.findTrustDomain(name);
			if (null != trustDomain) {
				return Collections.singleton(trustDomain);
			} else {
				// maybe a virtual trust domain?
				VirtualTrustDomainEntity virtualTrustDomain = this.trustDomainDAO
						.findVirtualTrustDomain(name);
				if (null != virtualTrustDomain) {
					return virtualTrustDomain.getTrustDomains();
				}
			}
		}

		throw new TrustDomainNotFoundException();
	}

	/**
	 * Returns new {@link TrustValidator}.
	 * 
	 * @param returnRevocationData
	 *            if <code>true</code> the used revocation data will be filled
	 *            in the returned {@link TrustValidator}.
	 */
	private TrustValidator getTrustValidator(TrustDomainEntity trustDomain,
			boolean returnRevocationData) {

		TrustLinker trustLinker = null;
		if (!returnRevocationData && trustDomain.isUseCaching()) {
			// if returnRevocationData set, don't use cached revocation data
			trustLinker = new TrustServiceTrustLinker(this.entityManager,
					this.queueConnectionFactory, this.queue);
		}

		return getTrustValidator(trustDomain, trustLinker, returnRevocationData);
	}

	private TrustValidator getTSATrustValidator(TrustDomainEntity trustDomain) {

		NetworkConfig networkConfig = configurationDAO.getNetworkConfig();

		TrustDomainCertificateRepository certificateRepository = new TrustDomainCertificateRepository(
				trustDomain);

		TrustValidator trustValidator = new TrustValidator(
				certificateRepository);

		trustValidator.addTrustLinker(new PublicKeyTrustLinker());

		OnlineOcspRepository ocspRepository = new OnlineOcspRepository(
				networkConfig);

		OnlineCrlRepository crlRepository = new OnlineCrlRepository(
				networkConfig);
		CachedCrlRepository cachedCrlRepository = new CachedCrlRepository(
				crlRepository);

		FallbackTrustLinker fallbackTrustLinker = new FallbackTrustLinker();
		fallbackTrustLinker.addTrustLinker(new OcspTrustLinker(ocspRepository));
		fallbackTrustLinker.addTrustLinker(new CrlTrustLinker(
				cachedCrlRepository));

		trustValidator.addTrustLinker(fallbackTrustLinker);

		return trustValidator;
	}

	/**
	 * Returns new {@link TrustValidator} configured according to the specified
	 * {@link TrustDomainEntity}.
	 * 
	 * @param trustDomain
	 * @param trustLinker
	 *            optional customized {@link TrustLinker}. Can be
	 *            <code>null</code>.
	 * @param returnRevocationData
	 *            if <code>true</code> the used revocation data will be filled
	 *            in the returned {@link TrustValidator}.
	 */
	private TrustValidator getTrustValidator(TrustDomainEntity trustDomain,
			TrustLinker trustLinker, boolean returnRevocationData) {

		NetworkConfig networkConfig = configurationDAO.getNetworkConfig();

		TrustDomainCertificateRepository certificateRepository = new TrustDomainCertificateRepository(
				trustDomain);

		TrustValidator trustValidator;
		if (returnRevocationData) {
			trustValidator = new TrustValidator(certificateRepository,
					new RevocationData());
		} else {
			trustValidator = new TrustValidator(certificateRepository);
		}
		trustValidator.addTrustLinker(new PublicKeyTrustLinker());

		OnlineOcspRepository ocspRepository = new OnlineOcspRepository(
				networkConfig);

		OnlineCrlRepository crlRepository = new OnlineCrlRepository(
				networkConfig);
		CachedCrlRepository cachedCrlRepository = new CachedCrlRepository(
				crlRepository);

		FallbackTrustLinker fallbackTrustLinker = new FallbackTrustLinker();
		if (null != trustLinker) {
			fallbackTrustLinker.addTrustLinker(trustLinker);
		}
		fallbackTrustLinker.addTrustLinker(new OcspTrustLinker(ocspRepository));
		fallbackTrustLinker.addTrustLinker(new CrlTrustLinker(
				cachedCrlRepository));

		trustValidator.addTrustLinker(fallbackTrustLinker);

		addConstraints(trustValidator, trustDomain);
		return trustValidator;
	}

	/**
	 * Returns new {@link TrustValidator} configured according to the specified
	 * {@link TrustDomainEntity} and using the specified revocation date. All
	 * validation will be done offline, not using any cache.
	 * 
	 * @param trustDomain
	 * @param ocspResponses
	 * @param crls
	 * 
	 * @throws IOException
	 * @throws CRLException
	 * @throws NoSuchProviderException
	 * @throws CertificateException
	 */
	private TrustValidator getTrustValidator(TrustDomainEntity trustDomain,
			List<byte[]> ocspResponses, List<byte[]> crls) throws IOException,
			CertificateException, NoSuchProviderException, CRLException {

		LOG
				.debug("get trust validator using specified ocsp repsonses and crls");

		TrustDomainCertificateRepository certificateRepository = new TrustDomainCertificateRepository(
				trustDomain);

		TrustValidator trustValidator = new TrustValidator(
				certificateRepository);
		trustValidator.addTrustLinker(new PublicKeyTrustLinker());

		OfflineOcspRepository ocspRepository = new OfflineOcspRepository(
				ocspResponses);
		OfflineCrlRepository crlRepository = new OfflineCrlRepository(crls);

		FallbackTrustLinker fallbackTrustLinker = new FallbackTrustLinker();

		fallbackTrustLinker.addTrustLinker(new TrustServiceOcspTrustLinker(
				ocspRepository));
		fallbackTrustLinker.addTrustLinker(new TrustServiceCrlTrustLinker(
				crlRepository));

		trustValidator.addTrustLinker(fallbackTrustLinker);

		addConstraints(trustValidator, trustDomain);
		return trustValidator;
	}

	/**
	 * Add certificate constraints to the specified {@link TrustValidator} using
	 * the specified {@link TrustDomainEntity}'s configuration.
	 * 
	 * @param trustValidator
	 * @param trustDomain
	 */
	private void addConstraints(TrustValidator trustValidator,
			TrustDomainEntity trustDomain) {

		// add certificate constraints
		CertificatePoliciesCertificateConstraint certificatePoliciesCertificateConstraint = null;
		KeyUsageCertificateConstraint keyUsageCertificateConstraint = null;
		EndEntityCertificateConstraint endEntityCertificateConstraint = null;
		for (CertificateConstraintEntity certificateConstraint : trustDomain
				.getCertificateConstraints()) {

			if (certificateConstraint instanceof PolicyConstraintEntity) {

				PolicyConstraintEntity policyConstraint = (PolicyConstraintEntity) certificateConstraint;
				if (null == certificatePoliciesCertificateConstraint) {
					certificatePoliciesCertificateConstraint = new CertificatePoliciesCertificateConstraint();
				}
				certificatePoliciesCertificateConstraint
						.addCertificatePolicy(policyConstraint.getPolicy());

			} else if (certificateConstraint instanceof KeyUsageConstraintEntity) {

				KeyUsageConstraintEntity keyUsageConstraint = (KeyUsageConstraintEntity) certificateConstraint;
				if (null == keyUsageCertificateConstraint) {
					keyUsageCertificateConstraint = new KeyUsageCertificateConstraint();
				}
				switch (keyUsageConstraint.getKeyUsage()) {
				case DIGITAL_SIGNATURE: {
					keyUsageCertificateConstraint
							.setDigitalSignatureFilter(keyUsageConstraint
									.isAllowed());
					break;
				}
				case NON_REPUDIATION: {
					keyUsageCertificateConstraint
							.setNonRepudiationFilter(keyUsageConstraint
									.isAllowed());
					break;
				}
				case KEY_ENCIPHERMENT: {
					keyUsageCertificateConstraint
							.setKeyEnciphermentFilter(keyUsageConstraint
									.isAllowed());
				}
				case DATA_ENCIPHERMENT: {
					keyUsageCertificateConstraint
							.setDataEnciphermentFilter(keyUsageConstraint
									.isAllowed());
				}
				case KEY_AGREEMENT: {
					keyUsageCertificateConstraint
							.setKeyAgreementFilter(keyUsageConstraint
									.isAllowed());
				}
				case KEY_CERT_SIGN: {
					keyUsageCertificateConstraint
							.setKeyCertificateSigningFilter(keyUsageConstraint
									.isAllowed());
				}
				case CRL_SIGN: {
					keyUsageCertificateConstraint
							.setCRLSigningFilter(keyUsageConstraint.isAllowed());
				}
				case ENCIPHER_ONLY: {
					keyUsageCertificateConstraint
							.setEncipherOnlyFilter(keyUsageConstraint
									.isAllowed());
				}
				case DECIPHER_ONLY: {
					keyUsageCertificateConstraint
							.setDecipherOnlyFilter(keyUsageConstraint
									.isAllowed());
				}
				}

			} else if (certificateConstraint instanceof QCStatementsConstraintEntity) {

				QCStatementsConstraintEntity qcStatementsConstraint = (QCStatementsConstraintEntity) certificateConstraint;
				trustValidator
						.addCertificateConstrain(new QCStatementsCertificateConstraint(
								qcStatementsConstraint.getQcComplianceFilter()));

			} else if (certificateConstraint instanceof DNConstraintEntity) {

				DNConstraintEntity dnConstraint = (DNConstraintEntity) certificateConstraint;
				trustValidator
						.addCertificateConstrain(new DistinguishedNameCertificateConstraint(
								dnConstraint.getDn()));

			} else if (certificateConstraint instanceof EndEntityConstraintEntity) {

				EndEntityConstraintEntity endEntityConstraint = (EndEntityConstraintEntity) certificateConstraint;
				if (null == endEntityCertificateConstraint) {
					endEntityCertificateConstraint = new EndEntityCertificateConstraint();
				}
				endEntityCertificateConstraint
						.addEndEntity(endEntityConstraint.getIssuerName(),
								endEntityConstraint.getSerialNumber());
			}
		}

		if (null != certificatePoliciesCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(certificatePoliciesCertificateConstraint);
		}
		if (null != keyUsageCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(keyUsageCertificateConstraint);
		}
		if (null != endEntityCertificateConstraint) {
			trustValidator
					.addCertificateConstrain(endEntityCertificateConstraint);
		}
	}

	@SNMPCounter
	public void updateCacheHitPercentage() {

		LOG.debug("update cache hit %");
		if (0L == this.cacheHits && 0L == this.cacheMisses) {
			return;
		}

		double v = ((double) this.cacheHits / (double) (this.cacheHits + this.cacheMisses));
		this.cacheHitPercentage = Math.round(v * 100);
		LOG.debug("cache hit % = " + this.cacheHitPercentage);
	}

	/**
	 * {@inheritDoc}
	 */
	public WSSecurityConfigEntity getWsSecurityConfig() {

		return this.configurationDAO.getWSSecurityConfig();
	}
}
