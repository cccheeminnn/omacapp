package vttp2022.project.addressprocessor.configs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    
    @Value("${spaces.endpoint}")
    private String endPoint;

    @Value("${spaces.region}")
    private String region;

    @Bean
    public AmazonS3 create() 
    {
        final String accessKey = System.getenv("S3_ACCESS_KEY");
        final String secretKey = System.getenv("S3_SECRET_KEY");

        //S3 Credentials
        final BasicAWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);
        final AWSStaticCredentialsProvider credProv = new AWSStaticCredentialsProvider(cred);
        final EndpointConfiguration endPointConfig = new EndpointConfiguration(endPoint, region);

        return AmazonS3ClientBuilder
            .standard()
            .withCredentials(credProv)
            .withEndpointConfiguration(endPointConfig)
            .build();
    }
}
