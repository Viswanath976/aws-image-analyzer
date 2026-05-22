package img.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageProcessorHandler implements RequestHandler<S3Event, String> {

    private static final String TABLE_NAME = "ImageAnalysis";
    private static final Region REGION = Region.US_EAST_1;

    private final RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(REGION)
            .build();

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .region(REGION)
            .build();

    @Override
    public String handleRequest(S3Event event, Context context) {

        S3EventNotificationRecord record = event.getRecords().get(0);
        String bucketName = record.getS3().getBucket().getName();
        String fileName = URLDecoder.decode(
                record.getS3().getObject().getKey(),
                StandardCharsets.UTF_8
        );

        context.getLogger().log("Processing Uploaded File: " + fileName + " from bucket: " + bucketName);

        try {
            Image s3Image = Image.builder()
                    .s3Object(S3Object.builder()
                            .bucket(bucketName)
                            .name(fileName)
                            .build())
                    .build();

            // 1. Detect generic labels
            DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder()
                    .image(s3Image)
                    .maxLabels(10)
                    .minConfidence(75F)
                    .build();

            DetectLabelsResponse labelsResponse = rekognitionClient.detectLabels(labelsRequest);
            List<String> labelNames = labelsResponse.labels().stream()
                    .map(Label::name)
                    .collect(Collectors.toList());
            context.getLogger().log("Found Labels: " + labelNames);

            // 2. Recognize celebrities
            RecognizeCelebritiesRequest celebRequest = RecognizeCelebritiesRequest.builder()
                    .image(s3Image)
                    .build();

            RecognizeCelebritiesResponse celebResponse = rekognitionClient.recognizeCelebrities(celebRequest);
            List<String> celebrityNames = new ArrayList<>();
            for (Celebrity celebrity : celebResponse.celebrityFaces()) {
                String name = celebrity.name();
                double confidence = celebrity.matchConfidence();
                context.getLogger().log("Found celebrity: " + name + " with " + confidence + "% confidence");
                celebrityNames.add(name);
            }

            // 3. Store in DynamoDB - single item map
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("imageName", AttributeValue.builder().s(fileName).build());
            item.put("processedAt", AttributeValue.builder().s(Instant.now().toString()).build());

            // Store labels as List
            if (!labelNames.isEmpty()) {
                List<AttributeValue> labelList = labelNames.stream()
                        .map(l -> AttributeValue.builder().s(l).build())
                        .collect(Collectors.toList());
                item.put("labels", AttributeValue.builder().l(labelList).build());
            }

            // Store celebrities as String Set, only if found
            if (!celebrityNames.isEmpty()) {
                item.put("celebrities", AttributeValue.builder().ss(celebrityNames).build());
            }

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(item)
                    .build();

            context.getLogger().log("Writing to table: " + TABLE_NAME + " in region: " + REGION);
            dynamoDbClient.putItem(request);
            context.getLogger().log("Successfully saved to DynamoDB.");

            return "Image Processed Successfully";

        } catch (InvalidImageFormatException e) {
            context.getLogger().log("ERROR: File " + fileName + " has invalid image format. " + e.getMessage());
            return "Failed: Invalid Image Format";
        } catch (Exception e) {
            context.getLogger().log("SYSTEM ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}