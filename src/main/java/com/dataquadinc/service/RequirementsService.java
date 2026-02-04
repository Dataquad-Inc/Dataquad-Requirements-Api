package com.dataquadinc.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Tuple;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dataquadinc.model.RequirementsModel;
import com.dataquadinc.repository.RequirementsDao;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequirementsService {
	private static final Logger logger = LoggerFactory.getLogger(RequirementsService.class);


	@Autowired
	private RequirementsDao requirementsDao;

	@Autowired
	private ModelMapper modelMapper;

	@Autowired
	private EmailService emailService;

	@Autowired
	private RestTemplate restTemplate;


	private static final Logger log = LoggerFactory.getLogger(BDM_service.class);


	@Transactional
	public RequirementAddedResponse createRequirement(RequirementsDto requirementsDto) throws IOException {
		// Map DTO to model
		RequirementsModel model = modelMapper.map(requirementsDto, RequirementsModel.class);

		// Handle recruiterIds properly
		if (requirementsDto.getRecruiterIds() != null && !requirementsDto.getRecruiterIds().isEmpty()) {
			model.setRecruiterIds(requirementsDto.getRecruiterIds());
		}
		// Convert recruiterIds array (from frontend) to JSON string
		if (requirementsDto.getRecruiterIds() != null && !requirementsDto.getRecruiterIds().isEmpty()) {
			// Convert recruiterIds (Set<String>) to JSON String
			ObjectMapper objectMapper = new ObjectMapper();
			String recruiterIdsJson = objectMapper.writeValueAsString(requirementsDto.getRecruiterIds());
			model.setRecruiterIds(Collections.singleton(recruiterIdsJson));  // Set the JSON string in the model
		}

		// Clean and process recruiter IDs
		if (requirementsDto.getRecruiterIds() != null && !requirementsDto.getRecruiterIds().isEmpty()) {
			Set<String> cleanedRecruiterIds = requirementsDto.getRecruiterIds().stream()
					.map(this::cleanRecruiterId)
					.collect(Collectors.toSet());
			model.setRecruiterIds(cleanedRecruiterIds);
		}

		// Validate that only one of the fields (jobDescription or jobDescriptionFile) is provided
		if ((requirementsDto.getJobDescription() != null && !requirementsDto.getJobDescription().isEmpty()) &&
				(requirementsDto.getJobDescriptionFile() != null && !requirementsDto.getJobDescriptionFile().isEmpty())) {
			// Both fields are provided, throw an exception or return an error response
			throw new IllegalArgumentException("You can either provide a job description text or upload a job description file, but not both.");
		}

		// Handle job description: either text or file
		String jobDescription = ""; // Initialize to hold text-based description

		// If the job description is provided as text (string), save it and set the BLOB to null
		if (requirementsDto.getJobDescription() != null && !requirementsDto.getJobDescription().isEmpty()) {
			jobDescription = requirementsDto.getJobDescription();
			model.setJobDescription(jobDescription);  // Set the text-based description
			model.setJobDescriptionBlob(null);  // Ensure the file-based description is null
		}

		// If a file for the job description is uploaded, process the file using processJobDescriptionFile
		if (requirementsDto.getJobDescriptionFile() != null && !requirementsDto.getJobDescriptionFile().isEmpty()) {
			// Use processJobDescriptionFile to extract text or convert image to Base64
			String processedJobDescription = processJobDescriptionFile(requirementsDto.getJobDescriptionFile());

			// If it's text-based, set it as the job description
			model.setJobDescription(processedJobDescription);

			// If it's a Base64 image, store it as a BLOB (in case you need to handle images differently)
			if (processedJobDescription.startsWith("data:image")) { // This means it is an image in Base64 format
				byte[] jobDescriptionBytes = Base64.getDecoder().decode(processedJobDescription.split(",")[1]);
				model.setJobDescriptionBlob(jobDescriptionBytes);
			} else {
				model.setJobDescriptionBlob(null);  // Ensure BLOB is null if text is extracted
			}
		}


		// If jobId is not set, let @PrePersist handle the generation
		if (model.getJobId() == null || model.getJobId().isEmpty()) {
			model.setStatus("In Progress");
			model.setRequirementAddedTimeStamp(LocalDateTime.now());
			model.setUpdatedAt(LocalDateTime.now());
			requirementsDao.save(model);
		} else {
			// Throw exception if the jobId already exists
			throw new RequirementAlreadyExistsException(
					"Requirements Already Exists with Job Id : " + model.getJobId());
		}


		// Send email to each recruiter assigned to the requirement
		sendEmailsToRecruiters(model);


		// Return the response using the generated jobId
		return new RequirementAddedResponse(model.getJobId(), requirementsDto.getJobTitle(), "Requirement Added Successfully");
	}

	// Helper method to clean recruiter ID
	private String cleanRecruiterId(String recruiterId) {
		// Remove all quotes, brackets, and whitespace
		return recruiterId.replaceAll("[\"\\[\\]\\s]", "");
	}

	public String getRecruiterEmail(String recruiterId) {
		Tuple userTuple = requirementsDao.findUserEmailAndUsernameByUserId(recruiterId);
		return userTuple != null ? userTuple.get(0, String.class) : null;
	}


	private byte[] saveJobDescriptionFileAsBlob(MultipartFile jobDescriptionFile, String jobId) throws IOException {
		// Check if the file is empty
		if (jobDescriptionFile.isEmpty()) {
			throw new IOException("File is empty.");
		}

		// Get the byte array of the uploaded file
		byte[] jobDescriptionBytes = jobDescriptionFile.getBytes();

		// Find the job requirement by jobId
		RequirementsModel requirement = requirementsDao.findById(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement not found with Job Id: " + jobId));

		// Save the byte array (job description) into the database (BLOB column)
		requirement.setJobDescriptionBlob(jobDescriptionBytes);

		// Save the updated requirement back to the database
		requirementsDao.save(requirement);

		return jobDescriptionBytes;  // Return the byte array if needed
	}


	// Update the sendEmailsToRecruiters method to handle comma-separated string
	public void sendEmailsToRecruiters(RequirementsModel model) {
		logger.info("Starting email sending process for job ID: {}", model.getJobId());

		try {
			Set<String> recruiterIds = model.getRecruiterIds();
			String assignedBy = model.getAssignedBy();

			Set<String> sentEmails = new HashSet<>(); // To prevent duplicate emails

			if (recruiterIds == null || recruiterIds.isEmpty()) {
				logger.warn("No recruiter IDs found for job ID: {}", model.getJobId());
			} else {
				logger.info("Found {} recruiters to send emails to for job ID: {}", recruiterIds.size(), model.getJobId());

				for (String recruiterId : recruiterIds) {
					try {
						String cleanedRecruiterId = cleanRecruiterId(recruiterId);
						logger.debug("Processing recruiter ID: {}", cleanedRecruiterId);

						boolean isUpdate = model.getUpdatedAt() != null &&
								model.getRequirementAddedTimeStamp() != null &&
								!model.getUpdatedAt().isEqual(model.getRequirementAddedTimeStamp());

						Tuple userTuple = requirementsDao.findUserEmailAndUsernameByUserId(cleanedRecruiterId);
						if (userTuple != null) {
							String recruiterEmail = userTuple.get(0, String.class);
							String recruiterName = userTuple.get(1, String.class);

							if (recruiterEmail != null && !recruiterEmail.isEmpty() && sentEmails.add(recruiterEmail)) {
								String status = model.getStatus() != null ? model.getStatus().toLowerCase() : "inprogress";
								String subject, text;

								if (isUpdate) {
									subject = "Requirement Updated: " + model.getJobTitle();
									text = constructUpdatedEmailBody(model, recruiterName);
								} else {
									switch (status) {
										case "hold":
											subject = "Job On Hold: " + model.getJobTitle();
											text = constructHoldEmailBody(model, recruiterName);
											break;
										case "closed":
											subject = "Job Closed: " + model.getJobTitle();
											text = constructClosedEmailBody(model, recruiterName);
											break;
										default:
											subject = "New Job Assignment: " + model.getJobTitle();
											text = constructEmailBody(model, recruiterName);
											break;
									}
								}

								emailService.sendEmail(recruiterEmail, subject, text);
								logger.info("Email sent to recruiter: {} <{}>", recruiterName, recruiterEmail);
							}
						} else {
							logger.warn("No user info found for recruiter ID: {}", recruiterId);
						}
					} catch (Exception e) {
						logger.error("Error processing recruiter {}: {}", recruiterId, e.getMessage(), e);
					}
				}
			}

			// ✅ Send email to Team Lead (assignedBy)
			if (assignedBy != null && !assignedBy.isEmpty()) {
				try {
					Tuple teamLeadTuple = requirementsDao.findUserEmailAndUsernameByAssignedBy(assignedBy);
					if (teamLeadTuple != null) {
						String leadEmail = teamLeadTuple.get(0, String.class);
						String leadName = teamLeadTuple.get(1, String.class);

						if (leadEmail != null && !leadEmail.isEmpty() && sentEmails.add(leadEmail)) {
							String subject = "Team Assignment Notification: " + model.getJobTitle();
							String text = constructTeamLeadEmailBodyByStatus(model, leadName);
							emailService.sendEmail(leadEmail, subject, text);
							logger.info("Email sent to Team Lead: {} <{}>", leadName, leadEmail);
						}
					} else {
						logger.warn("No user info found for Team Lead: {}", assignedBy);
					}
				} catch (Exception e) {
					logger.error("Failed to send email to Team Lead ({}): {}", assignedBy, e.getMessage(), e);
				}
			}

			logger.info("Completed email sending process for job ID: {}", model.getJobId());
		} catch (Exception e) {
			logger.error("Critical error in sending emails for job ID: {}: {}", model.getJobId(), e.getMessage(), e);
			throw new RuntimeException("Error in sending emails: " + e.getMessage(), e);
		}
	}

	private String constructTeamLeadEmailBodyByStatus(RequirementsModel model, String leadName) {
		String status = model.getStatus() != null ? model.getStatus().toLowerCase() : "inprogress";
		switch (status) {
			case "hold":
				return constructTeamLeadHoldEmail(model, leadName);
			case "closed":
				return constructTeamLeadClosedEmail(model, leadName);
			default:
				return constructTeamLeadInProgressEmail(model, leadName);
		}
	}


	private String constructTeamLeadInProgressEmail(RequirementsModel model, String leadName) {
		return "Dear " + leadName + ",<br><br>" +
				"You have successfully assigned the following requirement to your team.<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br>" +
				"<b>Status:</b> " + model.getStatus() + "<br><br>" +
				"Recruiters have been notified about this requirement.<br><br>" +
				"Regards,<br>Dataquad";
	}

	private String constructTeamLeadHoldEmail(RequirementsModel model, String leadName) {
		return "Dear " + leadName + ",<br><br>" +
				"The following job requirement you assigned is currently on hold:<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br><br>" +
				"Your team has been notified.<br><br>" +
				"Regards,<br>Dataquad";
	}

	private String constructTeamLeadClosedEmail(RequirementsModel model, String leadName) {
		return "Dear " + leadName + ",<br><br>" +
				"This is to inform you that the following requirement you assigned has been <b>closed</b>:<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br><br>" +
				"Recruiters have been notified. No further submissions are needed.<br><br>" +
				"Regards,<br>Dataquad";
	}




	// Update constructEmailBody method to use recruiterName instead of fetching separately
	private String constructEmailBody(RequirementsModel model, String recruiterName) {
		String status = model.getStatus() != null ? model.getStatus().toLowerCase() : "inprogress";
		switch (status) {
			case "hold":
				return constructHoldEmailBody(model, recruiterName);
			case "closed":
				return constructClosedEmailBody(model, recruiterName);
			default:
				return constructInProgressEmailBody(model, recruiterName);
		}
	}

	private String constructInProgressEmailBody(RequirementsModel model, String recruiterName) {
		return "Dear " + recruiterName + ",<br><br>" +
				"You have been assigned a new job requirement. Please find the details below:<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br>" +
				"<b>Job Type:</b> " + model.getJobType() + "<br>" +
				"<b>Experience Required:</b> " + model.getExperienceRequired() + " years<br>" +
				"<b>Assigned By:</b> " + model.getAssignedBy() + "<br><br>" +
				"Please start working on this requirement immediately. Check your dashboard for more details.<br><br>" +
				"Regards,<br>Dataquad";
	}

	private String constructHoldEmailBody(RequirementsModel model, String recruiterName) {
		return "Dear " + recruiterName + ",<br><br>" +
				"The following job requirement is currently on hold:<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br><br>" +
				"No further action is required at this moment. You will be notified once it's resumed.<br><br>" +
				"Regards,<br>Dataquad";
	}

	private String constructClosedEmailBody(RequirementsModel model, String recruiterName) {
		return "Dear " + recruiterName + ",<br><br>" +
				"This is to inform you that the following job requirement has been <b>closed</b>:<br><br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br><br>" +
				"No further submissions are needed. Thank you for your efforts.<br><br>" +
				"Regards,<br>Dataquad";
	}

	private String constructUpdatedEmailBody(RequirementsModel model, String recruiterName) {
		return "Dear " + recruiterName + ",<br><br>" +
				"The job requirement assigned to you has been <b>updated</b>. Please find the latest details below:<br><br>" +
				"<b>Job Id:</b> " + model.getJobId() + "<br>" +
				"<b>Job Title:</b> " + model.getJobTitle() + "<br>" +
				"<b>Client:</b> " + model.getClientName() + "<br>" +
				"<b>Location:</b> " + model.getLocation() + "<br>" +
				"<b>Job Type:</b> " + model.getJobType() + "<br>" +
				"<b>Experience Required:</b> " + model.getExperienceRequired() + " years<br>" +
				"<b>Assigned By:</b> " + model.getAssignedBy() + "<br><br>" +
				"Kindly check your dashboard for full updates and proceed accordingly.<br><br>" +
				"Regards,<br>Dataquad";
	}



	public String processJobDescriptionFile(MultipartFile jobDescriptionFile) throws IOException {
		// Check if the file is empty
		if (jobDescriptionFile.isEmpty()) {
			throw new IOException("File is empty.");
		}

		// Get file extension to determine file type
		String fileName = jobDescriptionFile.getOriginalFilename();
		String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

		switch (fileExtension) {
			case "pdf":
				return extractTextFromPdf(jobDescriptionFile);  // Return extracted text
			case "docx":
				return extractTextFromDocx(jobDescriptionFile);  // Return extracted text
			case "txt":
				return extractTextFromTxt(jobDescriptionFile);  // Return extracted text
			case "jpg":
			case "jpeg":
			case "png":
			case "gif":
				return convertImageToBase64(jobDescriptionFile);  // Convert image to base64 (instead of saving locally)
			default:
				throw new IOException("Unsupported file type.");
		}
	}

	// Method to extract text from PDF file
	private String extractTextFromPdf(MultipartFile file) throws IOException {
		PDDocument document = PDDocument.load(file.getInputStream());
		PDFTextStripper stripper = new PDFTextStripper();
		String text = stripper.getText(document);
		document.close();
		return text;  // Return extracted text from PDF
	}

	// Method to extract text from DOCX file
	private String extractTextFromDocx(MultipartFile file) throws IOException {
		XWPFDocument docx = new XWPFDocument(file.getInputStream());
		StringBuilder text = new StringBuilder();
		docx.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));
		return text.toString();  // Return extracted text from DOCX
	}

	// Method to extract text from TXT file
	private String extractTextFromTxt(MultipartFile file) throws IOException {
		InputStream inputStream = file.getInputStream();
		String text = new String(inputStream.readAllBytes());
		return text;  // Return the text content of the TXT file
	}

	// Method to convert image to base64 (instead of saving it locally)
	private String convertImageToBase64(MultipartFile file) throws IOException {
		byte[] imageBytes = file.getBytes();
		return Base64.getEncoder().encodeToString(imageBytes);  // Return base64 encoded image
	}


	public Object getRequirementsDetails() {
		// 1. Get the first and last date of the current month
		LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
		LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

		// 2. Fetch data from repository
		List<RequirementsModel> requirementsList =
				requirementsDao.findByRequirementAdded();

		logger.info("Fetched no of Requirements {}", requirementsList.size());

		// 3. Convert to DTOs
		List<RequirementsDto> dtoList = requirementsList.stream()
				.map(requirement -> {
					RequirementsDto dto = new RequirementsDto();
					List<String> recruiterNames = requirementsDao.findRecruiterNamesByJobId(requirement.getJobId());

					dto.setJobId(requirement.getJobId());
					dto.setJobTitle(requirement.getJobTitle());
					dto.setClientName(requirement.getClientName());
					dto.setJobDescription(requirement.getJobDescription());
					dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());
					dto.setJobType(requirement.getJobType());
					dto.setLocation(requirement.getLocation());
					dto.setJobMode(requirement.getJobMode());
					dto.setExperienceRequired(requirement.getExperienceRequired());
					dto.setNoticePeriod(requirement.getNoticePeriod());
					dto.setRelevantExperience(requirement.getRelevantExperience());
					dto.setQualification(requirement.getQualification());
					dto.setSalaryPackage(requirement.getSalaryPackage());
					dto.setNoOfPositions(requirement.getNoOfPositions());
					dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
					dto.setRecruiterIds(requirement.getRecruiterIds());
					dto.setStatus(requirement.getStatus());
					dto.setRecruiterName(new HashSet<>(recruiterNames));
					dto.setAssignedBy(requirement.getAssignedBy());
					dto.setUpdatedAt(requirement.getUpdatedAt());

					// Submissions and Interviews
					String jobId = requirement.getJobId();
					dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(jobId));
					dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(jobId));

					// No need to manually set age anym
					return dto;
				})
				.collect(Collectors.toList());

		// 4. Return appropriate response
		if (dtoList.isEmpty()) {
			return new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Requirements Not Found", LocalDateTime.now());
		} else {
			return dtoList;
		}
	}

	public PagedResponse<RequirementsDto> getRequirementsWithPaginationAndSearch(int page, int size, String search) {
		// Get current month date range
		LocalDate[] dateRange = getCurrentMonthDateRange();
		LocalDate startDate = dateRange[0];
		LocalDate endDate = dateRange[1];
		
		// Create Pageable object
		Pageable pageable = PageRequest.of(page, size);
		
		// Fetch paginated data from repository
		Page<RequirementsModel> requirementsPage = requirementsDao.findByRequirementAddedWithSearchPageable(startDate, endDate, search, pageable);
		
		// Convert to DTOs
		List<RequirementsDto> dtoList = requirementsPage.getContent().stream()
				.map(this::mapToRequirementsDto)
				.collect(Collectors.toList());
		
		logger.info("Fetched {} requirements (page {}, size {}, search: '{}')", dtoList.size(), page, size, search);
		
		return new PagedResponse<>(dtoList, page, size, requirementsPage.getTotalElements());
	}

	private RequirementsDto mapToRequirementsDto(RequirementsModel requirement) {
		RequirementsDto dto = new RequirementsDto();
		List<String> recruiterNames = requirementsDao.findRecruiterNamesByJobId(requirement.getJobId());

		dto.setJobId(requirement.getJobId());
		dto.setJobTitle(requirement.getJobTitle());
		dto.setClientName(requirement.getClientName());
		dto.setJobDescription(requirement.getJobDescription());
		dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());
		dto.setJobType(requirement.getJobType());
		dto.setLocation(requirement.getLocation());
		dto.setJobMode(requirement.getJobMode());
		dto.setExperienceRequired(requirement.getExperienceRequired());
		dto.setNoticePeriod(requirement.getNoticePeriod());
		dto.setRelevantExperience(requirement.getRelevantExperience());
		dto.setQualification(requirement.getQualification());
		dto.setSalaryPackage(requirement.getSalaryPackage());
		dto.setNoOfPositions(requirement.getNoOfPositions());
		dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
		dto.setRecruiterIds(requirement.getRecruiterIds());
		dto.setStatus(requirement.getStatus());
		dto.setRecruiterName(new HashSet<>(recruiterNames));
		dto.setAssignedBy(requirement.getAssignedBy());
		dto.setUpdatedAt(requirement.getUpdatedAt());

		// Submissions and Interviews
		String jobId = requirement.getJobId();
		dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(jobId));
		dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(jobId));

		return dto;
	}

	public LocalDate[] getCurrentMonthDateRange() {
		LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
		LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
		return new LocalDate[]{startOfMonth, endOfMonth};
	}


	public List<RequirementsDto> getRequirementsByDateRange(LocalDate startDate, LocalDate endDate) {

		// 💥 Second check: End date must not be before start date
		if (endDate.isBefore(startDate)) {
			throw new DateRangeValidationException("End date cannot be before start date.");
		}


		List<RequirementsModel> requirements = requirementsDao.findByRequirementAddedTimeStampBetween(startDate, endDate);

		if (requirements.isEmpty()) {
			throw new RequirementNotFoundException("No requirements found between " + startDate + " and " + endDate);
		}

		// ✅ Add logger here since this block is guaranteed to have results
		Logger logger = LoggerFactory.getLogger(RequirementsService.class);
		logger.info("✅ Fetched {} requirements between {} and {}", requirements.size(), startDate, endDate);

		return requirements.stream().map(requirement -> {
			List<String> recruiterNames = requirementsDao.findRecruiterNamesByJobId(requirement.getJobId());

			RequirementsDto dto = new RequirementsDto();
			dto.setJobId(requirement.getJobId());
			dto.setJobTitle(requirement.getJobTitle());
			dto.setClientName(requirement.getClientName());
			dto.setJobDescription(requirement.getJobDescription());
			dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());
			dto.setJobType(requirement.getJobType());
			dto.setLocation(requirement.getLocation());
			dto.setJobMode(requirement.getJobMode());
			dto.setExperienceRequired(requirement.getExperienceRequired());
			dto.setNoticePeriod(requirement.getNoticePeriod());
			dto.setRelevantExperience(requirement.getRelevantExperience());
			dto.setQualification(requirement.getQualification());
			dto.setSalaryPackage(requirement.getSalaryPackage());
			dto.setNoOfPositions(requirement.getNoOfPositions());
			dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
			dto.setRecruiterIds(requirement.getRecruiterIds());
			dto.setStatus(requirement.getStatus());
			dto.setRecruiterName(new HashSet<>(recruiterNames));
			dto.setAssignedBy(requirement.getAssignedBy());

			// Stats
			String jobId = requirement.getJobId();
			dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(jobId));
			dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(jobId));

			return dto;
		}).collect(Collectors.toList());
	}


	public RequirementsDto getRequirementDetailsById(String jobId) {
		RequirementsModel requirement = requirementsDao.findById(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id : " + jobId));
		return modelMapper.map(requirement, RequirementsDto.class);
	}

	@Transactional
	public Object assignToRecruiter(String jobId, Set<String> recruiterIds) {
		// Fetch the existing requirement by jobId
		RequirementsModel requirement = requirementsDao.findById(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id : " + jobId));

		// Get current recruiter IDs
		Set<String> cleanedRecruiterIds = recruiterIds.stream()
				.map(this::cleanRecruiterId)
				.collect(Collectors.toSet());

		// Check if the recruiter is already assigned to the job
		if (cleanedRecruiterIds.contains(recruiterIds)) {
			return new ErrorResponse(HttpStatus.CONFLICT.value(),
					"Requirement Already Assigned to Recruiter : " + cleanedRecruiterIds, LocalDateTime.now());
		}

		// Add new recruiter ID
		requirement.setRecruiterIds(cleanedRecruiterIds);

		// Save the recruiter relationship in the database
		RequirementsModel jobRecruiterEmail = new RequirementsModel();
		jobRecruiterEmail.setJobId(cleanedRecruiterIds.toString());
		jobRecruiterEmail.setRecruiterIds(cleanedRecruiterIds);

		// Save the updated requirement in the DB
		requirementsDao.save(requirement);

		// Send email to the recruiter using the existing method
		sendEmailsToRecruiters(requirement);

		return new AssignRecruiterResponse(jobId, String.join(",", cleanedRecruiterIds));
	}


	@Transactional
	public void statusUpdate(StatusDto status) {
		RequirementsModel requirement = requirementsDao.findById(status.getJobId()).orElseThrow(
				() -> new RequirementNotFoundException("Requirement Not Found with Id : " + status.getJobId()));
		requirement.setStatus(status.getStatus());
//        requirement.setRemark(status.getRemark());  // If you are using remark, set it here
		requirementsDao.save(requirement);
	}

	public List<RecruiterRequirementsDto> getJobsAssignedToRecruiter(String recruiterId) {
		// 📅 Calculate current month start and end
		LocalDate today = LocalDate.now();
		LocalDateTime startDateTime = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime endDateTime = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

		// 🔍 Fetch jobs for recruiter within current month
		List<RequirementsModel> jobsByRecruiterId = requirementsDao.findJobsByRecruiterId(
				recruiterId, startDateTime, endDateTime);
		logger.info("Number of Requirements {}", jobsByRecruiterId.size());


		// 🔁 Map to DTOs
		return jobsByRecruiterId.stream()
				.map(job -> {
					RecruiterRequirementsDto dto = modelMapper.map(job, RecruiterRequirementsDto.class);
					dto.setAssignedBy(job.getAssignedBy());
					return dto;
				})
				.collect(Collectors.toList());
	}


	public List<RecruiterRequirementsDto> getJobsAssignedToRecruiterByDate(String recruiterId, LocalDate startDate, LocalDate endDate) {

		// 💥 First check: Null check for input dates
		if (startDate == null || endDate == null) {
			throw new DateRangeValidationException("Start date and End date must not be null.");
		}

		// 💥 Second check: End date must not be before start date
		if (endDate.isBefore(startDate)) {
			throw new DateRangeValidationException("End date cannot be before start date.");
		}

		// 💥 Convert LocalDate to LocalDateTime for comparison
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

		// 🔍 Fetch jobs assigned to recruiter within the date range
		List<RequirementsModel> jobs = requirementsDao.findJobsByRecruiterIdAndDateRange(recruiterId, startDateTime, endDateTime);

		// 💥 Fourth check: No jobs found
		if (jobs.isEmpty()) {
			throw new NoJobsAssignedToRecruiterException("No Jobs Assigned To Recruiter: " + recruiterId +
					" between " + startDate + " and " + endDate);
		}

		// ✅ Add logger here since this block is guaranteed to have results
		Logger logger = LoggerFactory.getLogger(RequirementsService.class);
		logger.info("✅ Fetched {} jobs assigned to recruiter {} between {} and {}", jobs.size(), recruiterId, startDate, endDate);

		// 🔄 Map entities to DTOs
		return jobs.stream()
				.map(job -> {
					RecruiterRequirementsDto dto = modelMapper.map(job, RecruiterRequirementsDto.class);
					dto.setAssignedBy(job.getAssignedBy()); // Ensure assignedBy is mapped
					return dto;
				})
				.collect(Collectors.toList());
	}


	@Transactional
	public ResponseBean updateRequirementDetails(RequirementsDto requirementsDto) {
		try {
			// Fetch the existing requirement by jobId
			RequirementsModel existingRequirement = requirementsDao.findById(requirementsDto.getJobId())
					.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id : " + requirementsDto.getJobId()));

			// Log before update
			logger.info("Before update: " + existingRequirement);

			// Update the existing requirement with the new details from the DTO
			if (requirementsDto.getJobTitle() != null) existingRequirement.setJobTitle(requirementsDto.getJobTitle());
			if (requirementsDto.getClientName() != null)
				existingRequirement.setClientName(requirementsDto.getClientName());

			// Handle job description: either text or file
			if (requirementsDto.getJobDescription() != null && !requirementsDto.getJobDescription().isEmpty()) {
				existingRequirement.setJobDescription(requirementsDto.getJobDescription());  // Set text-based description
				existingRequirement.setJobDescriptionBlob(null);  // Nullify the BLOB if text is provided
			}

			// If a file for job description is provided, set it as BLOB and nullify the text description
			if (requirementsDto.getJobDescriptionFile() != null && !requirementsDto.getJobDescriptionFile().isEmpty()) {
				byte[] jobDescriptionBytes = saveJobDescriptionFileAsBlob(requirementsDto.getJobDescriptionFile(), requirementsDto.getJobId());
				existingRequirement.setJobDescriptionBlob(jobDescriptionBytes);  // Set the BLOB field
				existingRequirement.setJobDescription(null);  // Nullify the text-based description
			}

			// If the jobDescriptionFile is null, but jobDescriptionBlob is updated, update the BLOB
			if (requirementsDto.getJobDescriptionFile() == null && requirementsDto.getJobDescriptionBlob() != null) {
				existingRequirement.setJobDescriptionBlob(requirementsDto.getJobDescriptionBlob());  // Set the BLOB field
				existingRequirement.setJobDescription(null);  // Nullify the text-based description
			}

			// Set other fields
			existingRequirement.setJobType(requirementsDto.getJobType());
			existingRequirement.setLocation(requirementsDto.getLocation());
			existingRequirement.setJobMode(requirementsDto.getJobMode());
			existingRequirement.setExperienceRequired(requirementsDto.getExperienceRequired());
			existingRequirement.setNoticePeriod(requirementsDto.getNoticePeriod());
			existingRequirement.setRelevantExperience(requirementsDto.getRelevantExperience());
			existingRequirement.setQualification(requirementsDto.getQualification());
			existingRequirement.setSalaryPackage(requirementsDto.getSalaryPackage());
			existingRequirement.setNoOfPositions(requirementsDto.getNoOfPositions());
			existingRequirement.setRecruiterIds(requirementsDto.getRecruiterIds());
			if (StringUtils.hasText(requirementsDto.getAssignedTo())) {
				existingRequirement.setAssignedBy(requirementsDto.getAssignedTo());
			} else {
				existingRequirement.setAssignedBy(requirementsDto.getAssignedBy());
			}
			existingRequirement.setUpdatedAt(LocalDateTime.now());
			if (requirementsDto.getStatus() != null) existingRequirement.setStatus(requirementsDto.getStatus());

			if (requirementsDto.getStatus() != null) {
				existingRequirement.setStatus(requirementsDto.getStatus());

				// ✅ If requirement is marked as CLOSED → trigger bench import
				if ("closed".equalsIgnoreCase(requirementsDto.getStatus())) {
					try {
						String jobId = requirementsDto.getJobId();

						// 1. Fetch candidates for jobId
						String fetchUrl = "https://mymulya.com/candidate/closedjobs/" + jobId;

						ResponseEntity<List<Map<String, Object>>> fetchResponse = restTemplate.exchange(
								fetchUrl,
								HttpMethod.GET,
								null,
								new ParameterizedTypeReference<List<Map<String, Object>>>() {}
						);

						List<Map<String, Object>> candidateList = fetchResponse.getBody();

						if (candidateList != null && !candidateList.isEmpty()) {
							// 2. Post candidates to /bench/import
							String benchUrl = "https://mymulya.com/candidate/bench/import";

							HttpHeaders headers = new HttpHeaders();
							headers.setContentType(MediaType.APPLICATION_JSON);

							HttpEntity<List<Map<String, Object>>> benchRequest = new HttpEntity<>(candidateList, headers);

							ResponseEntity<String> benchResponse = restTemplate.postForEntity(benchUrl, benchRequest, String.class);

							logger.info("Bench import response for jobId {}: {}", jobId, benchResponse.getBody());
						} else {
							logger.warn("No candidates found for closed jobId: {}", jobId);
						}
					} catch (Exception ex) {
						logger.error("Failed to push candidates to bench for jobId: {}", requirementsDto.getJobId(), ex);
					}
				}
			}
			// Save the updated requirement to the database
			requirementsDao.save(existingRequirement);

			// Log after update
			logger.info("After update: " + existingRequirement);

			// Send emails to recruiters (after the requirement has been successfully updated)
			sendEmailsToRecruiters(existingRequirement); // Assuming this method handles the sending of emails to recruiters

			// Return success response
			return new ResponseBean(true, "Updated Successfully", null, null);
		} catch (Exception e) {
			logger.error("Error updating requirement", e.getMessage());
			return new ResponseBean(false, "Error updating requirement" + e.getMessage(), "Internal Server Error", null);
		}
	}


	@Transactional
	public ResponseBean deleteRequirementDetails(String jobId) {
		// Fetch the existing requirement by jobId
		RequirementsModel existingRequirement = requirementsDao.findById(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id : " + jobId));

		// Delete the requirement from the database
		requirementsDao.delete(existingRequirement);

		// Return a response indicating successful deletion
		return new ResponseBean(true, "Deleted Successfully", null, new DataResponse(jobId));
	}

	public String getRecruiterName(String recruiterId) {
		String cleanedRecruiterId = recruiterId.trim().replace("\"", "");

		try {
			logger.info("Fetching recruiter for ID: {}", cleanedRecruiterId);

			Tuple userTuple = requirementsDao.findUserEmailAndUsernameByUserId(cleanedRecruiterId);

			if (userTuple != null) {
				logger.info("Tuple found: {}", userTuple);
				return userTuple.get("user_name", String.class);
			} else {
				logger.warn("No recruiter found with ID: {}", cleanedRecruiterId);
				return null;
			}
		} catch (Exception e) {
			logger.error("Error fetching recruiter username", e.getMessage());
			throw new RuntimeException("Error fetching recruiter username" + e.getMessage(), e);
		}
	}

	public List<RecruiterInfoDto> getRecruitersForJob(String jobId) {
		// Get the requirement with the given job ID
		RequirementsModel requirement = requirementsDao.findById(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id: " + jobId));

		// Log raw recruiter IDs
		System.out.println("Raw Recruiter IDs: " + requirement.getRecruiterIds());

		// Extract recruiter IDs properly
		Set<String> recruiterIds = requirement.getRecruiterIds().stream()
				.map(String::trim)  // Trim any spaces
				.filter(id -> !id.isEmpty())  // Remove empty strings
				.collect(Collectors.toSet());

		// Log cleaned recruiter IDs
		System.out.println("Cleaned Recruiter IDs: " + recruiterIds);

		// Fetch recruiter details for each recruiter ID
		List<RecruiterInfoDto> recruiters = new ArrayList<>();
		for (String recruiterId : recruiterIds) {
			Tuple userTuple = requirementsDao.findUserEmailAndUsernameByUserId(recruiterId);

			// Log recruiter fetching status
			System.out.println("Recruiter ID: " + recruiterId + ", Found in DB: " + (userTuple != null));

			if (userTuple != null) {
				String recruiterName = userTuple.get(1, String.class);  // Assuming username is at index 1
				recruiters.add(new RecruiterInfoDto(recruiterId, recruiterName));
			} else {
				System.err.println("Recruiter not found in DB for ID: " + recruiterId);
			}
		}

		return recruiters;
	}

	public RequirementDetailsDto getRequirementDetailsByJobId(String jobId) {
		// Fetch the requirement
		RequirementsModel requirement = requirementsDao.findByJobId(jobId)
				.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id: " + jobId));

		// Manually convert to DTO
		RequirementsDto requirementsDto = mapToDto(requirement);

		// Fetch submitted candidates
		List<Tuple> submittedCandidatesList = requirementsDao.findCandidatesByJobId(jobId);
		List<CandidateDto> submittedCandidates = mapCandidates(submittedCandidatesList);

		// Fetch interview scheduled candidates
		List<Tuple> interviewCandidatesList = requirementsDao.findInterviewScheduledCandidatesByJobId(jobId);
		List<InterviewCandidateDto> interviewScheduledCandidates = mapInterviewCandidates(interviewCandidatesList);

		// Fetch placements
		List<Tuple> placementsList = requirementsDao.findPlacementsByJobId(jobId);
		List<PlacementDto> placements = mapPlacements(placementsList);

		// Set submission and interview counts
		requirementsDto.setNumberOfSubmissions(submittedCandidates.size());
		requirementsDto.setNumberOfInterviews(interviewScheduledCandidates.size());

		// Build final DTO
		return new RequirementDetailsDto(requirementsDto, submittedCandidates, interviewScheduledCandidates, placements);
	}

	private RequirementsDto mapToDto(RequirementsModel requirement) {
		RequirementsDto dto = new RequirementsDto();

		dto.setJobId(requirement.getJobId());
		dto.setJobTitle(requirement.getJobTitle());
		dto.setClientName(requirement.getClientName());
		dto.setJobDescription(requirement.getJobDescription());
		dto.setJobType(requirement.getJobType());
		dto.setLocation(requirement.getLocation());
		dto.setJobMode(requirement.getJobMode());
		dto.setExperienceRequired(requirement.getExperienceRequired());
		dto.setNoticePeriod(requirement.getNoticePeriod());
		dto.setRelevantExperience(requirement.getRelevantExperience());
		dto.setQualification(requirement.getQualification());
		dto.setSalaryPackage(requirement.getSalaryPackage());
		dto.setNoOfPositions(requirement.getNoOfPositions());
		dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
		dto.setRecruiterIds(requirement.getRecruiterIds());
		dto.setStatus(requirement.getStatus());
		dto.setAssignedBy(requirement.getAssignedBy());
		dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());

		return dto;
	}


	/**
	 * Maps Tuple data to CandidateDto objects, including recruiter name.
	 */
	private List<CandidateDto> mapCandidates(List<Tuple> candidatesList) {
		return candidatesList.stream()
				.map(candidate -> {
					try {
						// Retrieve recruiter name from the Tuple
						String recruiterName = candidate.get("recruiterName", String.class);

						return new CandidateDto(
								getTupleValue(candidate, "candidate_id"),
								getTupleValue(candidate, "full_name"),
								getTupleValue(candidate, "candidate_email_id"),
								getTupleValue(candidate, "contact_number"),
								getTupleValue(candidate, "qualification"),
								getTupleValue(candidate, "skills"),
								getTupleValue(candidate, "overall_feedback"),
								recruiterName  // Add recruiter name
						);
					} catch (Exception e) {
						System.err.println("Error mapping candidate: " + candidate + " | Exception: " + e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)  // Remove any null entries
				.collect(Collectors.toList());
	}


	/**
	 * Maps Tuple data to InterviewCandidateDto objects, including recruiter name.
	 */
	private List<InterviewCandidateDto> mapInterviewCandidates(List<Tuple> interviewList) {
		return interviewList.stream()
				.map(interview -> {
					try {
						// Retrieve recruiter name from the Tuple
						String recruiterName = interview.get("recruiterName", String.class);

						// Format interview date-time if available
						Timestamp interviewTimestamp = interview.get("interviewDateTime", Timestamp.class);
						String interviewDateTime = interviewTimestamp != null
								? interviewTimestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
								: null;

						// Retrieve additional fields from the interview details table
						String candidateName = interview.get("candidateName", String.class);
						String email = interview.get("email", String.class);
						String interviewLevel = interview.get("interviewLevel", String.class);

						return new InterviewCandidateDto(
								getTupleValue(interview, "candidate_id"),  // Mapping candidateId
								candidateName,                             // Mapping candidateName
								email,                                     // Mapping email
								interviewLevel,                            // Mapping interviewLevel
								interviewDateTime,                         // Mapping interviewDateTime
								recruiterName                              // Mapping recruiterName
						);
					} catch (Exception e) {
						System.err.println("Error mapping interview: " + interview + " | Exception: " + e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)  // Remove any null entries
				.collect(Collectors.toList());
	}


	/**
	 * Maps Tuple data to PlacementDto objects.
	 */
	private List<PlacementDto> mapPlacements(List<Tuple> placementsList) {
		return placementsList.stream()
				.map(placement -> {
					try {
						// Retrieve recruiter name from the Tuple
						String recruiterName = placement.get("recruiterName", String.class);

						// Retrieve other fields from the Tuple
						String candidateName = placement.get("candidateName", String.class);
						String email = placement.get("email", String.class);
						String contactNumber = placement.get("contactNumber", String.class);
						String qualification = placement.get("qualification", String.class);
						String overallFeedback = placement.get("overallFeedback", String.class);

						return new PlacementDto(
								getTupleValue(placement, "candidate_id"),  // Mapping candidateId
								candidateName,                             // Mapping candidateName
								email,                                     // Mapping email
								contactNumber,                             // Mapping contactNumber
								qualification,                             // Mapping qualification
								overallFeedback,                           // Mapping overallFeedback
								recruiterName                              // Mapping recruiterName
						);
					} catch (Exception e) {
						System.err.println("Error mapping placement: " + placement + " | Exception: " + e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)  // Remove any null entries
				.collect(Collectors.toList());
	}


	/**
	 * Safely retrieves values from Tuple, handling null cases.
	 */
	private String getTupleValue(Tuple tuple, String columnName) {
		try {
			return tuple.get(columnName, String.class);
		} catch (Exception e) {
			System.err.println("Error fetching column: " + columnName + " | Exception: " + e.getMessage());
			return null;
		}
	}

	public CandidateStatsResponse getCandidateStatsLast30Days() {

		LocalDate endDate = LocalDate.now();
		LocalDate startDate = LocalDate.now().withDayOfMonth(1); // First day of this month


		List<UserStatsDTO> userStatsList = new ArrayList<>();

		// 👤 Employee Stats with last 30 days filter
		List<Tuple> employeeStats = requirementsDao.getEmployeeCandidateStats(startDate, endDate);
		userStatsList.addAll(employeeStats.stream()
				.map(tuple -> {
					UserStatsDTO dto = new UserStatsDTO();
					dto.setEmployeeId(tuple.get("employeeId", String.class));
					dto.setEmployeeName(tuple.get("employeeName", String.class));
					dto.setEmployeeEmail(tuple.get("employeeEmail", String.class));
					dto.setRole("Employee");

					dto.setNumberOfClients(convertToInt(tuple.get("numberOfClients")));
					dto.setNumberOfRequirements(convertToInt(tuple.get("numberOfRequirements")));
					dto.setNumberOfSubmissions(convertToInt(tuple.get("numberOfSubmissions")));
					dto.setNumberOfScreenRejects(convertToInt(tuple.get("numberOfScreenRejects")));
					dto.setNumberOfInterviews(convertToInt(tuple.get("numberOfInterviews")));
					dto.setNumberOfPlacements(convertToInt(tuple.get("numberOfPlacements")));

					return dto;
				}).collect(Collectors.toList())
		);

		// 👨‍🏫 Teamlead Stats with last 30 days filter
		List<Tuple> teamleadStats = requirementsDao.getTeamleadCandidateStats(startDate, endDate);
		userStatsList.addAll(teamleadStats.stream()
				.map(tuple -> {
					UserStatsDTO dto = new UserStatsDTO();
					dto.setEmployeeId(tuple.get("employeeId", String.class));
					dto.setEmployeeName(tuple.get("employeeName", String.class));
					dto.setEmployeeEmail(tuple.get("employeeEmail", String.class));
					dto.setRole("Teamlead");

					dto.setNumberOfClients(convertToInt(tuple.get("numberOfClients")));
					dto.setNumberOfRequirements(convertToInt(tuple.get("numberOfRequirements")));
					dto.setSelfSubmissions(convertToInt(tuple.get("selfSubmissions")));
					dto.setSelfInterviews(convertToInt(tuple.get("selfInterviews")));
					dto.setSelfPlacements(convertToInt(tuple.get("selfPlacements")));
					dto.setTeamScreenRejectCount(convertToInt(tuple.get("teamScreenRejectCount")));
					dto.setTeamSubmissions(convertToInt(tuple.get("teamSubmissions")));
					dto.setTeamInterviews(convertToInt(tuple.get("teamInterviews")));
					dto.setTeamPlacements(convertToInt(tuple.get("teamPlacements")));

					return dto;
				}).collect(Collectors.toList())
		);

		// Log the summary info at the end
		System.out.println(String.format(
				"Fetched Employee and Teamlead Count from %s to %s. Total records retrieved: %d",
				startDate, endDate, userStatsList.size()
		));

		return new CandidateStatsResponse(userStatsList);
	}


	public List<Coordinator_DTO> getCoordinatorStats() {
		List<Tuple> tuples = requirementsDao.countInterviewsByStatus();
		List<Coordinator_DTO> dtoList = new ArrayList<>();

		for (Tuple tuple : tuples) {
			Coordinator_DTO dto = new Coordinator_DTO();

			dto.setEmployeeId(tuple.get("employeeId", String.class));
			dto.setEmployeeName(tuple.get("employeeName", String.class));
			dto.setEmployeeEmail(tuple.get("employeeEmail", String.class));

			dto.setGetTotalInterviews(convertToInt(tuple.get("totalInterviews")));
			dto.setTotalScheduled(convertToInt(tuple.get("scheduledInterviewsCount")));
			dto.setTotalRejected(convertToInt(tuple.get("rejectedInterviewsCount")));
			dto.setTotalSelected(convertToInt(tuple.get("selectedInterviewsCount")));

			dtoList.add(dto);
		}

		return dtoList;
	}

	private int convertToInt(Object value) {
		if (value instanceof BigInteger) {
			return ((BigInteger) value).intValue(); // MySQL COUNT() returns BigInteger
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal) value).intValue(); // Handles numeric decimal cases
		} else if (value instanceof Number) {
			return ((Number) value).intValue(); // Fallback for any numeric types
		} else {
			return 0; // Default to 0 if null or unknown type
		}
	}


	// Fetch both Submitted Candidates, Scheduled Interviews, and Employee Details in one call
	public CandidateResponseDTO getCandidateData(String userId) {
		log.info("🔍 Fetching candidate data for userId: {}", userId);

		// 1️⃣ Fetch role and username
		Tuple roleInfo = requirementsDao.getUserRoleAndUsername(userId);
		if (roleInfo == null) {
			log.warn("No role information found for user ID: {}", userId);
			// You can either throw a custom exception or return an empty response
			throw new ResourceNotFoundException("User role information not found for userId: " + userId);
		}
		String role = roleInfo.get("role", String.class);


		String username = roleInfo.get("userName", String.class);
		log.info("✅ Retrieved role '{}' and username '{}' for userId: {}", role, username, userId);

		LocalDate endDate = LocalDate.now();
		LocalDate startDate = LocalDate.now().withDayOfMonth(1); // First day of this month


		// 💥 First check: Null check for input dates
		if (startDate == null || endDate == null) {
			throw new DateRangeValidationException("Start date and End date must not be null.");
		}

		// 💥 Second check: End date must not be before start date
		if (endDate.isBefore(startDate)) {
			throw new DateRangeValidationException("End date cannot be before start date.");
		}


		List<SubmittedCandidateDTO> submittedCandidates;
		List<InterviewScheduledDTO> scheduledInterviews;
		List<JobDetailsDTO> jobDetails;
		List<PlacementDetailsDTO> placementDetails;
		List<ClientDetailsDTO> clientDetails;
		List<Tuple> employeeDetailsTuples;

		// 2️⃣ Fetch data based on role
		if ("Teamlead".equalsIgnoreCase(role)) {
			log.info("🧩 User is a Teamlead. Fetching data assigned by username: {}", username);
			submittedCandidates = requirementsDao.findSubmittedCandidatesByAssignedByAndDateRange(username, startDate, endDate);
			scheduledInterviews = requirementsDao.findScheduledInterviewsByAssignedByAndDateRange(username, startDate, endDate);
			jobDetails = requirementsDao.findJobDetailsByAssignedByAndDateRange(username, startDate, endDate);
			placementDetails = requirementsDao.findPlacementCandidatesByAssignedByAndDateRange(username, startDate, endDate);
			clientDetails = requirementsDao.findClientDetailsByAssignedByAndDateRange(username, startDate, endDate);
			employeeDetailsTuples = requirementsDao.getTeamleadDetailsByUserId(userId);
		} else {
			log.info("🧩 User is an individual contributor. Fetching data by userId: {}", userId);
			submittedCandidates = requirementsDao.findSubmittedCandidatesByUserIdAndDateRange(userId, startDate, endDate);
			scheduledInterviews = requirementsDao.findScheduledInterviewsByUserIdAndDateRange(userId, startDate, endDate);
			jobDetails = requirementsDao.findJobDetailsByUserIdAndDateRange(userId, startDate, endDate);
			placementDetails = requirementsDao.findPlacementCandidatesByUserIdAndDateRange(userId, startDate, endDate);
			clientDetails = requirementsDao.findClientDetailsByUserIdAndDateRange(userId, startDate, endDate);
			employeeDetailsTuples = requirementsDao.getEmployeeDetailsByUserId(userId);
		}

		// 3️⃣ Grouping the fetched data
		log.info("📦 Grouping submissions, interviews, placements, job details, and client details by client name");
		Map<String, List<SubmittedCandidateDTO>> groupedSubmissions = groupByClientName(submittedCandidates);
		Map<String, List<InterviewScheduledDTO>> groupedInterviews = groupByClientName(scheduledInterviews);
		Map<String, List<PlacementDetailsDTO>> groupedPlacements = groupByClientName(placementDetails);
		Map<String, List<JobDetailsDTO>> groupedJobDetails = groupByClientName(jobDetails);
		Map<String, List<ClientDetailsDTO>> groupedClientDetails = groupByClientName(clientDetails);

		// 4️⃣ Mapping employee details
		log.info("🛠️ Mapping employee details for userId: {}", userId);
		List<EmployeeDetailsDTO> employeeDetails = mapEmployeeDetailsTuples(employeeDetailsTuples);

		log.info("📆 Date Range: From {} to {}", startDate, endDate);

		// 🔢 Logging total counts
		log.info("📊 Total Submitted Candidates: {}", submittedCandidates.size());
		log.info("📊 Total Scheduled Interviews: {}", scheduledInterviews.size());
		log.info("📊 Total Job Details: {}", jobDetails.size());
		log.info("📊 Total Placement Details: {}", placementDetails.size());
		log.info("📊 Total Client Details: {}", clientDetails.size());
		log.info("📊 Total Employee Details: {}", employeeDetails.size());

		// 5️⃣ Return compiled DTO
		log.info("✅ Successfully fetched and compiled candidate data for userId: {}", userId);

		return new CandidateResponseDTO(
				groupedSubmissions, groupedInterviews, groupedPlacements,
				groupedJobDetails, groupedClientDetails, employeeDetails
		);
	}


	// Generic method to group a list by normalized client name
	private <T> Map<String, List<T>> groupByClientName(List<T> list) {
		return list.stream()
				.collect(Collectors.groupingBy(item -> normalizeClientName(getClientName(item)),
						LinkedHashMap::new, Collectors.toList()));
	}

	// Generic method to retrieve client name from various DTO types
	private <T> String getClientName(T item) {
		if (item instanceof ClientDetailsDTO) {
			return ((ClientDetailsDTO) item).getClientName();
		} else if (item instanceof SubmittedCandidateDTO) {
			return ((SubmittedCandidateDTO) item).getClientName();
		} else if (item instanceof InterviewScheduledDTO) {
			return ((InterviewScheduledDTO) item).getClientName();
		} else if (item instanceof PlacementDetailsDTO) {
			return ((PlacementDetailsDTO) item).getClientName();
		} else if (item instanceof JobDetailsDTO) {
			return ((JobDetailsDTO) item).getClientName();
		} else {
			return "";
		}
	}

	// Helper method to normalize the client name (simple toLowerCase as an example)
	private String normalizeClientName(String clientName) {
		return clientName != null ? clientName.toLowerCase() : "";
	}


	// Method to map Tuple to EmployeeDetailsDTO
	// Helper method to convert List<Tuple> to List<EmployeeDetailsDTO>
	private List<EmployeeDetailsDTO> mapEmployeeDetailsTuples(List<Tuple> employeeDetailsTuples) {
		List<EmployeeDetailsDTO> employeeDetails = new ArrayList<>();

		for (Tuple tuple : employeeDetailsTuples) {
			String joiningDateStr = tuple.get("joiningDate", String.class);
			String dobStr = tuple.get("dob", String.class);

			// Parsing date strings into LocalDate if not null or empty
			LocalDate joiningDate = joiningDateStr != null && !joiningDateStr.isEmpty() ? LocalDate.parse(joiningDateStr) : null;
			LocalDate dob = dobStr != null && !dobStr.isEmpty() ? LocalDate.parse(dobStr) : null;

			EmployeeDetailsDTO dto = new EmployeeDetailsDTO(
					tuple.get("employeeId", String.class),
					tuple.get("employeeName", String.class),
					tuple.get("role", String.class),
					tuple.get("employeeEmail", String.class),
					tuple.get("designation", String.class),
					joiningDate,  // use correct type
					tuple.get("gender", String.class),
					dob,           // use correct type
					tuple.get("phoneNumber", String.class),
					tuple.get("personalEmail", String.class),
					tuple.get("status", String.class)
			);
			employeeDetails.add(dto);
		}
		return employeeDetails;
	}


	// helper method to check if alias exists in tuple
	private boolean hasAlias(Tuple tuple, String alias) {
		return tuple.getElements().stream().anyMatch(e -> alias.equalsIgnoreCase(e.getAlias()));
	}

	public List<RequirementsDto> getRequirementsByAssignedBy(String userId) {
		// 1. Check if the user exists
		int userExists = requirementsDao.countByUserId(userId);
		if (userExists == 0) {
			logger.warn("User ID '{}' not found in the database", userId);
			throw new ResourceNotFoundException("User ID '" + userId + "' not found in the database.");
		}

		// 2. Get the user_name of the recruiter
		String assignedBy = requirementsDao.findUserNameByUserId(userId);

		// 3. Get current month start and end datetime
		LocalDate today = LocalDate.now();
		LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

		// 4. Fetch requirements
		List<RequirementsModel> requirements = requirementsDao.findJobsAssignedByName(assignedBy, startOfMonth, endOfMonth);

		// 5. Logging
		logger.info("Fetched {} requirements for user ID '{}' (assigned_by='{}') for current month {} to {}",
				requirements.size(), userId, assignedBy, startOfMonth.toLocalDate(), endOfMonth.toLocalDate());

		// 6. Map to DTO
		return requirements.stream()
				.map(requirement -> {
					RequirementsDto dto = new RequirementsDto();

					dto.setJobId(requirement.getJobId());
					dto.setJobTitle(requirement.getJobTitle());
					dto.setClientName(requirement.getClientName());
					dto.setJobDescription(requirement.getJobDescription());
					dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());
					dto.setJobType(requirement.getJobType());
					dto.setLocation(requirement.getLocation());
					dto.setJobMode(requirement.getJobMode());
					dto.setExperienceRequired(requirement.getExperienceRequired());
					dto.setNoticePeriod(requirement.getNoticePeriod());
					dto.setRelevantExperience(requirement.getRelevantExperience());
					dto.setQualification(requirement.getQualification());
					dto.setSalaryPackage(requirement.getSalaryPackage());
					dto.setNoOfPositions(requirement.getNoOfPositions());
					dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
					dto.setRecruiterIds(requirement.getRecruiterIds());
					dto.setStatus(requirement.getStatus());
					dto.setAssignedBy(requirement.getAssignedBy());
					dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(requirement.getJobId()));
					dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(requirement.getJobId()));

					return dto;
				})
				.collect(Collectors.toList());
	}

	public List<RequirementsDto> getRequirementsByAssignedByAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
		// 1. Validate date range
		if (startDate == null || endDate == null) {
			throw new DateRangeValidationException("Start date and End date must not be null.");
		}
		if (endDate.isBefore(startDate)) {
			throw new DateRangeValidationException("End date cannot be before start date.");
		}

		// 2. Check if user exists
		int userExists = requirementsDao.countByUserId(userId);
		if (userExists == 0) {
			logger.warn("User ID '{}' not found in the database", userId);
			throw new ResourceNotFoundException("User ID '" + userId + "' not found in the database.");
		}

		// 3. Get userName from userId
		String assignedBy = requirementsDao.findUserNameByUserId(userId);

		// 4. Prepare date range
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

		// 5. Fetch data
		List<RequirementsModel> requirements = requirementsDao.findJobsAssignedByNameAndDateRange(
				assignedBy, startDateTime, endDateTime
		);

		// 6. Handle empty result
		if (requirements.isEmpty()) {
			throw new NoJobsAssignedToRecruiterException("No requirements found for userId '" + userId +
					"' between " + startDate + " and " + endDate);
		}

		// 7. Map to DTOs
		List<RequirementsDto> dtoList = requirements.stream().map(requirement -> {
			RequirementsDto dto = new RequirementsDto();

			dto.setJobId(requirement.getJobId());
			dto.setJobTitle(requirement.getJobTitle());
			dto.setClientName(requirement.getClientName());
			dto.setJobDescription(requirement.getJobDescription());
			dto.setJobDescriptionBlob(requirement.getJobDescriptionBlob());
			dto.setJobType(requirement.getJobType());
			dto.setLocation(requirement.getLocation());
			dto.setJobMode(requirement.getJobMode());
			dto.setExperienceRequired(requirement.getExperienceRequired());
			dto.setNoticePeriod(requirement.getNoticePeriod());
			dto.setRelevantExperience(requirement.getRelevantExperience());
			dto.setQualification(requirement.getQualification());
			dto.setSalaryPackage(requirement.getSalaryPackage());
			dto.setNoOfPositions(requirement.getNoOfPositions());
			dto.setRequirementAddedTimeStamp(requirement.getRequirementAddedTimeStamp());
			dto.setRecruiterIds(requirement.getRecruiterIds());
			dto.setStatus(requirement.getStatus());
			dto.setAssignedBy(requirement.getAssignedBy());
			dto.setNumberOfSubmissions(requirementsDao.getNumberOfSubmissionsByJobId(requirement.getJobId()));
			dto.setNumberOfInterviews(requirementsDao.getNumberOfInterviewsByJobId(requirement.getJobId()));

			return dto;
		}).collect(Collectors.toList());

		// 8. Log and return
		logger.info("✅ Fetched {} requirements assigned by '{}' (userId: {}) between {} and {}",
				dtoList.size(), assignedBy, userId, startDate, endDate);

		return dtoList;
	}

	public CandidateStatsResponse getCandidateStatsDateFilter(LocalDate startDate, LocalDate endDate) {
		List<UserStatsDTO> userStatsList = new ArrayList<>();

		// 👤 Employee Stats
		List<Tuple> employeeStats = requirementsDao.getEmployeeCandidateStats(startDate, endDate);
		userStatsList.addAll(employeeStats.stream()
				.map(tuple -> {
					UserStatsDTO dto = new UserStatsDTO();
					dto.setEmployeeId(tuple.get("employeeId", String.class));
					dto.setEmployeeName(tuple.get("employeeName", String.class));
					dto.setEmployeeEmail(tuple.get("employeeEmail", String.class));
					dto.setRole("Employee");

					dto.setNumberOfClients(convertToInt(tuple.get("numberOfClients")));
					dto.setNumberOfRequirements(convertToInt(tuple.get("numberOfRequirements")));
					dto.setNumberOfSubmissions(convertToInt(tuple.get("numberOfSubmissions")));
					dto.setNumberOfInterviews(convertToInt(tuple.get("numberOfInterviews")));
					dto.setNumberOfPlacements(convertToInt(tuple.get("numberOfPlacements")));

					return dto;
				}).collect(Collectors.toList())
		);

		// 👨‍🏫 Teamlead Stats
		List<Tuple> teamleadStats = requirementsDao.getTeamleadCandidateStats(startDate, endDate);
		userStatsList.addAll(teamleadStats.stream()
				.map(tuple -> {
					UserStatsDTO dto = new UserStatsDTO();
					dto.setEmployeeId(tuple.get("employeeId", String.class));
					dto.setEmployeeName(tuple.get("employeeName", String.class));
					dto.setEmployeeEmail(tuple.get("employeeEmail", String.class));
					dto.setRole("Teamlead");

					dto.setNumberOfClients(convertToInt(tuple.get("numberOfClients")));
					dto.setNumberOfRequirements(convertToInt(tuple.get("numberOfRequirements")));
					dto.setSelfSubmissions(convertToInt(tuple.get("selfSubmissions")));
					dto.setSelfInterviews(convertToInt(tuple.get("selfInterviews")));
					dto.setSelfPlacements(convertToInt(tuple.get("selfPlacements")));
					dto.setTeamSubmissions(convertToInt(tuple.get("teamSubmissions")));
					dto.setTeamInterviews(convertToInt(tuple.get("teamInterviews")));
					dto.setTeamPlacements(convertToInt(tuple.get("teamPlacements")));

					return dto;
				}).collect(Collectors.toList())
		);

		return new CandidateStatsResponse(userStatsList);
	}

	public CandidateResponseDTO getCandidateDataWithDateRange(String userId, LocalDate startDate, LocalDate endDate) {
		log.info("🔍 Fetching candidate data with date range for userId: {} | {} to {}", userId, startDate, endDate);

		// 1️⃣ Fetch role and username
		Tuple roleInfo = requirementsDao.getUserRoleAndUsername(userId);
		String role = roleInfo.get("role", String.class);
		String username = roleInfo.get("userName", String.class);
		log.info("✅ Retrieved role '{}' and username '{}' for userId: {}", role, username, userId);

		List<SubmittedCandidateDTO> submittedCandidates;
		List<InterviewScheduledDTO> scheduledInterviews;
		List<JobDetailsDTO> jobDetails;
		List<PlacementDetailsDTO> placementDetails;
		List<ClientDetailsDTO> clientDetails;
		List<Tuple> employeeDetailsTuples;

		// 2️⃣ Fetch date-filtered data based on role
		if ("Teamlead".equalsIgnoreCase(role)) {
			log.info("🧩 Teamlead role detected. Fetching data assigned by username: {} with date range.", username);
			submittedCandidates = requirementsDao.findSubmittedCandidatesByAssignedByAndDateRange(username, startDate, endDate);
			scheduledInterviews = requirementsDao.findScheduledInterviewsByAssignedByAndDateRange(username, startDate, endDate);
			jobDetails = requirementsDao.findJobDetailsByAssignedByAndDateRange(username, startDate, endDate);
			placementDetails = requirementsDao.findPlacementCandidatesByAssignedByAndDateRange(username, startDate, endDate);
			clientDetails = requirementsDao.findClientDetailsByAssignedByAndDateRange(username, startDate, endDate);
			employeeDetailsTuples = requirementsDao.getTeamleadDetailsByUserId(userId); // no date filter
		} else {
			log.info("🧩 Employee role detected. Fetching data by userId: {} with date range.", userId);
			submittedCandidates = requirementsDao.findSubmittedCandidatesByUserIdAndDateRange(userId, startDate, endDate);
			scheduledInterviews = requirementsDao.findScheduledInterviewsByUserIdAndDateRange(userId, startDate, endDate);
			jobDetails = requirementsDao.findJobDetailsByUserIdAndDateRange(userId, startDate, endDate);
			placementDetails = requirementsDao.findPlacementCandidatesByUserIdAndDateRange(userId, startDate, endDate);
			clientDetails = requirementsDao.findClientDetailsByUserIdAndDateRange(userId, startDate, endDate);
			employeeDetailsTuples = requirementsDao.getEmployeeDetailsByUserId(userId); // no date filter
		}

		// 3️⃣ Group the data
		Map<String, List<SubmittedCandidateDTO>> groupedSubmissions = groupByClientName(submittedCandidates);
		Map<String, List<InterviewScheduledDTO>> groupedInterviews = groupByClientName(scheduledInterviews);
		Map<String, List<PlacementDetailsDTO>> groupedPlacements = groupByClientName(placementDetails);
		Map<String, List<JobDetailsDTO>> groupedJobDetails = groupByClientName(jobDetails);
		Map<String, List<ClientDetailsDTO>> groupedClientDetails = groupByClientName(clientDetails);

		// 4️⃣ Map employee details (not date dependent)
		List<EmployeeDetailsDTO> employeeDetails = mapEmployeeDetailsTuples(employeeDetailsTuples);

		// 🔢 Logging total counts
		log.info("📊 Filtered Submitted Candidates: {}", submittedCandidates.size());
		log.info("📊 Filtered Scheduled Interviews: {}", scheduledInterviews.size());
		log.info("📊 Filtered Job Details: {}", jobDetails.size());
		log.info("📊 Filtered Placement Details: {}", placementDetails.size());
		log.info("📊 Filtered Client Details: {}", clientDetails.size());

		// 5️⃣ Return compiled DTO
		log.info("✅ Successfully fetched and compiled date-filtered candidate data for userId: {}", userId);

		return new CandidateResponseDTO(
				groupedSubmissions,
				groupedInterviews,
				groupedPlacements,
				groupedJobDetails,
				groupedClientDetails,
				employeeDetails
		);
	}

	public List<InProgressRequirementDTO> getInProgressRequirements(LocalDate startDate, LocalDate endDate) {
		log.info("🔍 Fetching 'In Progress' requirements between {} and {}", startDate, endDate);

		boolean isToday = startDate.equals(endDate) && startDate.equals(LocalDate.now());

		List<Object[]> results = requirementsDao.findInProgressRequirementsByDateRange(startDate, endDate, isToday);
		log.debug("✅ Raw DB results fetched: {}", results.size());

		List<InProgressRequirementDTO> dtos = new ArrayList<>();

		for (Object[] row : results) {
			try {
				InProgressRequirementDTO dto = mapRowToDTO(row);
				if (dto != null) {
					dtos.add(dto);
				}
			} catch (Exception ex) {
				log.error("❌ Error mapping row to DTO: {}", Arrays.toString(row), ex);
			}
		}

		log.info("✅ Successfully mapped {} simplified In Progress requirements.", dtos.size());

		dtos.sort(
				Comparator.comparing(
						InProgressRequirementDTO::getUpdatedDateTime,
						Comparator.nullsLast(Comparator.reverseOrder())
				)
		);

		return dtos;
	}

	private InProgressRequirementDTO mapRowToDTO(Object[] row) {
		String recruiterId = (String) row[0];
		String recruiterName = (String) row[1];
		String jobId = (String) row[2];
		String clientName = (String) row[3];
		String bdmName = (String) row[4];
		String teamlead = (String) row[5];
		String technologies = (String) row[6];
		Object rawPostedDate = row[7];
		Object rawUpdatedDateTime = row[8];
		Object rawNumberOfSubmissions = row[9];
		Object rawNumberOfScreenReject = row[10];
		String jobTitle = (String) row[11];
		String jobMode = (String) row[12];
		String jobType = (String) row[13];
		String experienceRequired = (String) row[14];
		String relevantExperience = (String) row[15];


		LocalDate postedDate = parseToLocalDate(rawPostedDate);
		LocalDateTime updatedDateTime = parseToLocalDateTime(rawUpdatedDateTime);

		// Optional: Convert to IST (only if needed — if DB stores UTC timestamps)
		if (updatedDateTime != null) {
			updatedDateTime = updatedDateTime.atZone(ZoneId.of("UTC"))
					.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
					.toLocalDateTime();
		}

		long numberOfSubmissions = rawNumberOfSubmissions != null ? ((Number) rawNumberOfSubmissions).longValue() : 0;
		long numberOfScreenReject = rawNumberOfScreenReject != null ? ((Number) rawNumberOfScreenReject).longValue() : 0;

		return new InProgressRequirementDTO(
				recruiterId,
				recruiterName,
				jobId,
				clientName,
				bdmName,
				teamlead,
				technologies,
				postedDate,
				updatedDateTime,
				numberOfSubmissions,
				numberOfScreenReject,
				jobTitle,
				jobMode,
				jobType,
				experienceRequired,
				relevantExperience
		);
	}

	private LocalDate parseToLocalDate(Object obj) {
		if (obj instanceof String str) {
			try {
				return LocalDate.parse(str);
			} catch (DateTimeParseException e) {
				log.warn("⚠️ Error parsing LocalDate: {}", str, e);
			}
		} else if (obj instanceof LocalDate date) {
			return date;
		}
		return null;
	}

	private LocalDateTime parseToLocalDateTime(Object obj) {
		if (obj instanceof String str) {
			try {
				return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			} catch (DateTimeParseException e) {
				log.warn("⚠️ Error parsing LocalDateTime: {}", str, e);
			}
		} else if (obj instanceof LocalDateTime dateTime) {
			return dateTime;
		}
		return null;
	}


	public String sendInProgressEmail(String userId, List<InProgressRequirementDTO> requirements) {
		String recruiterName;

		// ✅ If userId is null or "null", treat it as request for all recruiters
		if (userId == null || "null".equalsIgnoreCase(userId)) {
			recruiterName = "All Recruiters";

			// 🔁 If no requirements provided, fetch from DB
			if (requirements == null) {
				LocalDate today = LocalDate.now();
				requirements = getInProgressRequirements(today, today);
			}

		} else {
			recruiterName = requirementsDao.findUserNameByUserId(userId);
			if (recruiterName == null || recruiterName.isEmpty()) {
				throw new UserNotFoundException("No User Found with User Id: " + userId);
			}

			if (requirements == null) {
				throw new IllegalArgumentException("❌ Recruiter-specific email requires data in request body.");
			}

			// 🔍 Filter only this recruiter's data
			requirements = requirements.stream()
					.filter(r -> userId.equals(r.getRecruiterId()))
					.collect(Collectors.toList());
		}

		if (requirements == null || requirements.isEmpty()) {
			return "⚠️ No InProgress data found for: " + recruiterName;
		}

		logger.info("📨 Preparing to send InProgress email for: {}", recruiterName);

		requirements.sort(Comparator
				.comparing((InProgressRequirementDTO dto) ->
						Optional.ofNullable(dto.getTeamlead()).orElse("zzzzzz"), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(InProgressRequirementDTO::getUpdatedDateTime,
						Comparator.nullsLast(Comparator.reverseOrder()))
		);

		String subject = "InProgress Stats - " + recruiterName + " " + LocalDate.now();

		String html = (userId == null || "null".equalsIgnoreCase(userId))
				? buildTeamleadWiseEmail(requirements)
				: buildRecruiterWiseEmail(requirements, recruiterName);

		List<String> recipients = requirementsDao.findEmailsByDesignationIgnoreCase("director");

		if (recipients.isEmpty()) {
			throw new RuntimeException("No recipients found with designation = 'director'");
		}

		for (String email : recipients) {
			emailService.sendEmail(email, subject, html);
		}
		return "✅ Email Sent Successfully for: " + recruiterName;
	}

	private String buildTeamleadWiseEmail(List<InProgressRequirementDTO> requirements) {
		StringBuilder sb = new StringBuilder();

		sb.append("<h2>In Progress Submission Report</h2>");
		sb.append("<p><strong>Generated For:</strong> All Recruiters</p>");
		long distinctJobCount = requirements.stream()
				.map(InProgressRequirementDTO::getJobId)
				.filter(Objects::nonNull)
				.distinct()
				.count();

		long totalSubmissionCount = requirements.stream()
				.map(InProgressRequirementDTO::getNumberOfSubmissions)
				.filter(Objects::nonNull)
				.mapToLong(Long::longValue)
				.sum();

		sb.append("<p><strong>Total Jobs:</strong> ").append(distinctJobCount).append("</p><br>");
		sb.append("<p><strong>Total Submissions:</strong> ").append(totalSubmissionCount).append("</p><br>");

		List<InProgressRequirementDTO> filtered = requirements.stream()
				.filter(r -> r.getTeamlead() != null && !r.getTeamlead().isBlank())
				.toList();

		Map<String, List<InProgressRequirementDTO>> groupedByTeamlead =
				filtered.stream().collect(Collectors.groupingBy(InProgressRequirementDTO::getTeamlead));

		List<String> sortedTeamleads = new ArrayList<>(groupedByTeamlead.keySet());
		sortedTeamleads.sort(String.CASE_INSENSITIVE_ORDER);

		for (String teamlead : sortedTeamleads) {
			List<InProgressRequirementDTO> teamleadRequirements = groupedByTeamlead.get(teamlead);

			long distinctRecruiters = teamleadRequirements.stream()
					.map(InProgressRequirementDTO::getRecruiterId)
					.filter(Objects::nonNull)
					.distinct()
					.count();

			long distinctJobs = teamleadRequirements.stream()
					.map(InProgressRequirementDTO::getJobId)
					.filter(Objects::nonNull)
					.distinct()
					.count();

			long totalSubmissions = teamleadRequirements.stream()
					.mapToLong(InProgressRequirementDTO::getNumberOfSubmissions)
					.sum();

			sb.append("<h3>Team Lead: ").append(teamlead).append("</h3>");
			sb.append("<p>👤 Recruiters: ").append(distinctRecruiters)
					.append(" | 📌 Jobs: ").append(distinctJobs)
					.append(" | 📥 Submissions: ").append(totalSubmissions)
					.append("</p>");

			sb.append("<table style='border-collapse: collapse; width: 100%; margin-bottom: 30px;' border='1' cellspacing='0' cellpadding='8'>");
			sb.append("<thead style='background-color: #f2f2f2;'>")
					.append("<tr>")
					.append("<th>Recruiter</th>")
					.append("<th>BDM</th>")
					.append("<th>Job ID</th>")
					.append("<th>Client</th>")
					.append("<th>Job Title</th>")
					.append("<th>Job Mode</th>")
					.append("<th>Job Type</th>")
					.append("<th>Experience Required</th>")
					.append("<th>Relevant Experience</th>")
					.append("<th>Technologies</th>")
					.append("<th>Submissions</th>")
					.append("</tr>")
					.append("</thead><tbody>");

			for (InProgressRequirementDTO req : teamleadRequirements) {
				sb.append("<tr>")
						.append("<td>").append(Optional.ofNullable(req.getRecruiterName()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getBdm()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getJobId()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getClientName()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getJobTitle()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getJobMode()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getJobType()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getExperienceRequired()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getRelevantExperience()).orElse("-")).append("</td>")
						.append("<td>").append(Optional.ofNullable(req.getTechnology()).orElse("-")).append("</td>")
						.append("<td>").append(req.getNumberOfSubmissions()).append("</td>")
						.append("</tr>");
			}

			sb.append("</tbody></table>");
		}

		return sb.toString();
	}
	private String buildRecruiterWiseEmail(List<InProgressRequirementDTO> requirements, String recruiterName) {
		StringBuilder sb = new StringBuilder();

		sb.append("<h3>In Progress Submission Report for Recruiter: ").append(recruiterName).append("</h3>");
		sb.append("<p>Total Jobs: ").append(requirements.size()).append("</p>");

		sb.append("<table style='border-collapse: collapse; width: 100%;' border='1' cellspacing='0' cellpadding='8'>");
		sb.append("<thead style='background-color: #f2f2f2;'>");
		sb.append("<tr>")
				.append("<th>BDM</th>")
				.append("<th>Team Lead</th>")
				.append("<th>Job ID</th>")
				.append("<th>Client</th>")
				.append("<th>Job Title</th>")
				.append("<th>Job Mode</th>")
				.append("<th>Job Type</th>")
				.append("<th>Experience Required</th>")
				.append("<th>Relevant Experience</th>")
				.append("<th>Technologies</th>")
				.append("<th>Submissions</th>")
				.append("</tr>");
		sb.append("</thead><tbody>");

		for (InProgressRequirementDTO req : requirements) {
			sb.append("<tr>")
					.append("<td>").append(Optional.ofNullable(req.getBdm()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getTeamlead()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getJobId()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getClientName()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getJobTitle()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getJobMode()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getJobType()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getExperienceRequired()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getRelevantExperience()).orElse("-")).append("</td>")
					.append("<td>").append(Optional.ofNullable(req.getTechnology()).orElse("-")).append("</td>")
					.append("<td>").append(req.getNumberOfSubmissions()).append("</td>")
					.append("</tr>");
		}

		sb.append("</tbody></table>");
		return sb.toString();
	}

}