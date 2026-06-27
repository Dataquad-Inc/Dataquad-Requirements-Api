package com.dataquadinc.service;
import com.dataquadinc.dto.InProgressRequirementDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@Service
public class InProgressEmailScheduler {

    private final RequirementsService  requirementsService;

    public InProgressEmailScheduler(RequirementsService  requirementsService) {
        this.requirementsService = requirementsService;
    }

    @Scheduled(cron = "0 0 18 * * ?",zone = "Asia/Kolkata")
    public void sendDailyInProgressReportEmail() {
        try {
            LocalDate today = LocalDate.now();

            // FIX: get Map response instead of List
            Map<String, Object> response =
                    requirementsService.getInProgressRequirements(today, today, 0, Integer.MAX_VALUE, null, "IN");

            // Extract actual list
            List<InProgressRequirementDTO> requirements =
                    (List<InProgressRequirementDTO>) response.get("content");

            // Send for all recruiters
            String result = requirementsService.sendInProgressEmail(null, requirements);

            System.out.println("Email Sent Successfully: " + result);

        } catch (Exception e) {
            System.err.println("Error sending scheduled email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
