package com.cloudant.client.api.model;

import javax.net.ssl.SSLSocketFactory;

/**
 * Represents optional configuration properties for connecting to CloudantDB.
 * @author Ganesh K Choudhary
 */
public class ConnectOptions {

	private int socketTimeout;
	private int connectionTimeout ;
	private int maxConnections ;
	
	private String proxyHost ;
	private int proxyPort ;
	private boolean isSSLAuthenticationDisabled;
	private SSLSocketFactory secureSSLSocketFactory;
	
	public ConnectOptions(){
		// default constructor
	}

	public ConnectOptions setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
		return this ;
	}

	public ConnectOptions setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this ;
	}

	public ConnectOptions setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
		return this ;
	}

	public ConnectOptions setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
		return this ;
	}

	public ConnectOptions setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
		return this ;
	}

	/** Sets whether hostname verification and certificate chain validation
	 * should be disabled.
	 * @param disabled set to true to disable or false to enable.
	 * @return the updated {@link ConnectOptions} object.
	 * @see #isSSLAuthenticationDisabled */
	public ConnectOptions setSSLAuthenticationDisabled(boolean disabled) {
		this.isSSLAuthenticationDisabled = disabled;
		return this;
	}

	/**
	 * Specifies the SSLSocketFactory to use when connecting to CloudantDB
	 * over a <code>https</code> URL.
	 * @param factory An SSLSocketFactory, or <code>null</code> for the
	 *                default SSLSocketFactory of the JRE.
	 * @see #getSecureSSLSocketFactory()
	 */
	public ConnectOptions setSecureSSLSocketFactory(SSLSocketFactory factory) {
		this.secureSSLSocketFactory = factory;
		return this;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	/** @return true if hostname verification and certificate chain validation are
	 *  set to disabled or false otherwise.
	 *  @see #setSSLAuthenticationDisabled(boolean) */
	public boolean isSSLAuthenticationDisabled() {
		return isSSLAuthenticationDisabled;
	}

	/**
	 * Returns the SSLSocketFactory that gets used when connecting to
	 * CloudantDB over a <code>https</code> URL.
	 * @return An SSLSocketFactory, or <code>null</code>, which stands for
	 *         the default SSLSocketFactory of the JRE.
	 * @see #setSecureSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
	 */
	public SSLSocketFactory getSecureSSLSocketFactory() {
		return secureSSLSocketFactory;
	}
}
