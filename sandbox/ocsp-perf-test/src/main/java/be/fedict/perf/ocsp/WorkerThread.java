package be.fedict.perf.ocsp;

import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;

public class WorkerThread extends Thread {

	private final ManagerTimerTask manager;

	private final CertificateRepository certificateRepository;

	private final NetworkConfig networkConfig;

	private final HttpClient httpClient;

	public WorkerThread(ManagerTimerTask manager,
			CertificateRepository certificateRepository,
			NetworkConfig networkConfig) {
		this.manager = manager;
		this.certificateRepository = certificateRepository;
		this.networkConfig = networkConfig;
		this.httpClient = new DefaultHttpClient();
		if (null != this.networkConfig) {
			HttpHost proxy = new HttpHost(this.networkConfig.getProxyHost(),
					this.networkConfig.getProxyPort());
			this.httpClient.getParams().setParameter(
					ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				byte[] ocspReqData = this.certificateRepository
						.getOCSPRequest();

				HttpPost httpPost = new HttpPost("http://ocsp.eid.belgium.be");
				HttpEntity httpEntity = new ByteArrayEntity(ocspReqData);
				httpPost.setEntity(httpEntity);

				long t0 = System.currentTimeMillis();
				HttpResponse httpResponse = this.httpClient.execute(httpPost);
				long t1 = System.currentTimeMillis();

				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (HttpURLConnection.HTTP_OK != statusCode) {
					throw new RuntimeException(
							"invalid OCSP HTTP status code: " + statusCode);
				}
				InputStream contentInputStream = httpResponse.getEntity()
						.getContent();
				OCSPResp ocspResp = new OCSPResp(contentInputStream);
				contentInputStream.close();
				int ocspRespStatus = ocspResp.getStatus();
				if (OCSPResponseStatus.SUCCESSFUL != ocspRespStatus) {
					throw new RuntimeException("invalid OCSP response status: "
							+ ocspRespStatus);
				}
				BasicOCSPResp basicOCSPResp = (BasicOCSPResp) ocspResp
						.getResponseObject();

				this.manager.reportWork(t1 - t0);
			}
		} catch (Exception e) {
			System.err.println("worker error: " + e.getMessage());
			throw new RuntimeException("worker error: " + e.getMessage(), e);
		}
	}
}
