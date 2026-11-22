package com.ajaykumar.ai_helpdesk.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class GeminiServiceTest {

    @Test
    void analyzeIssue_withoutApiKey_throwsClearError(){
        GeminiService service=new GeminiService();
        ReflectionTestUtils.setField(service,"apiKey","");

        IllegalStateException ex=assertThrows(IllegalStateException.class,
            () -> service.analyzeIssue("Wi-fi not working"));

    assertEquals("Gemini API key is not configured",ex.getMessage());
    }
    
}
