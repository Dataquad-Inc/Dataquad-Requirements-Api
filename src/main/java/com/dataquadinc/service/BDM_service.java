package com.dataquadinc.service;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.ClientAlreadyExistsException;
import com.dataquadinc.exceptions.DateRangeValidationException;
import com.dataquadinc.exceptions.ResourceNotFoundException;
import com.dataquadinc.model.BDM_Client;
import com.dataquadinc.repository.BDM_Repo;
import com.dataquadinc.repository.BdmEmployeeProjection;
import com.dataquadinc.repository.RequirementsDao;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BDM_service {
    private static final Logger logger = LoggerFactory.getLogger(BDM_service.class);


    @Autowired
    private BDM_Repo repository;

    @Autowired
    private BDM_Repo repo;

    @Autowired
    private RequirementsDao requirementsDao;

    private static final Logger log = LoggerFactory.getLogger(BDM_service.class);


    private final Path UPLOAD_DIR = Paths.get("uploads");

    public BDM_Client saveClient(BDM_Client client) {

        String normalizedClientName = client.getClientName().trim().toLowerCase();
        boolean exists = repository.existsByClientNameIgnoreCase(normalizedClientName);

        if (repository.existsByClientNameIgnoreCase(client.getClientName().toLowerCase())) {
            throw new RuntimeException("Client name '" + client.getClientName() + "' already exists!");
        }
        client.setId(generateCustomId());
        return repository.save(client);
    }

    private String generateCustomId() {
        List<BDM_Client> clients = repository.findAll();
        int maxNumber = clients.stream()
                .map(client -> {
                    try {
                        return Integer.parseInt(client.getId().replace("CLIENT", ""));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(0);

        return String.format("CLIENT%03d", maxNumber + 1);
    }

    private BDM_Dto convertToDTO(BDM_Client client) {
        BDM_Dto dto = new BDM_Dto();
        dto.setId(client.getId());
        dto.setClientName(client.getClientName());
        dto.setClientAddress(client.getClientAddress());
        dto.setLocation(client.getLocation());
        dto.setNetPayment(client.getNetPayment());
        dto.setGst(client.getGst());
        dto.setSupportingCustomers(client.getSupportingCustomers());
        dto.setClientWebsiteUrl(client.getClientWebsiteUrl());
        dto.setClientLinkedInUrl(client.getClientLinkedInUrl());
        dto.setClientSpocName(client.getClientSpocName());
        dto.setClientSpocEmailid(client.getClientSpocEmailid());
        dto.setDocumentData(client.getDocumentedData());
        dto.setSupportingDocuments(client.getSupportingDocuments());  // List<byte[]>
        dto.setClientSpocLinkedin(client.getClientSpocLinkedin());
        dto.setClientSpocMobileNumber(client.getClientSpocMobileNumber());
        dto.setOnBoardedBy(client.getOnBoardedBy());
        dto.setPositionType(client.getPositionType());
        dto.setStatus(client.getStatus());
        dto.setFeedBack(client.getFeedBack());
        return dto;
    }

    private BDM_Client convertToEntity(BDM_Dto dto) {
        BDM_Client client = new BDM_Client();
        client.setId(dto.getId());
        client.setClientName(dto.getClientName());
        client.setClientAddress(dto.getClientAddress());
        client.setLocation(dto.getLocation());
        client.setNetPayment(dto.getNetPayment());
        client.setGst(dto.getGst());
        client.setSupportingCustomers(dto.getSupportingCustomers());
        client.setClientWebsiteUrl(dto.getClientWebsiteUrl());
        client.setClientLinkedInUrl(dto.getClientLinkedInUrl());
        client.setClientSpocName(dto.getClientSpocName());
        client.setClientSpocEmailid(dto.getClientSpocEmailid());
        client.setDocumentedData(dto.getDocumentData());
        client.setSupportingDocuments(dto.getSupportingDocuments());  // List<byte[]>
        client.setClientSpocLinkedin(dto.getClientSpocLinkedin());
        client.setClientSpocMobileNumber(dto.getClientSpocMobileNumber());
        client.setOnBoardedBy(dto.getOnBoardedBy());
        client.setPositionType(dto.getPositionType());
        client.setFeedBack(dto.getFeedBack());
        return client;
    }

    public BDM_Dto createClient(BDM_Dto dto, List<MultipartFile> files) throws IOException {

        if (!repo.findByClientName(dto.getClientName()).isEmpty()) {
            throw new ClientAlreadyExistsException("Client Name '" + dto.getClientName() + "' already exists.");
        }

        BDM_Client entity = convertToEntity(dto);
        entity.setId(generateCustomId()); // Generate custom ID

        // ✅ Set assignedBy logic
        String createdBy = dto.getOnBoardedBy();
        String assignedTo = dto.getAssignedTo();

        if (assignedTo != null && !assignedTo.isBlank()) {
            entity.setOnBoardedBy(assignedTo);  // assigned to someone selected
        } else {
            entity.setOnBoardedBy(createdBy);   // assigned to creator
        }

        // ✅ File upload
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        List<String> fileNames = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = file.getOriginalFilename();
                    fileNames.add(fileName);

                    // Save file to disk
                    Path filePath = uploadDir.resolve(fileName);
                    Files.write(filePath, file.getBytes());
                }
            }
        }

        entity.setSupportingDocuments(fileNames);  // ✅ Store file names in DB

        entity = repo.save(entity); // Save client in DB
        return convertToDTO(entity);
    }

    public List<BDM_Dto>getAllClients() {

        // 2. Fetch clients created in the current month
        List<BDM_Client> clients = repository.getClients();

        // 4. Logging
        logger.info("Fetched {} clients for current month {} to {}", clients.size());

        // 5. Return the list of clients
        return clients.stream()
                .map(client -> {
                    BDM_Dto dto = convertToDTO(client);
                    int requirementCount = repository.countRequirementsByClientName(client.getClientName());
                    dto.setNumberOfRequirements(requirementCount); // ⬅️ Set count here
                    return dto;
                })
                .collect(Collectors.toList());
}

    //@Override
    public List<Map<String, Object>> getOverallClients() {

        List<Object[]> results = repository.findOverallClients();

        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] obj : results) {
            Map<String, Object> map = new HashMap<>();

            map.put("clientId", obj[0]);
            map.put("clientName", obj[1]);
            map.put("bdmName", obj[2]);
            map.put("clientWebsiteUrl", obj[3]);
            map.put("clientLinkedInUrl", obj[4]);
            map.put("clientAddress", obj[5]);
            map.put("location", obj[6] != null ? obj[6] : "Not Available");

            response.add(map);
        }

        return response;
    }

    public List<BDM_Dto> getClientsByUserId(String userId) {

        // Step 1: get userName from DB
        String userName = requirementsDao.findUserNameByUserId(userId);

        if (userName == null) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        // Step 2: fetch clients onboarded by this user
        List<BDM_Client> clients = repository.findByOnBoardedBy(userName);

        // Step 3: convert to DTO
        return clients.stream()
                .map(client -> {
                    BDM_Dto dto = convertToDTO(client);

                    int requirementCount = repository.countRequirementsByClientName(client.getClientName());
                    dto.setNumberOfRequirements(requirementCount);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void evaluateClientStatuses() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<BDM_Client> allClients = repository.findAll();

        for (BDM_Client client : allClients) {
            boolean hasRecentJob = requirementsDao.existsByClientNameAndRequirementAddedTimeStampAfter(
                    client.getClientName(), thirtyDaysAgo);

            client.setStatus(hasRecentJob ? "ACTIVE" : "INACTIVE");
        }

        repository.saveAll(allClients); // bulk update
    }


    public Optional<BDM_Dto> getClientById(String id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    public Optional<BDM_Dto> updateClient(String id, BDM_Dto dto, List<MultipartFile> files) {
        return repository.findById(id).map(existingClient -> {

            // 🔁 Assignment logic
            String createdBy = dto.getOnBoardedBy();   // Person updating
            String assignedTo = dto.getAssignedTo();   // Optional person reassigned to

            if (assignedTo != null && !assignedTo.isBlank()) {
                existingClient.setOnBoardedBy(assignedTo);  // Set to assigned person
            } else if (createdBy != null && !createdBy.isBlank()) {
                existingClient.setOnBoardedBy(createdBy);   // Set to updater if not reassigned
            }

            // 🔁 Update only non-null fields
            if (dto.getClientName() != null) existingClient.setClientName(dto.getClientName());
            if (dto.getClientAddress() != null) existingClient.setClientAddress(dto.getClientAddress());
            if (dto.getNetPayment() != 0) existingClient.setNetPayment(dto.getNetPayment());
            if (dto.getGst() != 0.0) existingClient.setGst(dto.getGst());
            if (dto.getClientWebsiteUrl() != null) existingClient.setClientWebsiteUrl(dto.getClientWebsiteUrl());
            if (dto.getClientLinkedInUrl() != null) existingClient.setClientLinkedInUrl(dto.getClientLinkedInUrl());
            if (dto.getClientSpocName() != null) existingClient.setClientSpocName(dto.getClientSpocName());
            if (dto.getClientSpocEmailid() != null) existingClient.setClientSpocEmailid(dto.getClientSpocEmailid());
            if (dto.getClientSpocLinkedin() != null) existingClient.setClientSpocLinkedin(dto.getClientSpocLinkedin());
            if (dto.getClientSpocMobileNumber() != null) existingClient.setClientSpocMobileNumber(dto.getClientSpocMobileNumber());
            if (dto.getPositionType() != null) existingClient.setPositionType(dto.getPositionType());
            if (dto.getSupportingCustomers() != null) existingClient.setSupportingCustomers(dto.getSupportingCustomers());
            if (dto.getFeedBack() != null) existingClient.setFeedBack(dto.getFeedBack());
            // 🔁 File uploads
            try {
                if (files != null && !files.isEmpty()) {
                    Path uploadDir = Paths.get("uploads");
                    if (!Files.exists(uploadDir)) {
                        Files.createDirectories(uploadDir);
                    }

                    List<String> fileNames = new ArrayList<>(existingClient.getSupportingDocuments());

                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            String fileName = file.getOriginalFilename();
                            fileNames.add(fileName);

                            Path filePath = uploadDir.resolve(fileName);
                            Files.write(filePath, file.getBytes());
                        }
                    }
                    existingClient.setSupportingDocuments(fileNames);
                }
            } catch (IOException e) {
                e.printStackTrace();  // Optionally convert to RuntimeException if needed
            }

            return convertToDTO(repository.save(existingClient));
        });
    }


    // ✅ 1. Method with startDate and endDate passed
    public List<BdmEmployeeDTO> getAllBdmEmployeesDateFilter(LocalDate startDate, LocalDate endDate) {
        logger.info("📅 Fetching BDM employee stats from {} to {}", startDate, endDate);

        List<BdmEmployeeProjection> bdmUsers = repository.findAllBdmEmployees();
        logger.info("👥 Total BDM employees found: {}", bdmUsers.size());

        List<BdmEmployeeDTO> result = new ArrayList<>();

        for (BdmEmployeeProjection user : bdmUsers) {
            String userId = user.getUserId();
            String userName = user.getUserName();

            logger.info("➡️ Processing BDM: {} (ID: {})", userName, userId);

            long clientCount = repository.countClientsByUserIdAndDateRange(userId, startDate, endDate);
            List<String> clientNames = repository.findClientNamesByUserIdAndDateRange(userId, startDate, endDate);

            logger.info("📦 Clients for {}: {} | Count: {}", userName, clientNames, clientCount);

            long submissionCount = 0;
            long interviewCount = 0;
            long placementCount = 0;
            long requirementsCount = 0;

            for (String client : clientNames) {
                logger.debug("🔍 Client: {}", client);

                long subCount = repository.countAllSubmissionsByClientNameAndDateRange(client, startDate, endDate);
                long intCount = repository.countAllInterviewsByClientNameAndDateRange(client, startDate, endDate);
                long plcCount = repository.countAllPlacementsByClientNameAndDateRange(client, startDate, endDate);
                long reqCount = repository.countRequirementsByClientNameAndDateRange(client, startDate, endDate);

                logger.debug("📑 Submissions: {}, Interviews: {}, Placements: {}, Requirements: {}",
                        subCount, intCount, plcCount, reqCount);

                submissionCount += subCount;
                interviewCount += intCount;
                placementCount += plcCount;
                requirementsCount += reqCount;
            }

            result.add(new BdmEmployeeDTO(
                    userId,
                    userName,
                    user.getRoleName(),
                    user.getEmail(),
                    user.getStatus(),
                    clientCount,
                    requirementsCount,
                    submissionCount,
                    interviewCount,
                    placementCount
            ));
        }

        logger.info("✅ BDM stats generation completed.");
        return result;
    }

    // ✅ 2. Method for current month (auto sets date range)
    public List<BdmEmployeeDTO> getAllBdmEmployees() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1);

        logger.info("🗓️ Defaulting to current month: {} to {}", startDate, endDate);

        List<BdmEmployeeProjection> bdmUsers = repository.findAllBdmEmployees();
        logger.info("👥 Total BDM employees found: {}", bdmUsers.size());

        List<BdmEmployeeDTO> result = new ArrayList<>();

        for (BdmEmployeeProjection user : bdmUsers) {
            String userId = user.getUserId();
            String userName = user.getUserName();

            logger.info("➡️ Processing BDM: {} (ID: {})", userName, userId);

            long clientCount = repository.countClientsByUserIdAndDateRange(userId, startDate, endDate);
            List<String> clientNames = repository.findClientNamesByUserIdAndDateRange(userId, startDate, endDate);

            logger.info("📦 Clients for {}: {} | Count: {}", userName, clientNames, clientCount);

            long submissionCount = 0;
            long interviewCount = 0;
            long placementCount = 0;
            long requirementsCount = 0;

            for (String client : clientNames) {
                logger.debug("🔍 Client: {}", client);

                long subCount = repository.countAllSubmissionsByClientNameAndDateRange(client, startDate, endDate);
                long intCount = repository.countAllInterviewsByClientNameAndDateRange(client, startDate, endDate);
                long plcCount = repository.countAllPlacementsByClientNameAndDateRange(client, startDate, endDate);
                long reqCount = repository.countRequirementsByClientNameAndDateRange(client, startDate, endDate);

                logger.debug("📑 Submissions: {}, Interviews: {}, Placements: {}, Requirements: {}",
                        subCount, intCount, plcCount, reqCount);

                submissionCount += subCount;
                interviewCount += intCount;
                placementCount += plcCount;
                requirementsCount += reqCount;
            }

            result.add(new BdmEmployeeDTO(
                    userId,
                    userName,
                    user.getRoleName(),
                    user.getEmail(),
                    user.getStatus(),
                    clientCount,
                    requirementsCount,
                    submissionCount,
                    interviewCount,
                    placementCount
            ));
        }

        logger.info("✅ Current month BDM stats generation completed.");
        return result;
    }

    public void deleteClient(String id) {
        repository.deleteById(id);
    }

    public BdmClientDetailsDTO getBdmClientDetails(String userId) {

        LocalDate endDate=LocalDate.now();
        LocalDate startDate=endDate.withDayOfMonth(1);

        log.info("🔍 Fetching BDM client details for userId: {}", userId);

        // 1️⃣ Fetch BDM Details
        List<BdmDetailsDto> bdmDetails = getBdmDetails(userId);
        log.info("✅ Fetched {} BDM details for userId: {}", bdmDetails.size(), userId);


        // 2️⃣ Fetch Clients onboarded by the BDM
        List<BdmClientDto> clientDetails = getClientDetails(userId, startDate, endDate);
        log.info("✅ Fetched {} client details for userId: {}", clientDetails.size(), userId);


        // 3️⃣ Fetch Submissions for each client
        Map<String, List<BdmSubmissionDTO>> submissions = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            submissions.put(client.getClientName(), getSubmissionsDateFilter(client.getClientName(),startDate,endDate));
        }

        // 4️⃣ Fetch Interviews for each client
        Map<String, List<BdmInterviewDTO>> interviews = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            interviews.put(client.getClientName(), getInterviewsDateFilter(client.getClientName(), startDate, endDate));
        }

        // 5️⃣ Fetch Placements for each client
        Map<String, List<BdmPlacementDTO>> placements = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            placements.put(client.getClientName(), getPlacementsDateFilter(client.getClientName(), startDate, endDate));
        }

        // 6️⃣ Fetch Requirements for each client (This section is now after client details)
        Map<String, List<RequirementDto>> requirements = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            requirements.put(client.getClientName(), getRequirements(client.getClientName()));
        }

        // 🔢 Logging total counts across all clients
        int totalSubmissions = submissions.values().stream().mapToInt(List::size).sum();
        int totalInterviews = interviews.values().stream().mapToInt(List::size).sum();
        int totalPlacements = placements.values().stream().mapToInt(List::size).sum();
        int totalRequirements = requirements.values().stream().mapToInt(List::size).sum();

        System.out.println(String.format(
                "Fetched BDM Count from %s to %s",
                startDate, endDate));

        log.info("📊 Total Clients: {}", clientDetails.size());
        log.info("📊 Total Submissions (all clients combined): {}", totalSubmissions);
        log.info("📊 Total Interviews (all clients combined): {}", totalInterviews);
        log.info("📊 Total Placements (all clients combined): {}", totalPlacements);
        log.info("📊 Total Requirements (all clients combined): {}", totalRequirements);

        // Return DTO with all details
        log.info("✅ Successfully fetched all details for BDM userId: {}", userId);

        return new BdmClientDetailsDTO(bdmDetails, clientDetails, submissions, interviews, placements, requirements);
    }
    public BdmClientDetailsDTO getBdmClientDetailsDateRange(String userId,LocalDate startDate,LocalDate endDate) {
        log.info("🔍 Fetching BDM client details for userId: {}", userId);

        // 1️⃣ Fetch BDM Details
        List<BdmDetailsDto> bdmDetails = getBdmDetails(userId);
        log.info("✅ Fetched {} BDM details for userId: {}", bdmDetails.size(), userId);


        // 2️⃣ Fetch Clients onboarded by the BDM
        List<BdmClientDto> clientDetails = getClientDetailsDateFilter(userId, startDate, endDate);
        log.info("✅ Fetched {} client details for userId: {}", clientDetails.size(), userId);


        // 3️⃣ Fetch Submissions for each client
        Map<String, List<BdmSubmissionDTO>> submissions = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            submissions.put(client.getClientName(), getSubmissionsDateFilter(client.getClientName(),startDate,endDate));
        }

        // 4️⃣ Fetch Interviews for each client
        Map<String, List<BdmInterviewDTO>> interviews = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            interviews.put(client.getClientName(), getInterviewsDateFilter(client.getClientName(),startDate,endDate));
        }

        // 5️⃣ Fetch Placements for each client
        Map<String, List<BdmPlacementDTO>> placements = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            placements.put(client.getClientName(), getPlacementsDateFilter(client.getClientName(),startDate,endDate));
        }

        // 6️⃣ Fetch Requirements for each client (This section is now after client details)
        Map<String, List<RequirementDto>> requirements = new HashMap<>();
        for (BdmClientDto client : clientDetails) {
            requirements.put(client.getClientName(), getRequirementsDateFilter(client.getClientName(),startDate,endDate));
        }

        // 🔢 Logging total counts across all clients
        int totalSubmissions = submissions.values().stream().mapToInt(List::size).sum();
        int totalInterviews = interviews.values().stream().mapToInt(List::size).sum();
        int totalPlacements = placements.values().stream().mapToInt(List::size).sum();
        int totalRequirements = requirements.values().stream().mapToInt(List::size).sum();



        log.info("📊 Total Clients: {}", clientDetails.size());
        log.info("📊 Total Submissions (all clients combined): {}", totalSubmissions);
        log.info("📊 Total Interviews (all clients combined): {}", totalInterviews);
        log.info("📊 Total Placements (all clients combined): {}", totalPlacements);
        log.info("📊 Total Requirements (all clients combined): {}", totalRequirements);

        // Return DTO with all details
        log.info("✅ Successfully fetched all details for BDM userId: {}", userId);

        return new BdmClientDetailsDTO(bdmDetails, clientDetails, submissions, interviews, placements, requirements);
    }

    private List<RequirementDto> getRequirements(String clientName) {
        log.info("🔍 Fetching requirements for client: {}", clientName);

        // Fetch the requirement data for the given client from the database
        List<Tuple> requirementTuples = requirementsDao.findRequirementsByClientName(clientName);

        // Convert the Tuple data into RequirementDto objects
        return requirementTuples.stream()
                .map(tuple -> new RequirementDto(
                        tuple.get("recruiter_name", String.class), // Ensure alias matches the query result
                        tuple.get("client_name", String.class),
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("assigned_by", String.class),
                        tuple.get("location", String.class),
                        tuple.get("notice_period", String.class)
                ))
                .collect(Collectors.toList());
    }
    private List<RequirementDto> getRequirementsDateFilter(String clientName,LocalDate startDate,LocalDate endDate) {
        log.info("🔍 Fetching requirements for client: {}", clientName);

        // Fetch the requirement data for the given client from the database
        List<Tuple> requirementTuples = requirementsDao.findRequirementsByClientNameDateFilter(clientName,startDate,endDate);

        // Convert the Tuple data into RequirementDto objects
        return requirementTuples.stream()
                .map(tuple -> new RequirementDto(
                        tuple.get("recruiter_name", String.class), // Ensure alias matches the query result
                        tuple.get("client_name", String.class),
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("assigned_by", String.class),
                        tuple.get("location", String.class),
                        tuple.get("notice_period", String.class)
                ))
                .collect(Collectors.toList());
    }


    private List<BdmDetailsDto> getBdmDetails(String userId) {
        List<Tuple> bdmTuples = requirementsDao.findBdmEmployeeByUserId(userId);
        log.info("🔍 Fetching BDM details for userId: {}", userId);

        return bdmTuples.stream()
                .map(tuple -> new BdmDetailsDto(
                        tuple.get("user_id", String.class),
                        tuple.get("user_name", String.class),
                        tuple.get("role_name", String.class), // Ensure alias matches query
                        tuple.get("email", String.class),
                        tuple.get("designation", String.class),
                        formatDate(tuple.get("joining_date")), // Handles both String and Date
                        tuple.get("gender", String.class),
                        formatDate(tuple.get("dob")), // Handles both String and Date
                        tuple.get("phone_number", String.class),
                        tuple.get("personalemail", String.class), // Ensure alias matches query
                        tuple.get("status", String.class),
                        tuple.get("client_name", String.class)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to format date fields to 'yyyy-MM-dd'.
     */
    private String formatDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        try {
            if (dateObj instanceof String) {
                return dateObj.toString();
            } else if (dateObj instanceof java.sql.Date || dateObj instanceof java.util.Date) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                return dateFormat.format(dateObj);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log error if necessary
        }
        return null;
    }


    // Utility method to safely convert Object to String date
    private String convertDate(Object dateObj) {
        if (dateObj == null) return null;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        if (dateObj instanceof java.sql.Date || dateObj instanceof java.util.Date) {
            return dateFormat.format(dateObj);
        } else if (dateObj instanceof String) {
            try {
                return dateFormat.format(dateFormat.parse((String) dateObj)); // Convert String to Date then format
            } catch (ParseException e) {
                return (String) dateObj; // Return as-is if parsing fails
            }
        }
        return null;
    }
    private List<BdmClientDto> getClientDetailsDateFilter(String userId,LocalDate startDate,LocalDate endDate) {
        List<Tuple> clientTuples = requirementsDao.findClientsByBdmUserIdAndCreatedAtBetween(userId,startDate,endDate);
        log.info("🔍 Fetching client details for userId: {}", userId);

        return clientTuples.stream()
                .map(tuple -> new BdmClientDto(
                        tuple.get("id", String.class),
                        tuple.get("client_name", String.class),
                        tuple.get("on_boarded_by", String.class),
                        tuple.get("client_address", String.class),
                        cleanAndConvertToList(tuple.get("client_spoc_name", String.class)),
                        cleanAndConvertToList(tuple.get("client_spoc_emailid", String.class)),
                        cleanAndConvertToList(tuple.get("client_spoc_mobile_number", String.class))
                ))
                .collect(Collectors.toList());
    }


    private List<BdmClientDto> getClientDetails(String userId ,LocalDate startDate, LocalDate endDate) {
        List<Tuple> clientTuples = requirementsDao.findClientsByBdmUserIdAndCreatedAtBetween(userId ,startDate, endDate);
        log.info("🔍 Fetching client details for userId: {}", userId);

        return clientTuples.stream()
                .map(tuple -> new BdmClientDto(
                        tuple.get("id", String.class),
                        tuple.get("client_name", String.class),
                        tuple.get("on_boarded_by", String.class),
                        tuple.get("client_address", String.class),
                        cleanAndConvertToList(tuple.get("client_spoc_name", String.class)),
                        cleanAndConvertToList(tuple.get("client_spoc_emailid", String.class)),
                        cleanAndConvertToList(tuple.get("client_spoc_mobile_number", String.class))
                ))
                .collect(Collectors.toList());
    }

    // ✅ Helper Method to Clean and Convert JSON Arrays / Comma-Separated Strings to List<String>
    private List<String> cleanAndConvertToList(String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }

        // Remove extra quotes and brackets if it's a JSON array
        input = input.replace("[", "").replace("]", "").replace("\"", "");

        // Split by commas and return a clean list
        return Arrays.stream(input.split(","))
                .map(String::trim) // Trim spaces
                .filter(s -> !s.isEmpty()) // Remove empty entries
                .collect(Collectors.toList());
    }

    private List<BdmSubmissionDTO> getSubmissionsDateFilter(String clientName,LocalDate startDate,LocalDate endDate) {
        List<Tuple> submissionTuples = requirementsDao.findAllSubmissionsByClientNameAndSubmittedAtBetween(clientName,startDate,endDate);
        log.info("🔍 Fetching submissions for client: {}", clientName);

        return submissionTuples.stream()
                .map(tuple -> new BdmSubmissionDTO(
                        tuple.get("candidate_id", String.class),
                        tuple.get("full_name", String.class),
                        tuple.get("candidateEmailId", String.class), // Use exact alias from SQL
                        tuple.get("contact_number", String.class),
                        tuple.get("qualification", String.class),
                        tuple.get("skills", String.class),
                        tuple.get("overall_feedback", String.class),
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("client_name", String.class)
                ))
                .collect(Collectors.toList());
    }
    private List<BdmSubmissionDTO> getSubmissions(String clientName, LocalDate startDate, LocalDate endDate) {
        List<Tuple> submissionTuples = requirementsDao.findAllSubmissionsByClientNameAndSubmittedAtBetween(clientName, startDate, endDate);
        log.info("🔍 Fetching submissions for client: {}", clientName);

        return submissionTuples.stream()
                .map(tuple -> new BdmSubmissionDTO(
                        tuple.get("candidate_id", String.class),
                        tuple.get("full_name", String.class),
                        tuple.get("candidateEmailId", String.class), // Use exact alias from SQL
                        tuple.get("contact_number", String.class),
                        tuple.get("qualification", String.class),
                        tuple.get("skills", String.class),
                        tuple.get("overall_feedback", String.class),
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("client_name", String.class)
                ))
                .collect(Collectors.toList());
    }

    private List<BdmInterviewDTO> getInterviews(String clientName) {
        List<Tuple> interviewTuples = requirementsDao.findAllInterviewsByClientName(clientName);
        return interviewTuples.stream()
                .map(tuple -> {
                    Timestamp timestamp = tuple.get("interview_date_time", Timestamp.class);
                    String interviewDateTimeStr = (timestamp != null)
                            ? OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            : null;

                    return new BdmInterviewDTO(
                            tuple.get("candidate_id", String.class),  // candidateId
                            tuple.get("full_name", String.class),     // fullName
                            tuple.get("candidateEmailId", String.class), // email
                            tuple.get("contact_number", String.class), // contactNumber
                            tuple.get("qualification", String.class),  // qualification
                            tuple.get("skills", String.class),         // skills
                            tuple.get("interview_status", String.class), // interviewStatus
                            tuple.get("interview_level", String.class),  // interviewLevel
                            interviewDateTimeStr  // interviewDateTime (converted to String)
                    );
                })
                .collect(Collectors.toList());
    }

    private List<BdmInterviewDTO> getInterviewsDateFilter(String clientName,LocalDate startDate,LocalDate endDate) {
        List<Tuple> interviewTuples = requirementsDao.findAllInterviewsByClientNameDateFilter(clientName,startDate,endDate);
        return interviewTuples.stream()
                .map(tuple -> {
                    Timestamp timestamp = tuple.get("interview_date_time", Timestamp.class);
                    String interviewDateTimeStr = (timestamp != null)
                            ? OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            : null;

                    return new BdmInterviewDTO(
                            tuple.get("candidate_id", String.class),  // candidateId
                            tuple.get("full_name", String.class),     // fullName
                            tuple.get("candidateEmailId", String.class), // email
                            tuple.get("contact_number", String.class), // contactNumber
                            tuple.get("qualification", String.class),  // qualification
                            tuple.get("skills", String.class),         // skills
                            tuple.get("interview_status", String.class), // interviewStatus
                            tuple.get("interview_level", String.class),  // interviewLevel
                            interviewDateTimeStr  // interviewDateTime (converted to String)
                    );
                })
                .collect(Collectors.toList());
    }


    private List<BdmPlacementDTO> getPlacements(String clientName) {
        List<Tuple> placementTuples = requirementsDao.findAllPlacementsByClientName(clientName);
        return placementTuples.stream()
                .map(tuple -> new BdmPlacementDTO(
                        tuple.get("candidate_id", String.class),
                        tuple.get("full_name", String.class),
                        tuple.get("candidateEmailId", String.class), // Fix alias
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("client_name", String.class)
                ))
                .collect(Collectors.toList());
    }

    private List<BdmPlacementDTO> getPlacementsDateFilter(String clientName,LocalDate startDate,LocalDate endDate) {
        List<Tuple> placementTuples = requirementsDao.findAllPlacementsByClientNameDateFilter(clientName,startDate,endDate);
        return placementTuples.stream()
                .map(tuple -> new BdmPlacementDTO(
                        tuple.get("candidate_id", String.class),
                        tuple.get("full_name", String.class),
                        tuple.get("candidateEmailId", String.class), // Fix alias
                        tuple.get("job_id", String.class),
                        tuple.get("job_title", String.class),
                        tuple.get("client_name", String.class)
                ))
                .collect(Collectors.toList());
    }

    public List<BDM_Client> getClientsByCreatedAtRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new DateRangeValidationException("Start date and End date must not be null.");
        }

        if (endDate.isBefore(startDate)) {
            throw new DateRangeValidationException("End date cannot be before start date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<BDM_Client> clients = repository.getClientsByCreatedAtRange(startDateTime, endDateTime);

        // 🔁 Set requirement count for each client
        for (BDM_Client client : clients) {
            int count = repository.countRequirementsByClientName(client.getClientName());
            client.setNumberOfRequirements(count);
        }

        logger.info("✅ Found {} clients created between {} and {}", clients.size(), startDate, endDate);
        return clients;
    }

    public List<RequirementsDto> getRequirementsForBdmByUserId(String userId) {
        int userExists = requirementsDao.countByUserId(userId);
        if (userExists == 0) {
            logger.warn("User ID '{}' not found in the database", userId);
            throw new ResourceNotFoundException("User ID '" + userId + "' not found in the database.");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        // ✅ Use the updated native query
        List<Tuple> results = repo.findRequirementsByBdmUserId(userId);

        List<RequirementsDto> dtos = results.stream().map(tuple -> {
            RequirementsDto dto = new RequirementsDto();

            dto.setJobId((String) tuple.get("job_id"));
            dto.setJobTitle((String) tuple.get("job_title"));
            dto.setClientName((String) tuple.get("client_name"));
            dto.setJobDescription((String) tuple.get("job_description"));
            dto.setJobDescriptionBlob((byte[]) tuple.get("job_description_blob"));
            dto.setJobType((String) tuple.get("job_type"));
            dto.setLocation((String) tuple.get("location"));
            dto.setJobMode((String) tuple.get("job_mode"));
            dto.setExperienceRequired((String) tuple.get("experience_required"));
            dto.setNoticePeriod((String) tuple.get("notice_period"));
            dto.setRelevantExperience((String) tuple.get("relevant_experience"));
            dto.setQualification((String) tuple.get("qualification"));
            dto.setSalaryPackage((String) tuple.get("salary_package"));
            dto.setNoOfPositions((Integer) tuple.get("no_of_positions"));

            Timestamp timestamp = (Timestamp) tuple.get("requirement_added_time_stamp");
            dto.setRequirementAddedTimeStamp(timestamp != null ? timestamp.toLocalDateTime() : null);

            dto.setStatus((String) tuple.get("status"));
            dto.setAssignedBy((String) tuple.get("assigned_by"));

            // ✅ Updated alias to match SQL
            String recruiterIdsStr = (String) tuple.get("recruiter_id");
            dto.setRecruiterIds(recruiterIdsStr != null ?
                    Arrays.stream(recruiterIdsStr.split(",")).map(String::trim).collect(Collectors.toSet())
                    : Collections.emptySet());

            String recruiterNamesStr = (String) tuple.get("recruiter_name");
            dto.setRecruiterName(recruiterNamesStr != null ?
                    Arrays.stream(recruiterNamesStr.split(",")).map(String::trim).collect(Collectors.toSet())
                    : Collections.emptySet());

            String jobId = dto.getJobId();
            dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(jobId));
            dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(jobId));

            return dto;
        }).collect(Collectors.toList());

        logger.info("✅ Fetched {} requirements for BDM userId '{}' between {} and {}",
                dtos.size(), userId, startOfMonth.toLocalDate(), endOfMonth.toLocalDate());

        return dtos;
    }


    public List<RequirementsDto> getRequirementsByBdmUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Tuple> requirements = repo.findRequirementsByBdmUserIdAndDateRange(userId, startDateTime, endDateTime);

        Map<String, RequirementsDto> dtoMap = new HashMap<>();

        for (Tuple tuple : requirements) {
            String jobId = (String) tuple.get("job_id");

            RequirementsDto dto = dtoMap.computeIfAbsent(jobId, id -> {
                RequirementsDto newDto = new RequirementsDto();

                newDto.setJobId(id);
                newDto.setJobTitle((String) tuple.get("job_title"));
                newDto.setClientName((String) tuple.get("client_name"));
                newDto.setJobDescription((String) tuple.get("job_description"));
                newDto.setJobDescriptionBlob((byte[]) tuple.get("job_description_blob"));
                newDto.setJobType((String) tuple.get("job_type"));
                newDto.setLocation((String) tuple.get("location"));
                newDto.setJobMode((String) tuple.get("job_mode"));
                newDto.setExperienceRequired((String) tuple.get("experience_required"));
                newDto.setNoticePeriod((String) tuple.get("notice_period"));
                newDto.setRelevantExperience((String) tuple.get("relevant_experience"));
                newDto.setQualification((String) tuple.get("qualification"));
                newDto.setSalaryPackage((String) tuple.get("salary_package"));
                newDto.setNoOfPositions((Integer) tuple.get("no_of_positions"));

                Timestamp timestamp = (Timestamp) tuple.get("requirement_added_time_stamp");
                newDto.setRequirementAddedTimeStamp(timestamp != null ? timestamp.toLocalDateTime() : null);

                newDto.setStatus((String) tuple.get("status"));
                newDto.setAssignedBy((String) tuple.get("assigned_by"));

                newDto.setRecruiterIds(new HashSet<>());
                newDto.setRecruiterName(new HashSet<>());

                newDto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(id));
                newDto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(id));

                return newDto;
            });

            // Parse and add recruiter IDs (comma-separated string)
            String recruiterIdsStr = (String) tuple.get("recruiter_id");
            if (recruiterIdsStr != null && !recruiterIdsStr.isEmpty()) {
                Arrays.stream(recruiterIdsStr.split(","))
                        .map(String::trim)
                        .forEach(dto.getRecruiterIds()::add);
            }

            // Parse and add recruiter names
            String recruiterNamesStr = (String) tuple.get("recruiter_name");
            if (recruiterNamesStr != null && !recruiterNamesStr.isEmpty()) {
                Arrays.stream(recruiterNamesStr.split(","))
                        .map(String::trim)
                        .forEach(dto.getRecruiterName()::add);
            }
        }

        List<RequirementsDto> resultList = new ArrayList<>(dtoMap.values());

        logger.info("✅ Fetched {} requirements for BDM userId {} between {} and {}",
                resultList.size(), userId, startDate, endDate);

        return resultList;
    }

}
