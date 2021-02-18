package repository;

import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.inject.ApplicationLifecycle;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Singleton
public class AmazonS3Client {
  public static final String AWS_S3_BUCKET = "aws.s3.bucket";
  private static final Logger log = LoggerFactory.getLogger("s3client");

  private final ApplicationLifecycle appLifecycle;
  private final Config config;
  private String bucket;
  private S3Client s3;

  @Inject
  public AmazonS3Client(ApplicationLifecycle appLifecycle, Config config) {
    this.appLifecycle = appLifecycle;
    this.config = config;

    log.info("aws s3 enabled: " + String.valueOf(enabled()));
    if (enabled()) {
      connect();
      putTestObject();
      getTestObject();
    }

    appLifecycle.addStopHook(
        () -> {
          if (s3 != null) {
            s3.close();
          }
          return CompletableFuture.completedFuture(null);
        });
  }

  public boolean enabled() {
    return config.hasPath(AWS_S3_BUCKET);
  }

  public void putObject(String key, byte[] data) {
    throwIfUninitialized();

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucket).key(key).build();
      s3.putObject(putObjectRequest, RequestBody.fromBytes(data));
    } catch (S3Exception e) {
      throw new RuntimeException("S3 exception: " + e.getMessage());
    }
  }

  public byte[] getObject(String key) {
    throwIfUninitialized();

    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().key(key).bucket(bucket).build();
      ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(getObjectRequest);
    } catch (S3Exception e) {
      throw new RuntimeException("S3 exception: " + e.getMessage());
    }
    return objectBytes.asByteArray();
  }

  private void throwIfUninitialized() {
    if (s3 == null) {
      throw new RuntimeException("S3Client is not initialized.");
    }
  }

  private void putTestObject() {
    String testInput = "UAT S3 test content";
    putObject("file1", testInput.getBytes());
  }

  private void getTestObject() {
    byte[] data = getObject("file1");
    log.info("got data from s3: " + new String(data));
  }

  private void connect() {
    bucket = config.getString(AWS_S3_BUCKET);

    Region region = Region.US_WEST_2;
    s3 = S3Client.builder().region(region).build();
  }
}
