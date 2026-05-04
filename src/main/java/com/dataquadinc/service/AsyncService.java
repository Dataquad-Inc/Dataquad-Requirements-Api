package com.dataquadinc.service;

import com.dataquadinc.model.RequirementsModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AsyncService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Async
    public void handlePostUpdateTasks(RequirementsModel entity, String status) {

        logger.info("Async processing started for jobId: {}", entity.getJobId());

        try {
            //Call external API only when CLOSED
            if ("closed".equalsIgnoreCase(status)) {

                String fetchUrl = "https://mymulya.com/candidate/closedjobs/" + entity.getJobId();

                ResponseEntity<List<Map<String, Object>>> fetchResponse =
                        restTemplate.exchange(
                                fetchUrl,
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                        );

                List<Map<String, Object>> candidates = fetchResponse.getBody();

                if (candidates != null && !candidates.isEmpty()) {

                    String benchUrl = "https://mymulya.com/candidate/bench/import";

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<List<Map<String, Object>>> request =
                            new HttpEntity<>(candidates, headers);

                    restTemplate.postForEntity(benchUrl, request, String.class);

                    logger.info("Bench import successful for jobId: {}", entity.getJobId());
                }
            }

            //Email sending async
            sendEmailsToRecruiters(entity);

        } catch (Exception e) {
            logger.error("Async processing failed for jobId: {}", entity.getJobId(), e);
        }
    }

    @Async
    public void sendEmailsToRecruiters(RequirementsModel entity) {
        try {
            // TODO: Implement actual email logic
            logger.info("Emails sent for jobId: {}", entity.getJobId());
        } catch (Exception e) {
            logger.error("Email sending failed for jobId: {}", entity.getJobId(), e);
        }
    }
}