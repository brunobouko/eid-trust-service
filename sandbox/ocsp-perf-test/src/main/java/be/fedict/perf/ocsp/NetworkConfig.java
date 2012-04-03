package be.fedict.perf.ocsp;

public class NetworkConfig {

	private final String proxyHost;
	
	private final int proxyPort;
	
	public NetworkConfig(String proxyHost, int proxyPort) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}
	
	public String getProxyHost() {
		return this.proxyHost;
	}
	
	public int getProxyPort() {
		return this.proxyPort;
	}
}
