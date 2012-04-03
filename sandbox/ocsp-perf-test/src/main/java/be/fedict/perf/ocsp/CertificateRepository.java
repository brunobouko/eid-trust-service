package be.fedict.perf.ocsp;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;

public class CertificateRepository {

	private final List<CertificateID> certificateIds;

	private Iterator<CertificateID> certificateIditerator;

	private final List<byte[]> ocspRequests;

	private Iterator<byte[]> ocspRequestIterator;

	public CertificateRepository() throws CertificateException, OCSPException,
			IOException {
		this.certificateIds = new LinkedList<CertificateID>();
		this.ocspRequests = new LinkedList<byte[]>();

		InputStream certificatesConfigInputStream = CertificateRepository.class
				.getResourceAsStream("/be/fedict/perf/ocsp/certificates.config");
		if (null == certificatesConfigInputStream) {
			throw new RuntimeException("certificates.config not found");
		}

		Map<String, X509Certificate> caCertificates = new HashMap<String, X509Certificate>();
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");

		Scanner scanner = new Scanner(certificatesConfigInputStream);
		scanner.useDelimiter("\t|\n");
		while (scanner.hasNextLine()) {
			String caAlias = scanner.next();
			X509Certificate caCertificate = caCertificates.get(caAlias);
			if (null == caCertificate) {
				InputStream caCertificateInputStream = CertificateRepository.class
						.getResourceAsStream("/be/fedict/perf/ocsp/" + caAlias
								+ ".crt");
				if (null == caCertificateInputStream) {
					throw new RuntimeException("missing CA certificate: "
							+ caAlias);
				}
				caCertificate = (X509Certificate) certificateFactory
						.generateCertificate(caCertificateInputStream);
				caCertificates.put(caAlias, caCertificate);
			}

			BigInteger certificateSerialNumber = scanner.nextBigInteger();
			CertificateID certificateID = new CertificateID(
					CertificateID.HASH_SHA1, caCertificate,
					certificateSerialNumber);
			this.certificateIds.add(certificateID);

			OCSPReqGenerator ocspReqGenerator = new OCSPReqGenerator();
			ocspReqGenerator.addRequest(certificateID);
			OCSPReq ocspReq = ocspReqGenerator.generate();
			byte[] ocspReqData = ocspReq.getEncoded();
			this.ocspRequests.add(ocspReqData);
		}

		if (this.certificateIds.isEmpty()) {
			throw new RuntimeException(
					"missing certificate entries in certificates.config");
		}

		this.certificateIditerator = this.certificateIds.iterator();
		this.ocspRequestIterator = this.ocspRequests.iterator();
	}

	public synchronized CertificateID getCertificateID() {
		if (false == this.certificateIditerator.hasNext()) {
			this.certificateIditerator = this.certificateIds.iterator();
		}
		return this.certificateIditerator.next();
	}

	public synchronized byte[] getOCSPRequest() {
		if (false == this.ocspRequestIterator.hasNext()) {
			this.ocspRequestIterator = this.ocspRequests.iterator();
		}
		return this.ocspRequestIterator.next();
	}

	public int getSize() {
		return this.certificateIds.size();
	}
}
