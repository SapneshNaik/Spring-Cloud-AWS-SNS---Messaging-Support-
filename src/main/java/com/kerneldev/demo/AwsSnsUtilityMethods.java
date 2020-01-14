package com.kerneldev.demo;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;
import java.net.URISyntaxException;

public class AwsSnsUtilityMethods {

    /**
     * Constructs an AwsBasicCredentials object with user AccessKeyID and SecretAccessKey
     *
     * @param accessKeyID
     * @param secretAccessKey
     * @return
     */
    public static AwsCredentialsProvider getAwsCredentials(String accessKeyID, String secretAccessKey) {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKeyID, secretAccessKey);

        AwsCredentialsProvider awsCredentialsProvider = () -> awsBasicCredentials;

        return awsCredentialsProvider;
    }


    /**
     * Builds a SdkHttpClient object with an Apache HTTP client configured with proxy
     *
     * @param proxy Proxy address (host:port)
     * @return
     * @throws URISyntaxException
     */
    private static SdkHttpClient getProxyHTTPClient(String proxy) throws URISyntaxException {
        URI proxyURI = new URI(proxy);

        // This HTTP Client supports system proxy
        final SdkHttpClient sdkHttpClient = ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                        .endpoint(proxyURI)
                        .build())
                .build();

        return sdkHttpClient;
    }


    /**
     * Returns an SnsClient Object that can be used to perform all AWS SNS operations
     *
     * @return
     * @throws URISyntaxException
     */
    public static SnsClient getSnsClient() throws URISyntaxException {
        return SnsClient.builder()
                .credentialsProvider(getAwsCredentials(
                        "Access Key ID",
                        "Secret Key/"))
                .region(Region.US_EAST_1) //Set your selected region
                .build();
    }
}
