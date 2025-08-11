package com.dataquadinc.service;
import com.dataquadinc.dto.InProgressRequirementDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;


@Service
public class InProgressEmailScheduler {

    private final RequirementsService  requirementsService;

    public InProgressEmailScheduler(RequirementsService  requirementsService) {
        this.requirementsService = requirementsService;
    }

    @Scheduled(cron = "0 0 18 * * ?",zone = "Asia/Kolkata") 
    public void sendDailyInProgressReportEmail() {
        try {
            // 🔍 Get requirements from today
            LocalDate today = LocalDate.now();
            List<InProgressRequirementDTO> requirements = requirementsService.getInProgressRequirements(today, today);

            // ✅ Send for all recruiters (userId = null)
            String result = requirementsService.sendInProgressEmail(null, requirements);
            System.out.println("✅ Email Sent Successfully: " + result);

        } catch (Exception e) {
            System.err.println("❌ Error sending scheduled email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
