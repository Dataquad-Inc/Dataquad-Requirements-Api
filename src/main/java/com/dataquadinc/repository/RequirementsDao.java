package com.dataquadinc.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.dataquadinc.dto.*;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dataquadinc.model.RequirementsModel;

@Repository
public interface RequirementsDao extends JpaRepository<RequirementsModel, String> {

    @Query(value = """
    SELECT u.user_name 
    FROM user_details u
    JOIN job_recruiters jr ON u.user_id = jr.recruiter_id
    WHERE jr.job_id = :jobId and u.status != 'inactive' and u.entity = 'IN'
""", nativeQuery = true)
    List<String> findRecruiterNamesByJobId(@Param("jobId") String jobId);

    @Query(value = """
    SELECT cs.*, cd.*, u.user_name AS recruiterName 
    FROM candidate_submissions cs
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN user_details u ON cd.user_email = u.email
    WHERE cs.job_id = :jobId and u.status != 'inactive' and u.entity = 'IN'
    """, nativeQuery = true)
    List<Tuple> findCandidatesByJobId(@Param("jobId") String jobId);

    @Query(value = """
    SELECT cs.*, 
           cd.*, 
           u.user_name AS recruiterName, 
           id.interview_date_time AS interviewDateTime,
           id.full_name AS candidateName,      -- Candidate name from interview_details table
           id.candidate_email_id AS email,     -- Candidate email from interview_details table
           id.interview_level AS interviewLevel -- Interview level from interview_details table
    FROM candidate_submissions cs
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN user_details u ON cd.user_email = u.email
    JOIN interview_details id ON cs.candidate_id = id.candidate_id
    WHERE cs.job_id = :jobId and u.status != 'inactive' and u.entity = 'IN'
      AND id.interview_date_time IS NOT NULL
      AND cs.job_id = id.job_id           -- Ensure job_id is present in both tables
""", nativeQuery = true)
    List<Tuple> findInterviewScheduledCandidatesByJobId(@Param("jobId") String jobId);

    @Query(value = "SELECT email, user_name FROM user_details WHERE user_id = :userId AND status != 'inactive' and entity = 'IN'", nativeQuery = true)
    Tuple findUserEmailAndUsernameByUserId(@Param("userId") String userId);

    @Query(value = "SELECT email, user_name FROM user_details WHERE LOWER(TRIM(user_name)) = LOWER(TRIM(:assignedBy)) AND status != 'inactive' and entity = 'IN'", nativeQuery = true)
    Tuple findUserEmailAndUsernameByAssignedBy(@Param("assignedBy") String assignedBy);


    @Query(value = """
            SELECT u.user_id, u.user_name, r.name AS role_name, u.email, 
                   u.designation, u.joining_date, u.gender, u.dob, 
                   u.phone_number, u.personalemail, u.status, b.client_name 
            FROM user_details u 
            LEFT JOIN user_roles ur ON u.user_id = ur.user_id 
            LEFT JOIN roles r ON ur.role_id = r.id
            LEFT JOIN bdm_client b ON u.user_id = b.on_boarded_by
            WHERE r.name = 'BDM' AND u.user_id = :userId and u.status != 'inactive' and u.entity = 'IN'
            """, nativeQuery = true)
    List<Tuple> findBdmEmployeeByUserId(@Param("userId") String userId);

    @Query(value = """
        SELECT id, client_name, on_boarded_by, client_address, 
               JSON_UNQUOTE(JSON_EXTRACT(client_spoc_name, '$')) AS client_spoc_name,
               JSON_UNQUOTE(JSON_EXTRACT(client_spoc_emailid, '$')) AS client_spoc_emailid,
               JSON_UNQUOTE(JSON_EXTRACT(client_spoc_mobile_number, '$')) AS client_spoc_mobile_number
        FROM bdm_client 
        WHERE on_boarded_by = (SELECT user_name FROM user_details WHERE user_id = :userId and status != 'inactive' and entity = 'IN')
          AND DATE(created_at) BETWEEN :startDate AND :endDate 
        """, nativeQuery = true)
    List<Tuple> findClientsByBdmUserIdAndCreatedAtBetween(@Param("userId") String userId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);


    @Query(value = """
    SELECT 
        r.job_id,
        MAX(TRIM(r.client_name)) AS client_name,
        MAX(TRIM(r.job_title)) AS job_title,
        MAX(TRIM(r.assigned_by)) AS assigned_by,
        MAX(TRIM(r.location)) AS location,
        MAX(TRIM(r.notice_period)) AS notice_period,
        GROUP_CONCAT(DISTINCT u.user_name) AS recruiter_name -- All recruiters under this job
    FROM requirements_model r
    JOIN job_recruiters jr 
        ON r.job_id = jr.job_id
    JOIN user_details u 
        ON jr.recruiter_id = u.user_id
    JOIN bdm_client b 
        ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
    WHERE TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(:clientName)) COLLATE utf8mb4_bin
      AND r.job_id IS NOT NULL AND u.status != 'inactive' and u.entity = 'IN'
    GROUP BY r.job_id, r.client_name
    """, nativeQuery = true)
    List<Tuple> findRequirementsByClientName(@Param("clientName") String clientName);


    @Query(value = """
        SELECT DISTINCT u.user_name AS recruiter_name, 
               r.client_name, 
               r.job_id, 
               r.job_title, 
               r.assigned_by, 
               r.location, 
               r.notice_period
        FROM requirements_model r
        JOIN job_recruiters jr 
            ON r.job_id = jr.job_id
        JOIN user_details u 
            ON jr.recruiter_id = u.user_id
        JOIN bdm_client b 
            ON TRIM(UPPER(r.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin
        WHERE TRIM(UPPER(b.client_name)) COLLATE utf8mb4_bin = TRIM(UPPER(:clientName)) COLLATE utf8mb4_bin
          AND r.job_id IS NOT NULL
          AND DATE(r.requirement_added_time_stamp) BETWEEN :startDate AND :endDate
          AND u.status != 'inactive' and u.entity = 'IN'
        """, nativeQuery = true)
    List<Tuple> findRequirementsByClientNameDateFilter(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT DISTINCT 
        cd.candidate_id, 
        cd.full_name, 
        cd.candidate_email_id AS candidateEmailId, 
        cd.contact_number, 
        cd.qualification, 
        cs.skills, 
        cs.overall_feedback, 
        cs.user_id,
        r.job_id, 
        r.job_title, 
        b.client_name
    FROM candidate_submissions cs
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE LOWER(b.client_name) LIKE LOWER(CONCAT('%', :clientName, '%'))
      AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    List<Tuple> findAllSubmissionsByClientNameAndSubmittedAtBetween(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query(value = """
    SELECT 
        cd.candidate_id, 
        cd.full_name, 
        cd.candidate_email_id AS candidateEmailId, 
        cd.contact_number, 
        cd.qualification, 
        cs.skills, 
        CASE 
            WHEN JSON_VALID(li.interview_status) 
            THEN JSON_UNQUOTE(JSON_EXTRACT(li.interview_status, '$[0].status')) 
            ELSE li.interview_status 
        END AS interview_status, 
        li.interview_level, 
        li.interview_date_time, 
        r.job_id, 
        r.job_title, 
        b.client_name
    FROM (
        SELECT 
            idt.candidate_id,
            idt.interview_status,
            idt.interview_level,
            idt.interview_date_time,
            cs.job_id,
            ROW_NUMBER() OVER (PARTITION BY idt.candidate_id ORDER BY idt.interview_date_time DESC) AS rn 
        FROM interview_details idt
        JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    ) li
    JOIN candidate_submissions cs ON li.job_id = cs.job_id
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE (b.client_name = :clientName OR r.client_name = :clientName 
           OR (:clientName IS NULL AND EXISTS (
                SELECT 1 FROM candidate_submissions cs2 
                WHERE cs2.job_id = r.job_id
           )) )
    AND li.rn = 1
    AND b.client_name IS NOT NULL 
    AND li.interview_date_time IS NOT NULL
    """, nativeQuery = true)
    List<Tuple> findAllInterviewsByClientName(@Param("clientName") String clientName);

    @Query(value = """
    SELECT 
        cd.candidate_id, 
        cd.full_name, 
        cd.candidate_email_id AS candidateEmailId, 
        cd.contact_number, 
        cd.qualification, 
        cs.skills, 
        CASE 
            WHEN JSON_VALID(idt.interview_status) 
            THEN JSON_UNQUOTE(JSON_EXTRACT(idt.interview_status, '$[0].status')) 
            ELSE idt.interview_status 
        END AS interview_status, 
        idt.interview_level, 
        idt.interview_date_time, 
        r.job_id, 
        r.job_title, 
        b.client_name
    FROM interview_details idt
    JOIN (
        SELECT 
            candidate_id, 
            MAX(interview_date_time) AS latest_interview_time
        FROM interview_details
        GROUP BY candidate_id
    ) latest ON latest.candidate_id = idt.candidate_id 
             AND latest.latest_interview_time = idt.interview_date_time
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE (b.client_name = :clientName OR r.client_name = :clientName 
           OR (:clientName IS NULL AND EXISTS (
                SELECT 1 FROM candidate_submissions cs2 
                WHERE cs2.job_id = r.job_id
           )))
      AND b.client_name IS NOT NULL 
      AND idt.interview_date_time IS NOT NULL
      AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    List<Tuple> findAllInterviewsByClientNameDateFilter(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
        SELECT 
            cd.candidate_id, 
            cd.full_name, 
            cd.candidate_email_id AS candidateEmailId,  
            r.job_id, 
            r.job_title, 
            b.client_name
        FROM interview_details idt
        JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
        JOIN candidates cd ON cs.candidate_id = cd.candidate_id
        JOIN requirements_model r ON cs.job_id = r.job_id
        JOIN bdm_client b ON r.client_name = b.client_name
        WHERE b.client_name = :clientName
          AND (
            (JSON_VALID(idt.interview_status) 
             AND JSON_SEARCH(idt.interview_status, 'one', 'PLACED', NULL, '$[*].status') IS NOT NULL)
            OR UPPER(idt.interview_status) = 'PLACED'
          )
        """, nativeQuery = true)
    List<Tuple> findAllPlacementsByClientName(@Param("clientName") String clientName);

    @Query(value = """
    SELECT 
        cd.candidate_id, 
        cd.full_name, 
        cd.candidate_email_id AS candidateEmailId,  
        r.job_id, 
        r.job_title, 
        b.client_name
    FROM interview_details idt
    JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN bdm_client b ON r.client_name = b.client_name
    WHERE b.client_name = :clientName
      AND (
        (JSON_VALID(idt.interview_status) 
         AND JSON_SEARCH(idt.interview_status, 'one', 'PLACED', NULL, '$[*].status') IS NOT NULL)
        OR UPPER(idt.interview_status) = 'PLACED'
      )
      AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    List<Tuple> findAllPlacementsByClientNameDateFilter(
            @Param("clientName") String clientName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        cs.overall_feedback AS overallFeedback,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        r.client_name AS clientName
    FROM interview_details idt
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE idt.user_id = :userId
""", nativeQuery = true)
    List<SubmittedCandidateDTO> findSubmittedCandidatesByUserId(@Param("userId") String userId);

    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        CASE 
            WHEN JSON_VALID(idt.interview_status) = 1 
              AND JSON_LENGTH(idt.interview_status) > 0
            THEN JSON_UNQUOTE(
                JSON_EXTRACT(
                    idt.interview_status,
                    CONCAT(
                        '$[',
                        CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                        '].status'
                    )
                )
            )
            ELSE idt.interview_status
        END AS interviewStatus,
        idt.interview_level AS interviewLevel,
        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName
    FROM interview_details idt
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE idt.user_id = :userId
      AND idt.interview_date_time IS NOT NULL
      AND idt.client_name IS NOT NULL
""", nativeQuery = true)
    List<InterviewScheduledDTO> findScheduledInterviewsByUserId(@Param("userId") String userId);


    @Query(value = """
    SELECT 
        r.job_id AS jobId,
        TRIM(r.job_title) AS jobTitle,
        TRIM(r.client_name) AS clientName,
        TRIM(BOTH '\\"' FROM r.assigned_by) AS assignedBy,
        r.status AS status,
        r.no_of_positions AS noOfPositions,
        r.qualification AS qualification,
        r.job_type AS jobType,
        r.job_mode AS jobMode,
        r.requirement_added_time_stamp AS postedDate
    FROM requirements_model r
    JOIN job_recruiters jr ON r.job_id = jr.job_id
    JOIN user_details u ON jr.recruiter_id = u.user_id
    WHERE u.user_id = :userId
""", nativeQuery = true)
    List<JobDetailsDTO> findJobDetailsByUserId(@Param("userId") String userId);

    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        CASE 
            WHEN JSON_VALID(idt.interview_status) 
            THEN JSON_UNQUOTE(JSON_EXTRACT(idt.interview_status, '$[0].status')) 
            ELSE idt.interview_status 
        END AS interviewStatus,
        idt.interview_level AS interviewLevel,
        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName
    FROM interview_details idt
    JOIN user_details u ON idt.user_id = u.user_id
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE u.user_id = :userId
      AND (
          (JSON_VALID(idt.interview_status) 
           AND JSON_UNQUOTE(JSON_EXTRACT(idt.interview_status, '$[0].status')) = 'PLACED')
          OR UPPER(idt.interview_status) = 'PLACED'
      )
""", nativeQuery = true)
    List<PlacementDetailsDTO> findPlacementCandidatesByUserId(@Param("userId") String userId);

    @Query(value = """
    SELECT 
        u.user_id AS employeeId,
        u.user_name AS employeeName,
        u.email AS employeeEmail,
        COUNT(DISTINCT CASE
            WHEN latest_status.interviewLevel = 'INTERNAL' THEN idt.interview_id
            ELSE NULL
        END) AS totalInterviews,            
            
        SUM(CASE 
            WHEN latest_status.status = 'SELECTED' AND latest_status.interviewLevel = 'INTERNAL' 
            THEN 1 ELSE 0 
        END) AS selectedInterviewsCount,

        SUM(CASE 
            WHEN latest_status.status = 'REJECTED' AND latest_status.interviewLevel = 'INTERNAL' 
            THEN 1 ELSE 0 
        END) AS rejectedInterviewsCount,

        SUM(CASE 
            WHEN latest_status.status = 'SCHEDULED' AND latest_status.interviewLevel = 'INTERNAL' 
            THEN 1 ELSE 0 
        END) AS scheduledInterviewsCount

    FROM user_details u
    JOIN interview_details idt ON idt.assigned_to = u.user_id

    -- Latest JSON object only
    JOIN JSON_TABLE(
        JSON_EXTRACT(
            idt.interview_status,
            CONCAT('$[', JSON_LENGTH(idt.interview_status) - 1, ']')
        ),
        '$' COLUMNS (
            status VARCHAR(50) PATH '$.status',
            interviewLevel VARCHAR(50) PATH '$.interviewLevel'
        )
    ) AS latest_status ON TRUE
    WHERE u.status != 'inactive' and u.entity = 'IN'
    GROUP BY u.user_id, u.user_name, u.email
    """, nativeQuery = true)
    List<Tuple> countInterviewsByStatus();


    @Query(value = """
                SELECT 
                    u.user_id AS employeeId,
                    u.user_name AS employeeName,                
                    r.name AS role,
                    u.email AS employeeEmail,
                    u.designation AS designation,
                    DATE_FORMAT(u.joining_date, '%Y-%m-%d') AS joiningDate, 
                    u.gender AS gender, 
                    DATE_FORMAT(u.dob, '%Y-%m-%d') AS dob, 
                    u.phone_number AS phoneNumber, 
                    u.personalemail AS personalEmail, 
                    u.status AS status
                FROM user_details u
                JOIN user_roles ur ON u.user_id = ur.user_id
                JOIN roles r ON ur.role_id = r.id
                WHERE r.name IN ('Employee', 'Teamlead')
                AND u.user_id = :userId and u.status != 'inactive' and u.entity = 'IN'
            """, nativeQuery = true)
    List<Tuple> getEmployeeDetailsByUserId(@Param("userId") String userId);

    @Query(value = """
            SELECT COUNT(DISTINCT cs.candidate_id)
            FROM candidate_submissions cs
            JOIN requirements_model req ON cs.job_id = req.job_id
            WHERE cs.job_id = :jobId
            """, nativeQuery = true)
    Integer getNumberOfSubmissionsByJobId(@Param("jobId") String jobId);


    @Query(value = """
            SELECT COALESCE(SUM(CASE 
                                  WHEN idt.interview_date_time IS NOT NULL 
                                  THEN 1 ELSE 0 
                               END), 0)
            FROM interview_details idt
            JOIN requirements_model req ON idt.job_id = req.job_id
            WHERE req.job_id = :jobId
            """, nativeQuery = true)
    Integer getNumberOfInterviewsByJobId(@Param("jobId") String jobId);


    // RequirementsDao.java
    @Query("SELECT r FROM RequirementsModel r WHERE DATE(r.requirementAddedTimeStamp) BETWEEN :startDate AND :endDate")
    List<RequirementsModel> findByRequirementAddedTimeStampBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT r FROM RequirementsModel r WHERE r.status IN ('Submitted','In Progress')")
    List<RequirementsModel> findByRequirementAdded();

    @Query("""
            SELECT r FROM RequirementsModel r
            WHERE r.status IN ('Submitted','In Progress')
            
            AND (
                (:startDate IS NULL OR :endDate IS NULL)
                OR DATE(r.requirementAddedTimeStamp) BETWEEN :startDate AND :endDate
            )
            
            AND (
                :search IS NULL OR :search = '' OR
                LOWER(r.jobId) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.jobType) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.status) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.assignedBy) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            
            ORDER BY r.requirementAddedTimeStamp DESC
            """)
    Page<RequirementsModel> findByRequirementAddedWithSearchPageable(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT r FROM RequirementsModel r
            WHERE
            
            (
                (:startDate IS NULL OR :endDate IS NULL)
                OR DATE(r.requirementAddedTimeStamp) BETWEEN :startDate AND :endDate
            )
            
            AND (
                :search IS NULL OR :search = '' OR
                LOWER(r.jobId) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.jobTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.clientName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.location) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.jobType) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.status) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(r.assignedBy) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            
            ORDER BY r.requirementAddedTimeStamp DESC
            """)
    Page<RequirementsModel> findByRequirementAddedWithSearchPageableWithDateFilter(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("search") String search,
            Pageable pageable);



    @Query(value = """
    SELECT 
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        r.client_name AS clientName,
        TRIM(BOTH '\"' FROM r.assigned_by) AS assignedBy,
        r.status AS status,
        r.no_of_positions AS noOfPositions,
        r.qualification AS qualification,
        r.job_type AS jobType,
        r.job_mode AS jobMode,
        r.requirement_added_time_stamp AS postedDate
    FROM requirements_model r
    WHERE TRIM(BOTH '\"' FROM r.assigned_by) = :username
""", nativeQuery = true)
    List<JobDetailsDTO> findJobDetailsByAssignedBy(@Param("username") String username);

    @Query(value = """
    SELECT DISTINCT
        b.id AS clientId,
        b.client_name AS clientName,
        b.client_address AS clientAddress,
        b.on_boarded_by AS onBoardedBy,
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_name, '$')), '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"' , '') AS clientSpocName,
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_mobile_number, '$')), '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"' , '') AS clientSpocMobileNumber
    FROM bdm_client b
    JOIN requirements_model r ON LOWER(b.client_name) = LOWER(r.client_name)
    WHERE REPLACE(REPLACE(r.assigned_by, '\"', ''), '"', '') = REPLACE(REPLACE(:username, '\"', ''), '"', '')
""", nativeQuery = true)
    List<ClientDetailsDTO> findClientDetailsByAssignedBy(@Param("username") String username);


    @Query(value = """
    SELECT 
        u.user_id AS employeeId,
        u.user_name AS employeeName,                
        r.name AS role,
        u.email AS employeeEmail,
        u.designation AS designation,
        DATE_FORMAT(u.joining_date, '%Y-%m-%d') AS joiningDate, 
        u.gender AS gender, 
        DATE_FORMAT(u.dob, '%Y-%m-%d') AS dob, 
        u.phone_number AS phoneNumber, 
        u.personalemail AS personalEmail, 
        u.status AS status
    FROM user_details u
    JOIN user_roles ur ON u.user_id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE r.name = 'Teamlead'
    AND u.user_id = :userId
    """, nativeQuery = true)
    List<Tuple> getTeamleadDetailsByUserId(@Param("userId") String userId);


    @Query(value = """
    SELECT 
        u.user_id AS userId,
        u.user_name AS userName,
        r.name AS role
    FROM user_details u
    JOIN user_roles ur ON u.user_id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE u.user_id = :userId and u.status != 'INACTIVE' and u.entity = 'IN'
""", nativeQuery = true)
    Tuple getUserRoleAndUsername(@Param("userId") String userId);


    @Query("SELECT r FROM RequirementsModel r " +
            "WHERE :recruiterId MEMBER OF r.recruiterIds " +
            "AND r.requirementAddedTimeStamp BETWEEN :startDate AND :endDate")
    List<RequirementsModel> findJobsByRecruiterIdAndDateRange(@Param("recruiterId") String recruiterId,
                                                              @Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM RequirementsModel r " +
            "WHERE :recruiterId MEMBER OF r.recruiterIds " +
            "AND (" +
            "   (r.requirementAddedTimeStamp BETWEEN :startDate AND :endDate AND r.status IN ('Submitted', 'In Progress')) " +
            "   OR " +
            "   (r.requirementAddedTimeStamp NOT BETWEEN :startDate AND :endDate AND r.status IN ('Submitted', 'In Progress'))" +
            ")")
    List<RequirementsModel> findJobsByRecruiterId(
            @Param("recruiterId") String recruiterId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    @Query(value = "SELECT COUNT(*) FROM user_details WHERE user_id = :userId AND status != 'INACTIVE' AND entity = 'IN'", nativeQuery = true)
    int countByUserId(@Param("userId") String userId);


    // Native query to validate if the username exists in user_details_prod
    @Query(value = "SELECT user_name FROM user_details WHERE user_id = :userId AND status != 'INACTIVE' AND entity = 'IN'", nativeQuery = true)
    String findUserNameByUserId(@Param("userId") String userId);

    @Query(value = "SELECT email FROM user_details WHERE LOWER(designation) = LOWER(:designation) AND status != 'INACTIVE' AND entity = 'IN'", nativeQuery = true)
    List<String> findEmailsByDesignationIgnoreCase(@Param("designation") String designation);




    @Query(value = "SELECT * FROM requirements_model " +
            "WHERE assigned_by = :assignedBy " +
            "AND requirement_added_time_stamp BETWEEN :startDate AND :endDate ", nativeQuery = true)
    List<RequirementsModel> findJobsAssignedByNameAndDateRange(
            @Param("assignedBy") String assignedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    @Query("SELECT r FROM RequirementsModel r " +
            "WHERE r.assignedBy = :assignedBy " +
            "AND (" +
            "   (r.requirementAddedTimeStamp BETWEEN :startDate AND :endDate AND r.status IN ('Submitted', 'In Progress')) " +
            "   OR " +
            "   (r.requirementAddedTimeStamp NOT BETWEEN :startDate AND :endDate AND r.status IN ('In Progress', 'Submitted'))" +
            ")")
    List<RequirementsModel> findJobsAssignedByName(
            @Param("assignedBy") String assignedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );



    @Query(value = """
    SELECT 
        cd.candidate_id, 
        id.full_name AS candidateName, 
        id.candidate_email_id AS email, 
        id.contact_number AS contactNumber, 
        cd.qualification AS qualification,
        cs.overall_feedback AS overallFeedback,
        u.user_name AS recruiterName
    FROM candidate_submissions cs
    JOIN candidates cd ON cs.candidate_id = cd.candidate_id
    JOIN user_details u ON cd.user_email = u.email
    JOIN interview_details id ON cs.candidate_id = id.candidate_id
    WHERE cs.job_id = :jobId
      AND id.is_placed = true
      AND cs.job_id = id.job_id
      AND u.status != 'inactive' and u.entity = 'IN'
""", nativeQuery = true)
    List<Tuple> findPlacementsByJobId(@Param("jobId") String jobId);

    Optional<RequirementsModel> findByJobId(String jobId);


    // Teamlead candidate statistics
    @Query(value = """
        SELECT 
            u.user_id AS employeeId,
            u.user_name AS employeeName,
            u.email AS employeeEmail,
            'TEAMLEAD' AS role,

            COALESCE((
                    SELECT COUNT(DISTINCT b.id)
                    FROM requirements_model r2\s
                    JOIN bdm_client b ON r2.client_name = b.client_name
                    WHERE (
                        REPLACE(REPLACE(r2.assigned_by, '\\"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '\\"', ''), '"', '')
                        OR EXISTS (
                            SELECT 1\s
                            FROM production.job_recruiters jr
                            WHERE jr.job_id = r2.job_id
                              AND jr.recruiter_id = u.user_id
                        )
                    )
                    AND b.client_name = r2.client_name
                    AND EXISTS (
                        SELECT 1
                        FROM candidate_submissions cs
                        WHERE cs.job_id = r2.job_id
                          AND cs.user_id = u.user_id
                          AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                    )
                ), 0) AS numberOfClients,

            
            COALESCE((
                SELECT COUNT(DISTINCT r2.job_id)
                FROM requirements_model r2
                WHERE (
                    REPLACE(REPLACE(r2.assigned_by, '\\"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '\\"', ''), '"', '')
                    OR EXISTS (
                        SELECT 1\s
                        FROM production.job_recruiters jr
                        WHERE jr.job_id = r2.job_id
                          AND jr.recruiter_id = u.user_id
                    )
                )
                AND EXISTS (
                    SELECT 1
                    FROM candidate_submissions cs
                    WHERE cs.job_id = r2.job_id
                      AND cs.user_id = u.user_id
                      AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                )
            ), 0) AS numberOfRequirements,

            COALESCE((
                SELECT COUNT(*)
                FROM candidates cd
                JOIN candidate_submissions cs ON cd.candidate_id = cs.candidate_id
                WHERE cs.user_id = u.user_id
                AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
            ), 0) AS selfSubmissions,

            COALESCE((
                SELECT COUNT(DISTINCT idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                WHERE idt.user_id = u.user_id
                  AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
                  AND cs.job_id IN (
                      SELECT job_id FROM requirements_model r2
                  )
                  AND JSON_VALID(idt.interview_status) = 1
                  AND JSON_LENGTH(idt.interview_status) > 0
                  AND NOT (
                      JSON_UNQUOTE(
                          JSON_EXTRACT(
                              idt.interview_status,
                              CONCAT(
                                  '$[',
                                  CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                  '].status'
                              )
                          )
                      ) = 'REJECTED'
                      AND JSON_UNQUOTE(
                          JSON_EXTRACT(
                              idt.interview_status,
                              CONCAT(
                                  '$[',
                                  CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                  '].interviewLevel'
                              )
                          )
                      ) = 'INTERNAL'
                  )
            ), 0) AS selfInterviews,

            COALESCE((
                SELECT COUNT(DISTINCT idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                WHERE cs.user_id = u.user_id
                AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
                AND (
                    idt.interview_status = 'PLACED'
                    OR (
                        JSON_VALID(idt.interview_status)
                        AND JSON_UNQUOTE(JSON_EXTRACT(
                            idt.interview_status,
                            CONCAT('$[', JSON_LENGTH(idt.interview_status)-1, '].status')
                        )) = 'PLACED'
                    )
                )
            ), 0) AS selfPlacements,

            COALESCE((
                SELECT COUNT(*)
                FROM candidate_submissions cs
                JOIN requirements_model r2 ON cs.job_id = r2.job_id
                WHERE REPLACE(REPLACE(r2.assigned_by, '\"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '\"', ''), '"', '')
                AND DATE(cs.profile_received_date) BETWEEN :startDate AND :endDate
                AND cs.candidate_id IN (
                    SELECT candidate_id FROM candidates 
                    WHERE user_id != u.user_id
                )
            ), 0) AS teamSubmissions,

            COALESCE((
                SELECT COUNT(DISTINCT cs.submission_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN requirements_model r2 ON cs.job_id = r2.job_id
                WHERE 
                    (
                        cs.user_id = u.user_id
                        OR REPLACE(REPLACE(r2.assigned_by, '\\"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '\\"', ''), '"', '')
                        OR EXISTS (
                            SELECT 1
                            FROM production.job_recruiters jr
                            WHERE jr.job_id = r2.job_id
                              AND jr.recruiter_id = u.user_id
                        )
                    )
                AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
                AND idt.interview_level = 'Internal'
                AND (
                    idt.interview_status = 'REJECTED'
                    OR (
                        JSON_VALID(idt.interview_status)
                        AND JSON_UNQUOTE(
                            JSON_EXTRACT(
                                idt.interview_status,
                                CONCAT('$[', JSON_LENGTH(idt.interview_status) - 1, '].status')
                            )
                        ) = 'REJECTED'
                    )
                )
            ), 0) AS teamScreenRejectCount,            

            COALESCE((
                SELECT COUNT(DISTINCT idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                WHERE idt.user_id != u.user_id
                  AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
                  AND cs.job_id IN (
                      SELECT job_id FROM requirements_model r2
                      WHERE REPLACE(REPLACE(r2.assigned_by, '"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '"', ''), '"', '')
                  )
                  AND JSON_VALID(idt.interview_status) = 1
                  AND JSON_LENGTH(idt.interview_status) > 0
                  AND NOT (
                      JSON_UNQUOTE(
                          JSON_EXTRACT(
                              idt.interview_status,
                              CONCAT(
                                  '$[',
                                  CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                  '].status'
                              )
                          )
                      ) = 'REJECTED'
                      AND JSON_UNQUOTE(
                          JSON_EXTRACT(
                              idt.interview_status,
                              CONCAT(
                                  '$[',
                                  CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                  '].interviewLevel'
                              )
                          )
                      ) = 'INTERNAL'
                  )
            ), 0) AS teamInterviews,

            COALESCE((
                SELECT COUNT(DISTINCT idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                JOIN requirements_model r2 ON cs.job_id = r2.job_id
                WHERE REPLACE(REPLACE(r2.assigned_by, '\"', ''), '"', '') = REPLACE(REPLACE(u.user_name, '\"', ''), '"', '')
                AND cs.user_id != u.user_id
                AND DATE(idt.timestamp) BETWEEN :startDate AND :endDate
                AND (
                    idt.interview_status = 'PLACED'
                    OR (
                        JSON_VALID(idt.interview_status)
                        AND JSON_UNQUOTE(
                            JSON_EXTRACT(
                                idt.interview_status,
                                CONCAT('$[', JSON_LENGTH(idt.interview_status) - 1, '].status')
                            )
                        ) = 'PLACED'
                    )
                )
            ), 0) AS teamPlacements

        FROM user_details u
        WHERE EXISTS (
            SELECT 1 FROM user_roles ur 
            JOIN roles rl ON ur.role_id = rl.id 
            WHERE ur.user_id = u.user_id 
              AND rl.name = 'Teamlead' 
              AND u.status != 'inactive' 
              AND u.entity = 'IN'
        )
        GROUP BY u.user_id, u.user_name, u.email
        """, nativeQuery = true)
    List<Tuple> getTeamleadCandidateStats(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query(value = """
        SELECT 
            u.user_id AS employeeId,
            u.user_name AS employeeName,
            u.email AS employeeEmail,
            r.name AS role,

            COALESCE((
                    SELECT COUNT(DISTINCT cs.submission_id)
                    FROM candidates cd
                    JOIN candidate_submissions cs ON cd.candidate_id = cs.candidate_id
                    LEFT JOIN interview_details idt ON idt.candidate_id = cd.candidate_id AND idt.job_id = cs.job_id
                    WHERE cs.user_id = u.user_id
                      AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                ), 0) AS numberOfSubmissions,
            

            COALESCE((
                SELECT COUNT(DISTINCT cs.submission_id)
                FROM candidates cd
                JOIN candidate_submissions cs ON cd.candidate_id = cs.candidate_id
                LEFT JOIN interview_details idt ON idt.candidate_id = cd.candidate_id AND idt.job_id = cs.job_id
                WHERE cs.user_id = u.user_id
                  AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                  AND (
                      idt.interview_date_time IS NOT NULL
                      AND idt.interview_level = 'Internal'
                      AND (
                          idt.interview_status = 'REJECTED'
                          OR (
                              JSON_VALID(idt.interview_status)
                              AND JSON_UNQUOTE(
                                  JSON_EXTRACT(
                                      idt.interview_status,
                                      CONCAT('$[', JSON_LENGTH(idt.interview_status) - 1, '].status')
                                  )
                              ) = 'REJECTED'
                          )
                      )
                  )
            ), 0) AS numberOfScreenRejects,

            COALESCE((
                SELECT COUNT(idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                WHERE idt.user_id = u.user_id
                  AND idt.interview_date_time IS NOT NULL
                  AND DATE(idt.interview_date_time) BETWEEN :startDate AND :endDate
            ), 0) AS numberOfInterviews,
            
            COALESCE((
                SELECT COUNT(DISTINCT idt.interview_id)
                FROM interview_details idt
                JOIN candidate_submissions cs ON idt.candidate_id = cs.candidate_id
                JOIN candidates cd ON cs.candidate_id = cd.candidate_id
                WHERE cs.user_id = u.user_id
                  AND idt.interview_date_time IS NOT NULL
                  AND (
                      idt.interview_status = 'PLACED'
                      OR (
                          JSON_VALID(idt.interview_status)
                          AND JSON_UNQUOTE(
                              JSON_EXTRACT(
                                  idt.interview_status,
                                  CONCAT('$[', JSON_LENGTH(idt.interview_status) - 1, '].status')
                              )
                          ) = 'PLACED'
                      )
                  )
                  AND DATE(idt.timestamp) BETWEEN :startDate AND :endDate
            ), 0) AS numberOfPlacements,
            
            COALESCE((
                    SELECT COUNT(DISTINCT b.client_name)
                    FROM requirements_model req
                    JOIN job_recruiters jrp ON req.job_id = jrp.job_id
                    JOIN bdm_client b ON LOWER(b.client_name) = LOWER(req.client_name)
                    WHERE jrp.recruiter_id = u.user_id
                      AND EXISTS (
                          SELECT 1 FROM candidate_submissions cs
                          WHERE cs.job_id = req.job_id
                            AND cs.user_id = u.user_id
                            AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                      )
                ), 0) AS numberOfClients,
            
            
            COALESCE((
                SELECT COUNT(DISTINCT req.job_id) 
                FROM requirements_model req 
                JOIN job_recruiters jrp ON req.job_id = jrp.job_id
                WHERE jrp.recruiter_id = u.user_id
                  AND EXISTS (
                      SELECT 1 FROM candidate_submissions cs
                      WHERE cs.job_id = req.job_id
                        AND cs.user_id = u.user_id
                        AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
                  )
            ), 0) AS numberOfRequirements
           
        FROM user_details u
        JOIN user_roles ur ON u.user_id = ur.user_id
        JOIN roles r ON ur.role_id = r.id
        WHERE r.name = 'Employee'
          AND u.status != 'INACTIVE' 
          AND u.entity = 'IN'
        """, nativeQuery = true)
    List<Tuple> getEmployeeCandidateStats(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        cs.overall_feedback AS overallFeedback,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        r.client_name AS clientName
    FROM candidate_submissions cs 
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE cs.user_id = :userId
      AND cs.submitted_at BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<SubmittedCandidateDTO> findSubmittedCandidatesByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        CASE 
            WHEN JSON_VALID(idt.interview_status) = 1 
              AND JSON_LENGTH(idt.interview_status) > 0
            THEN JSON_UNQUOTE(
                JSON_EXTRACT(
                    idt.interview_status,
                    CONCAT(
                        '$[',
                        CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                        '].status'
                    )
                )
            )
            ELSE NULL
        END AS interviewStatus,
        CASE 
            WHEN JSON_VALID(idt.interview_status) = 1 
              AND JSON_LENGTH(idt.interview_status) > 0
            THEN JSON_UNQUOTE(
                JSON_EXTRACT(
                    idt.interview_status,
                    CONCAT(
                        '$[',
                        CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                        '].interviewLevel'
                    )
                )
            )
            ELSE NULL
        END AS interviewLevel,
        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName
    FROM interview_details idt
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE idt.user_id = :userId
      AND idt.interview_date_time IS NOT NULL
      AND idt.client_name IS NOT NULL
      AND idt.interview_date_time BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<InterviewScheduledDTO> findScheduledInterviewsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query(value = """
    SELECT 
        r.job_id AS jobId,
        TRIM(r.job_title) AS jobTitle,
        TRIM(r.client_name) AS clientName,
        TRIM(BOTH '\\"' FROM r.assigned_by) AS assignedBy,
        r.status AS status,
        r.no_of_positions AS noOfPositions,
        r.qualification AS qualification,
        r.job_type AS jobType,
        r.job_mode AS jobMode,
        r.requirement_added_time_stamp AS postedDate
    FROM requirements_model r
    JOIN job_recruiters jr ON r.job_id = jr.job_id
    JOIN user_details u ON jr.recruiter_id = u.user_id
    WHERE u.user_id = :userId AND u.status != 'INACTIVE' AND u.entity = 'IN'
      AND EXISTS (
          SELECT 1
          FROM candidate_submissions cs
          WHERE cs.job_id = r.job_id
            AND cs.user_id = u.user_id
            AND cs.submitted_at BETWEEN :startDate AND :endDate
      )
""", nativeQuery = true)
    List<JobDetailsDTO> findJobDetailsByUserIdAndDateRange(@Param("userId") String userId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);


    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        CASE\s
                        WHEN JSON_VALID(idt.interview_status) = 1\s
                             AND JSON_LENGTH(idt.interview_status) > 0
                        THEN JSON_UNQUOTE(
                            JSON_EXTRACT(
                                idt.interview_status,
                                CONCAT(
                                    '$[',
                                    CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                    '].status'
                                )
                            )
                        )
                        ELSE idt.interview_status\s
                    END AS interviewStatus,
        idt.interview_level AS interviewLevel,
        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName
    FROM interview_details idt
    JOIN user_details u ON idt.user_id = u.user_id
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE u.user_id = :userId
      AND (
                        (JSON_VALID(idt.interview_status)\s
                         AND JSON_UNQUOTE(JSON_EXTRACT(
                              idt.interview_status,
                              CONCAT(
                                '$[',
                                CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                '].status'
                              )
                         )) = 'PLACED')
                        OR UPPER(idt.interview_status) = 'PLACED'
                    )
      AND idt.timestamp BETWEEN :startDate AND :endDate
      AND u.status != 'inactive' and u.entity = 'IN'
""", nativeQuery = true)
    List<PlacementDetailsDTO> findPlacementCandidatesByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    @Query(value = """
    SELECT DISTINCT
        b.id AS clientId,
        b.client_name AS clientName,
        b.client_address AS clientAddress,
        b.on_boarded_by AS onBoardedBy,
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_name, '$')), 
            '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"', ''
        ) AS clientSpocName,
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
            JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_mobile_number, '$')), 
            '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"', ''
        ) AS clientSpocMobileNumber
    FROM bdm_client b
    JOIN requirements_model r ON LOWER(b.client_name) = LOWER(r.client_name)
    JOIN user_details u ON u.user_id = :userId
    WHERE (
        TRIM(BOTH '"' FROM r.assigned_by) = TRIM(BOTH '"' FROM u.user_name)
        OR EXISTS (
            SELECT 1
            FROM job_recruiters jr
            WHERE jr.job_id = r.job_id
              AND jr.recruiter_id = u.user_id
        )
    )
    AND EXISTS (
        SELECT 1
        FROM candidate_submissions cs
        WHERE cs.job_id = r.job_id
          AND cs.user_id = u.user_id
          AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
    )
    AND u.status != 'INACTIVE'
    AND u.entity = 'IN'
""", nativeQuery = true)
    List<ClientDetailsDTO> findClientDetailsByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );






    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        cs.overall_feedback AS overallFeedback,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        r.client_name AS clientName
    FROM candidate_submissions cs
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    JOIN user_details u ON cs.user_id = u.user_id
    WHERE (
        TRIM(BOTH '\"' FROM r.assigned_by) = :assignedBy
        OR u.user_name = :assignedBy
    )
    AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate
    AND u.status != 'inactive' and u.entity = 'IN'
""", nativeQuery = true)
    List<SubmittedCandidateDTO> findSubmittedCandidatesByAssignedByAndDateRange(
            @Param("assignedBy") String assignedBy,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );



    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,

        -- Latest Status
        CASE 
            WHEN JSON_VALID(idt.interview_status) = 1 
              AND JSON_LENGTH(idt.interview_status) > 0
            THEN JSON_UNQUOTE(
                JSON_EXTRACT(
                    idt.interview_status,
                    CONCAT(
                        '$[',
                        CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                        '].status'
                    )
                )
            )
            ELSE NULL
        END AS interviewStatus,

        -- Latest Interview Level
        CASE 
            WHEN JSON_VALID(idt.interview_status) = 1 
              AND JSON_LENGTH(idt.interview_status) > 0
            THEN JSON_UNQUOTE(
                JSON_EXTRACT(
                    idt.interview_status,
                    CONCAT(
                        '$[',
                        CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                        '].interviewLevel'
                    )
                )
            )
            ELSE NULL
        END AS interviewLevel,

        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName,
        idt.user_id AS interviewTakenBy,

        -- Self or Team
        CASE 
            WHEN idt.user_id = :assignedBy THEN 'SELF'
            ELSE 'TEAM'
        END AS interviewType

    FROM interview_details idt
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id

    WHERE r.assigned_by = :assignedBy
      AND idt.interview_date_time IS NOT NULL
      AND JSON_VALID(idt.interview_status) = 1
      AND JSON_LENGTH(idt.interview_status) > 0
      AND idt.interview_date_time BETWEEN :startDate AND :endDate

      -- EXCLUDE if latest level = 'Internal' AND latest status = 'Rejected'
      AND NOT (
          JSON_UNQUOTE(
              JSON_EXTRACT(
                  idt.interview_status,
                  CONCAT(
                      '$[',
                      CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                      '].status'
                  )
              )
          ) = 'REJECTED'
          AND JSON_UNQUOTE(
              JSON_EXTRACT(
                  idt.interview_status,
                  CONCAT(
                      '$[',
                      CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                      '].interviewLevel'
                  )
              )
          ) = 'INTERNAL'
      )
""", nativeQuery = true)
    List<InterviewScheduledDTO> findScheduledInterviewsByAssignedByAndDateRange(
            @Param("assignedBy") String assignedBy,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query(value = """
    SELECT DISTINCT
        r.job_id AS jobId,
        TRIM(r.job_title) AS jobTitle,
        TRIM(r.client_name) AS clientName,
        TRIM(BOTH '\"' FROM r.assigned_by) AS assignedBy,
        r.status AS status,
        r.no_of_positions AS noOfPositions,
        r.qualification AS qualification,
        r.job_type AS jobType,
        r.job_mode AS jobMode,
        r.requirement_added_time_stamp AS postedDate
    FROM requirements_model r
    WHERE (
        REPLACE(REPLACE(r.assigned_by, '\"', ''), '\"', '') = REPLACE(REPLACE(:assignedBy, '\"', ''), '\"', '')
        OR EXISTS (
            SELECT 1
            FROM job_recruiters jr
            JOIN user_details u ON jr.recruiter_id = u.user_id
            WHERE jr.job_id = r.job_id
              AND REPLACE(REPLACE(u.user_name, '\"', ''), '\"', '') = REPLACE(REPLACE(:assignedBy, '\"', ''), '\"', '')
        )
    )
    AND EXISTS (
        SELECT 1
        FROM candidate_submissions cs
        WHERE cs.job_id = r.job_id
          AND cs.submitted_at BETWEEN :startDate AND :endDate
          AND (
              cs.user_id = (
                  SELECT u.user_id FROM user_details u WHERE u.user_name = :assignedBy LIMIT 1
              )
              OR EXISTS (
                  SELECT 1 FROM job_recruiters jr2 WHERE jr2.job_id = cs.job_id
                   AND jr2.recruiter_id = (
                       SELECT u2.user_id FROM user_details u2 WHERE u2.user_name = :assignedBy LIMIT 1
                   )
              )
          )
    )
""", nativeQuery = true)
    List<JobDetailsDTO> findJobDetailsByAssignedByAndDateRange(@Param("assignedBy") String assignedBy,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);




    @Query(value = """
    SELECT DISTINCT
        b.id AS clientId,  
        b.client_name AS clientName,  
        b.client_address AS clientAddress,  
        b.on_boarded_by AS onBoardedBy,
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_name, '$')), '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"', '') AS clientSpocName,  
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_EXTRACT(b.client_spoc_mobile_number, '$')), '[\"', ''), '\"]', ''), '\\\\"', ''), '\\\\', ''), '"', '') AS clientSpocMobileNumber
    FROM bdm_client b
    JOIN requirements_model r ON LOWER(b.client_name) = LOWER(r.client_name)
    WHERE (
        REPLACE(REPLACE(r.assigned_by, '\"', ''), '\"', '') = REPLACE(REPLACE(:assignedBy, '\"', ''), '\"', '')
        OR EXISTS (
            SELECT 1
            FROM job_recruiters jr
            JOIN user_details u ON jr.recruiter_id = u.user_id
            WHERE jr.job_id = r.job_id
              AND REPLACE(REPLACE(u.user_name, '\"', ''), '\"', '') = REPLACE(REPLACE(:assignedBy, '\"', ''), '\"', '')
        )
    )
    AND EXISTS (
        SELECT 1
        FROM candidate_submissions cs
        WHERE cs.job_id = r.job_id
          AND cs.submitted_at BETWEEN :startDate AND :endDate
          AND (
              cs.user_id = (
                  SELECT u.user_id FROM user_details u WHERE u.user_name = :assignedBy LIMIT 1
              )
              OR EXISTS (
                  SELECT 1 FROM job_recruiters jr2 WHERE jr2.job_id = r.job_id
                   AND jr2.recruiter_id = (
                       SELECT u2.user_id FROM user_details u2 WHERE u2.user_name = :assignedBy LIMIT 1
                   )
              )
          )
    )
""", nativeQuery = true)
    List<ClientDetailsDTO> findClientDetailsByAssignedByAndDateRange(@Param("assignedBy") String assignedBy,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);


    @Query(value = """
    SELECT 
        cd.candidate_id AS candidateId,
        cd.full_name AS fullName,
        cd.candidate_email_id AS candidateEmailId,
        cd.contact_number AS contactNumber,
        cd.qualification AS qualification,
        cs.skills AS skills,
        CASE\s
                        WHEN JSON_VALID(idt.interview_status) = 1\s
                             AND JSON_LENGTH(idt.interview_status) > 0
                        THEN JSON_UNQUOTE(
                            JSON_EXTRACT(
                                idt.interview_status,
                                CONCAT(
                                    '$[',
                                    CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                                    '].status'
                                )
                            )
                        )
                        ELSE idt.interview_status\s
                    END AS interviewStatus,
        idt.interview_level AS interviewLevel,
        idt.interview_date_time AS interviewDateTime,
        r.job_id AS jobId,
        r.job_title AS jobTitle,
        idt.client_name AS clientName
    FROM interview_details idt
    JOIN candidate_submissions cs ON cs.candidate_id = idt.candidate_id
    JOIN candidates cd ON cd.candidate_id = cs.candidate_id
    JOIN requirements_model r ON cs.job_id = r.job_id
    WHERE r.assigned_by = :assignedBy
    AND (
          (JSON_VALID(idt.interview_status)\s
            AND JSON_UNQUOTE(JSON_EXTRACT(
                idt.interview_status,
                    CONCAT(
                            '$[',
                                CAST(JSON_LENGTH(idt.interview_status) - 1 AS CHAR),
                            '].status'
                           )
                )) = 'PLACED')
           OR UPPER(idt.interview_status) = 'PLACED'
        )
      AND idt.timestamp BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<PlacementDetailsDTO> findPlacementCandidatesByAssignedByAndDateRange(
            @Param("assignedBy") String assignedBy,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = """
    (
    -- Part 1: EMPLOYEES with no active assigned jobs
    SELECT DISTINCT
        ud.user_id AS recruiterId,
        ud.user_name AS recruiterName,
        NULL AS jobId,
        NULL AS clientName,
        NULL AS bdm,
        NULL AS teamlead,
        NULL AS technology,
        NULL AS postedDate,
        NULL AS updatedDateTime,
        0 AS numberOfSubmissions,
        0 AS numberOfScreenReject,
        NULL AS jobTitle,
        NULL AS jobMode,
        NULL AS jobType,
        NULL AS exexperienceRequired,
        NULL AS relevantExperience
    FROM user_details ud
    JOIN user_roles ur ON ud.user_id = ur.user_id
    JOIN roles rl ON ur.role_id = rl.id
    WHERE rl.name IN ('EMPLOYEE','TEAMLEAD')
    AND ud.status = 'ACTIVE' AND ud.entity = 'IN'
      AND ud.user_id NOT IN (
          SELECT DISTINCT jr.recruiter_id
          FROM job_recruiters jr
          JOIN requirements_model r ON r.job_id = jr.job_id
          WHERE r.status IN ('In Progress', 'Submitted')
            AND DATE(r.updated_at) BETWEEN :startDate AND :endDate
      )
    AND ud.designation != 'testuser'
      AND NOT EXISTS (
          SELECT 1
          FROM job_recruiters jr
          JOIN requirements_model r ON r.job_id = jr.job_id
          WHERE jr.recruiter_id = ud.user_id
            AND r.status IN ('In Progress', 'Submitted')
            AND DATE(r.updated_at) BETWEEN :startDate AND :endDate
            AND ud.status = 'ACTIVE' AND ud.entity = 'IN'
      )
)
UNION ALL
(
    -- Part 2: EMPLOYEES with active jobs
    SELECT 
        ud.user_id AS recruiterId,
        ud.user_name AS recruiterName,
        r.job_id AS jobId,
        r.client_name as clientName,
        COALESCE((
              SELECT b.on_boarded_by\s
              FROM bdm_client b\s
              WHERE FIND_IN_SET(b.client_name, REPLACE(r.client_name, '_', ',')) > 0
              ORDER BY FIELD(b.client_name, SUBSTRING_INDEX(r.client_name, '_', 1)) DESC
              LIMIT 1
        ), 'N/A') AS bdm,
        COALESCE(r.assigned_by, 'N/A') AS teamlead,
        r.job_title AS technology,
        DATE_FORMAT(r.requirement_added_time_stamp, '%Y-%m-%d') AS postedDate,
        DATE_FORMAT(r.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedDateTime,
        (
            SELECT COUNT(DISTINCT cs.submission_id)
            FROM candidate_submissions cs
            WHERE cs.job_id = r.job_id
              AND cs.user_id = ud.user_id
              AND (
                (:isToday = true AND DATE(cs.submitted_at) = CURRENT_DATE)
                OR (:isToday = false AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate)
              )
        ) AS numberOfSubmissions,
        (
            SELECT COUNT(DISTINCT cs.submission_id)
            FROM candidate_submissions cs
            WHERE cs.job_id = r.job_id
              AND cs.user_id = ud.user_id
              AND cs.status = 'SCREEN REJECT'
              AND (
                (:isToday = true AND DATE(cs.submitted_at) = CURRENT_DATE)
                OR (:isToday = false AND DATE(cs.submitted_at) BETWEEN :startDate AND :endDate)
              )
        ) AS numberOfScreenReject,
        r.job_title AS jobTitle,
        r.job_mode AS jobMode,
        r.job_type AS jobType,
        r.experience_required AS experienceRequired,
        r.relevant_experience AS relevantExperience
    FROM user_details ud
    JOIN user_roles ur ON ud.user_id = ur.user_id
    JOIN roles rl ON ur.role_id = rl.id
    JOIN job_recruiters jr ON jr.recruiter_id = ud.user_id
    JOIN requirements_model r ON jr.job_id = r.job_id
    LEFT JOIN bdm_client b ON r.client_name = b.client_name
    WHERE r.status IN ('In Progress', 'Submitted')
    AND ud.status = 'ACTIVE' AND ud.entity = 'IN'
    AND ud.designation != 'testuser'
      AND DATE(r.updated_at) BETWEEN :startDate AND :endDate
)
ORDER BY recruiterName
""", nativeQuery = true)
    List<Object[]> findInProgressRequirementsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("isToday") boolean isToday
    );

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RequirementsModel r " +
            "WHERE r.clientName = :clientName AND r.requirementAddedTimeStamp >= :date")
    boolean existsByClientNameAndRequirementAddedTimeStampAfter(@Param("clientName") String clientName,
                                                                @Param("date") LocalDateTime date);

}

