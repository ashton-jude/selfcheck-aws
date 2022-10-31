package org.ashtonjude;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.rekognition.model.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.Image;

import java.util.Base64;
import java.util.List;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import java.io.*;
import java.util.Map;

public class ProcessPhoto implements RequestStreamHandler {
    private JSONParser parser = new JSONParser();
    static final String DYNAMODB_TABLE_STUDENT = "student";
    static AmazonDynamoDB client;
    static DynamoDB dynamoDb;
    static AmazonRekognition rekognitionClient;

    static {
        // Initialize AWS entities
        client = AmazonDynamoDBClientBuilder.defaultClient();
        dynamoDb = new DynamoDB(client);
        rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
    }

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        Student student = new Student();
        String photo = null;
        String body = null;

        try {
            JSONObject event = (JSONObject) parser.parse(reader);

            if (event.get("body") != null) {
                body = (String) event.get("body").toString();
                photo = (String) ((JSONObject) parser.parse(body)).get("photo");
            }

            // Process the photo
            student = processPhoto(photo);

            JSONObject responseBody = new JSONObject();
            responseBody.put("uuid", student.uuid);
            responseBody.put("firstName", student.firstName);
            responseBody.put("lastName", student.lastName);
            responseBody.put("grade", student.grade);
            responseBody.put("emotion", student.emotion);
            responseBody.put("isRegistered", student.isRegistered);

            JSONObject headerJson = new JSONObject();
            headerJson.put("x-custom-header", "Ashton Jude");

            responseJson.put("statusCode", 200);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

        } catch (Exception pex) {
            responseJson.put("statusCode", 200);
            responseJson.put("headers", "");
            responseJson.put("body", pex.getStackTrace());
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    private static String getStackTrace(Exception ex) {
        StringBuffer sb = new StringBuffer(500);
        StackTraceElement[] st = ex.getStackTrace();
        sb.append(ex.getClass().getName() + ": " + ex.getMessage() + "\n");
        for (int i = 0; i < st.length; i++) {
            sb.append("\t at " + st[i].toString() + "\n");
        }
        return sb.toString();
    }

    // This routine processes the photo and inserts required records
    public static Student processPhoto(String sourceImage) {

        // Get the Emotion for the source Image
        String emotion = getEmotion(sourceImage);

        // Check if the student exists in the database
        Student student = searchStudent(sourceImage);

        // If the student does not exit in the database, add the student and mark as unregistered
        if (student == null) {
            student = new Student();
            student.uuid = UUID.randomUUID().toString();
            student.photo = sourceImage;
            student.isRegistered = false;

            dynamoDb.getTable(DYNAMODB_TABLE_STUDENT)
                    .putItem(new PutItemSpec().withItem(new Item().withString("uuid", student.uuid)
                            .withString("photo", student.photo).withBoolean("isRegistered", student.isRegistered)));
        }

        // Update the emotion
        student.emotion = emotion;

        return student;
    }

    // This routine chwcks photos against current students
    public static Student searchStudent(String sourceImage) {

        Student student = null;
        ScanResult result = null;

        do {
            ScanRequest req = new ScanRequest();
            req.setTableName(DYNAMODB_TABLE_STUDENT);

            if (result != null) {
                req.setExclusiveStartKey(result.getLastEvaluatedKey());
            }

            result = client.scan(req);

            List<Map<String, AttributeValue>> rows = result.getItems();

            for (Map<String, AttributeValue> map : rows) {
                AttributeValue v = map.get("photo");
                String targetImage = v.getS();

                // Check if the source image matches target image
                if (compareFaces(sourceImage, targetImage)) {
                    student = new Student();
                    student.uuid = map.get("uuid").getS();
                    student.firstName = map.get("firstName").getS();
                    student.lastName = map.get("lastName").getS();
                    student.grade = Integer.parseInt(map.get("grade").getN());
                    student.isRegistered = map.get("isRegistered").getBOOL();
                    student.photo = sourceImage;

                    break;
                }

            }
        } while (result.getLastEvaluatedKey() != null);

        return student;
    }

    // This routine checks if 2 image are same
    public static Boolean compareFaces(String sourceImage, String targetImage) {
        Float similarityThreshold = 70F;

        Image source = new Image()
                .withBytes(ByteBuffer.wrap(Base64.getDecoder().decode(sourceImage)));
        Image target = new Image()
                .withBytes(ByteBuffer.wrap(Base64.getDecoder().decode(targetImage)));

        CompareFacesRequest request = new CompareFacesRequest()
                .withSourceImage(source)
                .withTargetImage(target)
                .withSimilarityThreshold(similarityThreshold);

        // Call operation
        CompareFacesResult compareFacesResult = rekognitionClient.compareFaces(request);

        // Matched Results
        List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();

        if (faceDetails.size() >= 1) {
            return true;
        } else {
            return false;
        }
    }


    // Returns the Emotion for the image
    public static String getEmotion(String sourceImage) {
        String returnEmotion = "UNKNOWN";

        Image source = new Image()
                .withBytes(ByteBuffer.wrap(Base64.getDecoder().decode(sourceImage)));

        // Call operation
        DetectFacesRequest detectFacesRequest = new DetectFacesRequest().withImage(source).withAttributes("ALL");
        DetectFacesResult detectFacesResult = rekognitionClient.detectFaces(detectFacesRequest);

        // Matched Results
        List<FaceDetail> faceDetails = detectFacesResult.getFaceDetails();

        if (faceDetails.size() >= 1) {
            List<Emotion> emotionList = faceDetails.get(0).getEmotions();
            float confidence = 0;

            //Get the emotion with higher confidence
            for (Emotion emotion : emotionList) {
                if (emotion.getConfidence() > confidence) {
                    confidence = emotion.getConfidence();
                    returnEmotion = emotion.getType();
                }
            }
        }
        return returnEmotion;
    }
}

