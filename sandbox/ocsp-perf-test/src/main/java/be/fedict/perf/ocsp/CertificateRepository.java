package be.fedict.perf.ocsp;

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

public class CertificateRepository {

	private final List<CertificateID> certificateIds;

	private Iterator<CertificateID> iterator;

	public CertificateRepository() throws CertificateException, OCSPException {
		this.certificateIds = new LinkedList<CertificateID>();

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
		}

		if (this.certificateIds.isEmpty()) {
			throw new RuntimeException(
					"missing certificate entries in certificates.config");
		}

		this.iterator = this.certificateIds.iterator();
	}

	public synchronized CertificateID getCertificateID() {
		if (false == this.iterator.hasNext()) {
			this.iterator = this.certificateIds.iterator();
		}
		return this.iterator.next();
	}

	public int getSize() {
		return this.certificateIds.size();
	}
}
