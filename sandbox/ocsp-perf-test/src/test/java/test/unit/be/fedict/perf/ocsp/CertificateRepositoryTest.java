package test.unit.be.fedict.perf.ocsp;

import static org.junit.Assert.*;

import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.perf.ocsp.CertificateRepository;

public class CertificateRepositoryTest {

	private static final Log LOG = LogFactory
			.getLog(CertificateRepositoryTest.class);

	@BeforeClass
	public static void beforeClass() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testLoadCertificateRepository() throws Exception {
		// operate
		CertificateRepository certificateRepository = new CertificateRepository(false);

		// verify
		LOG.debug("number of certificates in repository: "
				+ certificateRepository.getSize());
		assertTrue(certificateRepository.getSize() > 1000);
	}
}
