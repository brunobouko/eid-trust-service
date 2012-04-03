package test.unit.be.fedict.perf.ocsp;

import static org.junit.Assert.assertArrayEquals;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.perf.ocsp.CertificateRepository;

public class OCSPTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	//@Test
	public void testOCSPRequestConstant() throws Exception {
		// setup
		CertificateRepository certificateRepository = new CertificateRepository(false);
		CertificateID certificateID = null; //certificateRepository.getCertificateID();

		// operate
		byte[] ocspReqData1;
		{
			OCSPReqGenerator ocspReqGenerator = new OCSPReqGenerator();
			ocspReqGenerator.addRequest(certificateID);
			OCSPReq ocspReq = ocspReqGenerator.generate();
			ocspReqData1 = ocspReq.getEncoded();
		}

		byte[] ocspReqData2;
		{
			OCSPReqGenerator ocspReqGenerator = new OCSPReqGenerator();
			ocspReqGenerator.addRequest(certificateID);
			OCSPReq ocspReq = ocspReqGenerator.generate();
			ocspReqData2 = ocspReq.getEncoded();
		}

		// verify
		assertArrayEquals(ocspReqData1, ocspReqData2);
	}
}
