package com.ajaykumar.ai_helpdesk.service;
import java.util.Map;
import java.util.List;




import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpServerErrorException;

import tools.jackson.databind.JsonNode;


//this service sends the ticket issue to gemini and receives its AI-generated analysis.
@Service
public class GeminiService {
    private static final String GEMINI_URL="https://generativelanguage.googleapis.com/v1beta/models/";
    private static final List<String> MODELS=List.of(
        "gemini-3.8-flash"
    );
    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.8-flash}")
    private String model;

    private final RestClient restClient=RestClient.create();
    public String analyzeIssue(String issueDescription){
        if(apiKey ==null || apiKey.isBlank()){
            throw new IllegalStateException("Gemini API key is not configured");

        }String  prompt="""
        You are an AI customer support assistant
        Analyze this customer issue: %s
        Provide:
        1.Category
        2.Priority(low,medium or high)
        3.Possible reason
        4.Step-by-step suggested solution
        use simple english """.formatted(issueDescription);

Map<String,Object> request=Map.of(
    "contents", List.of(Map.of(
        "parts", List.of(Map.of("text",prompt))
    ))
);

JsonNode response=null;
RuntimeException lastError=null;

for(int attempt =1;attempt <=4; attempt++){
    try{

    response =restClient.post()
    .uri(GEMINI_URL+model +":generateContent")
.header("x-goog-api-key",apiKey)
.contentType(MediaType.APPLICATION_JSON)
.body(request)
.retrieve()
.body(JsonNode.class);
break;
    
} catch (org.springframework.web.client.HttpServerErrorException  e) {
if(attempt ==4)
    throw new IllegalStateException("AI service is busy,please try again later");
}
try{
    Thread.sleep(3000L*attempt);
}catch(InterruptedException ie){
    Thread.currentThread().interrupt();
    throw new IllegalStateException("Interrupted");
}

}



JsonNode result=response.path("candidates")
.path(0)
.path("content")
.path("parts")
.path(0)
.path("text");

if(result.isMissingNode() || result.isNull()){
    throw new IllegalStateException("Gemini returned an empty response");

}return result.asText();


}}