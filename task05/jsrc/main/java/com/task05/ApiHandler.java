package com.task05;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role"
)
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "region", value = "${region}"),
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final String TABLE_NAME = "Events";
	private final AmazonDynamoDB DYNAMO_DB_CLIENT;
	private final DynamoDB DYNAMO_DB;
    public ApiHandler() {
        this.DYNAMO_DB_CLIENT = AmazonDynamoDBClientBuilder.standard()
                .withRegion(System.getenv("region"))
                .build();
        this.DYNAMO_DB = new DynamoDB(DYNAMO_DB_CLIENT);
    }
	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

		try {
			// Parse request body
			String requestString = event.getBody();
			JSONParser parser = new JSONParser();
			JSONObject requestJsonObject = (JSONObject) parser.parse(requestString);
			int principalId = Integer.parseInt(requestJsonObject.get("principalId").toString());
			Map<String, String> content = (Map<String, String>) requestJsonObject.get("content");

			// Generate UUID for id
			String id = java.util.UUID.randomUUID().toString();

			// Get current datetime in ISO 8601 format
			String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

			// Save event to DynamoDB
			Table table = DYNAMO_DB.getTable(TABLE_NAME);
			Item item = new Item()
					.withPrimaryKey("id", id)
					.withInt("principalId", principalId)
					.withString("createdAt", createdAt)
					.withMap("body", content);
			table.putItem(new PutItemSpec().withItem(item));

			// Prepare response
			response.setStatusCode(201);
			response.setBody("{ \"id\": \"" + id + "\", \"principalId\": " + principalId + ", \"createdAt\": \"" + createdAt + "\", \"body\": " + content + " }");
		} catch (Exception e) {
			response.setStatusCode(500);
			response.setBody("Internal Server Error");
			e.printStackTrace();
		}

		return response;
	}
}
