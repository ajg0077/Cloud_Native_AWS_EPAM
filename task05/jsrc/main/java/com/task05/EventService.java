package com.task05;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.model.Region;
import com.task05.entity.Event;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;


public class EventService {
    private DynamoDBMapper dynamoDBMapper;
    private static String jsonBody = null;

    public APIGatewayProxyResponseEvent saveEvent(APIGatewayProxyRequestEvent apiGatewayRequest, Context context) {
        try {
            initDynamoDB();
            String requestString = apiGatewayRequest.getBody();
            JSONParser parser = new JSONParser();
            JSONObject requestJsonObject = (JSONObject) parser.parse(requestString);
            int principalId = Integer.parseInt(requestJsonObject.get("principalId").toString());
            Map<String, String> content = (Map<String, String>) requestJsonObject.get("content");
            String id = java.util.UUID.randomUUID().toString();

            // Get current datetime in ISO 8601 format
            String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            Event event = new Event();
            event.setId(id);
            event.setPrincipalId(principalId);
            event.setBody(content.toString());
            event.setCreatedAt(createdAt);
            dynamoDBMapper.save(event);
            context.getLogger().log("data saved successfully to dynamodb:::" + jsonBody);
            return createAPIResponse(event, 201);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void initDynamoDB() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.EU_CENTRAL_1)
                .build();
        dynamoDBMapper = new DynamoDBMapper(client);
    }

    private APIGatewayProxyResponseEvent createAPIResponse(Event event, int statusCode) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setBody("{ \"id\": \"" + event.getId() + "\", \"principalId\": " + event.getPrincipalId() + ", \"createdAt\": \"" + event.getCreatedAt() + "\", \"body\": " + event.getBody() + " }");
        responseEvent.setStatusCode(statusCode);
        return responseEvent;
    }
}
