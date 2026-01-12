package com.dataquadinc.repository;



import com.dataquadinc.model.BDM_Client;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BDM_Repo extends JpaRepository<BDM_Client,String> {
    @Query("SELECT c FROM BDM_Client c ORDER BY c.id DESC LIMIT 1")
    Optional<BDM_Client> findTopByOrderByIdDesc();

    boolean existsByClientNameIgnoreCase(String clientName);

    @Query(value = """
        SELECT u.user_id AS userId,
               u.user_name AS userName,
               u.email,
               u.status,
               r.name AS roleName
        FROM user_details u
        JOIN user_roles ur ON ur.user_id = u.user_id
        JOIN roles r ON ur.role_id = r.id
        WHERE r.name = 'BDM' and u.status = 'ACTIVE' and u.designation <> 'testuser'
    """, nativeQuery = true)
    List<BdmEmployeeProjection> findAllBdmEmployees();


    @Query("SELECT c.clientName FROM BDM_Client c WHERE c.clientName = :clientName")
    List<String> findByClientName(String clientName);

    @Query("SELECT b FROM BDM_Client b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<BDM_Client> getClientsByCreatedAtRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);


    @Query(value = """
    SELECT 
        r.job_id AS job_id,
        r.job_title AS job_title,
        SUBSTRING_INDEX(r.client_name, '__', 1) AS client_name,
        r.job_description AS job_description,
        r.job_description_blob AS job_description_blob,
        r.job_type AS job_type,
        r.location AS location,
        r.job_mode AS job_mode,
        r.experience_required AS experience_required,
        r.notice_period AS notice_period,
        r.relevant_experience AS relevant_experience,
        r.qualification AS qualification,
        r.salary_package AS salary_package,
        r.no_of_positions AS no_of_positions,
        r.requirement_added_time_stamp AS requirement_added_time_stamp,
        GROUP_CONCAT(jr.recruiter_id) AS recruiter_id,
        r.status AS status,
        GROUP_CONCAT(u2.user_name) AS recruiter_name,
        r.assigned_by AS assigned_by
    FROM requirements_model r
    JOIN bdm_client b 
        ON TRIM(UPPER(SUBSTRING_INDEX(r.client_name, '__', 1))) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
    JOIN user_details u 
        ON b.on_boarded_by = u.user_name
    LEFT JOIN job_recruiters jr 
        ON jr.job_id = r.job_id
    LEFT JOIN user_details u2 
        ON jr.recruiter_id = u2.user_id
    WHERE u.user_id = :userId
      AND r.assigned_by = u.user_name
      AND r.requirement_added_time_stamp >= :startDateTime
      AND r.requirement_added_time_stamp <= :endDateTime
      AND r.status  IN ('Submitted','In Progress') OR r.status IN ('In Progress','Submitted')
    GROUP BY r.job_id
""", nativeQuery = true)
    List<Tuple> findRequirementsByBdmUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
    @Query(value = """
    SELECT 
        r.job_id AS job_id,
        r.job_title AS job_title,
        SUBSTRING_INDEX(r.client_name, '__', 1) AS client_name,
        r.job_description AS job_description,
        r.job_description_blob AS job_description_blob,
        r.job_type AS job_type,
        r.location AS location,
        r.job_mode AS job_mode,
        r.experience_required AS experience_required,
        r.notice_period AS notice_period,
        r.relevant_experience AS relevant_experience,
        r.qualification AS qualification,
        r.salary_package AS salary_package,
        r.no_of_positions AS no_of_positions,
        r.requirement_added_time_stamp AS requirement_added_time_stamp,
        GROUP_CONCAT(jr.recruiter_id) AS recruiter_id,
        r.status AS status,
        GROUP_CONCAT(u2.user_name) AS recruiter_name,
        r.assigned_by AS assigned_by
    FROM requirements_model r
    JOIN bdm_client b 
        ON TRIM(UPPER(SUBSTRING_INDEX(r.client_name, '__', 1))) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
    JOIN user_details u 
        ON b.on_boarded_by = u.user_name
    LEFT JOIN job_recruiters jr 
        ON jr.job_id = r.job_id
    LEFT JOIN user_details u2 
        ON jr.recruiter_id = u2.user_id
    WHERE u.user_id = :userId
      AND r.assigned_by = u.user_name
      AND r.status  IN ('Submitted','In Progress')
    GROUP BY r.job_id
""", nativeQuery = true)
    List<Tuple> findRequirementsByBdmUserId(
            @Param("userId") String userId
    );

    @Query("SELECT b FROM BDM_Client b")
    List<BDM_Client> getClients();

    @Query(value = """
    SELECT COUNT(*) 
    FROM candidate_submissions c 
    JOIN requirements_model r ON c.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    AND c.profile_received_date BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllSubmissionsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*) 
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE (b.client_name = :clientName OR r.client_name = :clientName 
           OR (:clientName IS NULL AND EXISTS (
                SELECT 1 FROM candidate_submissions cs2 
                WHERE cs2.job_id = r.job_id
           )))
    AND idt.interview_date_time IS NOT NULL
    AND CAST(idt.interview_date_time AS DATE) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllInterviewsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*)
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
    AND (
        (JSON_VALID(idt.interview_status) 
         AND JSON_SEARCH(idt.interview_status, 'one', 'Placed', NULL, '$[*].status') IS NOT NULL)
        OR UPPER(idt.interview_status) = 'PLACED'
    )
    AND CAST(idt.interview_date_time AS DATE) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    long countAllPlacementsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT COUNT(*) 
    FROM (
        SELECT DISTINCT r.job_id 
        FROM requirements_model r
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        WHERE TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(:clientName)) COLLATE utf8mb4_bin
        AND r.job_id IS NOT NULL
        AND CAST(r.requirement_added_time_stamp AS DATE) BETWEEN :startDate AND :endDate
    ) AS distinct_jobs
""", nativeQuery = true)
    long countRequirementsByClientNameAndDateRange(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query(value = """
                SELECT COUNT(*)
                FROM bdm_client bc
                JOIN user_details ud ON ud.user_name = bc.on_boarded_by
                WHERE ud.user_id = :userId
                  AND bc.status = 'ACTIVE'
                  AND DATE(bc.created_at) BETWEEN :startDate AND :endDate
            """, nativeQuery = true)
    long countClientsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT client_name 
    FROM bdm_client 
    WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId)
    AND status = 'ACTIVE'
    AND DATE(created_at) BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<String> findClientNamesByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = "SELECT COUNT(*) FROM requirements_model WHERE client_name = :clientName", nativeQuery = true)
    int countRequirementsByClientName(@Param("clientName") String clientName);

}
