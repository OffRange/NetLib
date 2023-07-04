package de.davis.net.sync.tcp.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

public class TrustManager implements X509TrustManager {

    private final String[] serverCertificateFingerprint;

    public TrustManager(String[] serverCertificateFingerprint) {
        this.serverCertificateFingerprint = serverCertificateFingerprint;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        for(X509Certificate certificate : x509Certificates){
            certificate.checkValidity();


            if(Arrays.asList(serverCertificateFingerprint).contains("all"))
                continue;

            if(!Arrays.asList(serverCertificateFingerprint).contains(calculateSHA256Fingerprint(certificate)))
                throw new CertificateException("Server certificate is not trusted.");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    private String calculateSHA256Fingerprint(X509Certificate certificate) throws CertificateException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(certificate.getEncoded());
            return bytesToHexString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException("Failed to calculate certificate fingerprint.", e);
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
