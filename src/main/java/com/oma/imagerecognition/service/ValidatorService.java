package com.oma.imagerecognition.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Service
public class ValidatorService {

    /**
     * SSRF protection: block private/loopback/link-local/site-local.
     */
    public URI validatePublicUrl(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http"))) {
            throw new IllegalArgumentException("Only http/https URLs allowed");
        }
        String host = uri.getHost();
        if (host == null) throw new IllegalArgumentException("Invalid URL host");

        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                throw new IllegalArgumentException("Blocked non-public URL host: " + host);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + host);
        }

        return uri;
    }
}
