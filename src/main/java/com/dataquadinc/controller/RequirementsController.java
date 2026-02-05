package com.dataquadinc.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.dataquadinc.client.UserFeignClient;
import com.dataquadinc.dto.*;
import com.dataquadinc.exceptions.*;
import com.dataquadinc.model.RequirementsModel;
import com.dataquadinc.repository.RequirementsDao;
import com.dataquadinc.service.EmailService;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dataquadinc.service.RequirementsService;
import org.springframework.web.multipart.MultipartFile;


@CrossOrigin(origins = {"http://35.188.150.92", "http://192.168.0.140:3000", "http://192.168.0.139:3000","https://mymulya.com","http://localhost:3000",
		"http://192.168.0.135:80/","http://192.168.0.135/","http://182.18.177.16:443","http://mymulya.com:443","http://localhost/",
		"http://154.210.288.26/",
		"http://192.168.0.203:3000/",
		"http://192.168.0.167:3000/"})
@RestController
	@RequestMapping("/requirements")
//@CrossOrigin("*")
public class RequirementsController {

	@Autowired
	private RequirementsService service;

	@Autowired
	private EmailService emailService ;

	@Autowired
	UserFeignClient userFeignClient;

	@Autowired
	private RequirementsDao requirementsDao;

	private static final Logger logger = LoggerFactory.getLogger(RequirementsController.class);

	@PostMapping("/assignJob")
	public ResponseEntity<ResponseBean> addRequirement(
			@RequestParam("jobTitle") String jobTitle,
			@RequestParam("clientName") String clientName,
			@RequestParam(value = "jobDescription", required = false) String jobDescription,
			@RequestParam(value = "jobDescriptionFile", required = false) MultipartFile jobDescriptionFile,
			@RequestParam("jobType") String jobType,
			@RequestParam("location") String location,
			@RequestParam("jobMode") String jobMode,
			@RequestParam("experienceRequired") String experienceRequired,
			@RequestParam("noticePeriod") String noticePeriod,
			@RequestParam("relevantExperience") String relevantExperience,
			@RequestParam("qualification") String qualification,
			@RequestParam(value = "salaryPackage", required = false) String salaryPackage,
			@RequestParam("noOfPositions") int noOfPositions,
			@RequestParam("recruiterIds") Set<String> recruiterIds,
			@RequestParam(value = "recruiterName", required = false) Set<String> recruiterName,
			@RequestParam("assignedBy") String assignedBy,
			@RequestParam(value = "assignedTo", required = false) String assignedTo // ✅ NEW
	) throws IOException {
		try {
			// Validate that only one of jobDescription or jobDescriptionFile is provided
			if ((jobDescription != null && !jobDescription.isEmpty()) &&
					(jobDescriptionFile != null && !jobDescriptionFile.isEmpty())) {
				// Return error if both are provided
				return ResponseEntity.badRequest()
						.body(ResponseBean.errorResponse("You can either provide a job description text or upload a job description file, but not both.", "Bad Request"));
			}

			// Create RequirementsDto from request parameters
			RequirementsDto requirementsDto = new RequirementsDto();
			requirementsDto.setJobTitle(jobTitle);
			requirementsDto.setClientName(clientName);

			// Logic to set job description
			String finalJobDescription = null;
			byte[] jobDescriptionBlob = null;

			// If jobDescriptionFile is provided, process the file
			if (jobDescriptionFile != null && !jobDescriptionFile.isEmpty()) {
				// Convert the file content to byte array (BLOB)
				jobDescriptionBlob = jobDescriptionFile.getBytes(); // Save the file as a BLOB
				// Set jobDescription to null since we are using the file (BLOB)
				finalJobDescription = null;
			}
			// If jobDescription (text) is provided, use it
			else if (jobDescription != null && !jobDescription.isEmpty()) {
				finalJobDescription = jobDescription;
				// Set jobDescriptionBlob to null since we are using the text
				jobDescriptionBlob = null;
			}

			// Set the job description (from file or text)
			requirementsDto.setJobDescription(finalJobDescription);
			requirementsDto.setJobDescriptionBlob(jobDescriptionBlob); // Set the BLOB (or null)


			// Set the other fields
			requirementsDto.setJobType(jobType);
			requirementsDto.setLocation(location);
			requirementsDto.setJobMode(jobMode);
			requirementsDto.setExperienceRequired(experienceRequired);
			requirementsDto.setNoticePeriod(noticePeriod);
			requirementsDto.setRelevantExperience(relevantExperience);
			requirementsDto.setQualification(qualification);
			requirementsDto.setSalaryPackage(salaryPackage);
			requirementsDto.setNoOfPositions(noOfPositions);
			requirementsDto.setRecruiterIds(recruiterIds);
			requirementsDto.setRecruiterName(recruiterName);

			// ✅ AssignedBy logic using assignedTo
			if (assignedTo != null && !assignedTo.isBlank()) {
				requirementsDto.setAssignedBy(assignedTo);
			} else {
				requirementsDto.setAssignedBy(assignedBy);
			}

			// Call the service to create the requirement
			RequirementAddedResponse response = service.createRequirement(requirementsDto);

			// Return success response
			return ResponseEntity.status(HttpStatus.CREATED).body(ResponseBean.successResponse("Requirement added successfully", response));

		} catch (IllegalArgumentException e) {
			// Handle specific validation error
			return ResponseEntity.badRequest().body(ResponseBean.errorResponse(e.getMessage(), "Bad Request"+e.getMessage()));
		} catch (Exception e) {
			// General exception handling
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseBean.errorResponse("Unexpected error occurred: " + e.getMessage(), "Internal Server Error"+e.getMessage()));
		}
	}

	@GetMapping("/download-job-description/{jobId}")
	public ResponseEntity<Object> downloadJobDescription(@PathVariable String jobId) {
		try {
			logger.info("Downloading job description for job ID: {}", jobId);

			// Fetch the requirement details from the database
			RequirementsModel requirement = requirementsDao.findById(jobId)
					.orElseThrow(() -> new RequirementNotFoundException("Requirement not found with Job Id: " + jobId));

			logger.debug("Requirement found for job ID: {}", jobId);

			// Fetch the job description BLOB from the RequirementsModel entity
			byte[] jobDescriptionBytes = requirement.getJobDescriptionBlob();

			// Check if job description is available
			if (jobDescriptionBytes == null || jobDescriptionBytes.length == 0) {
				logger.error("Job description is missing for job ID: {}", jobId);
				// Return a ResponseBean with an error message
				ResponseBean errorResponse = new ResponseBean(false, "Job description is missing for job ID: " + jobId, "No file found", null);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
			}

			logger.debug("Job description fetched successfully for job ID: {}", jobId);

			// Dynamically set the filename based on the job title and file type
			String filename = requirement.getJobTitle() + "-JobDescription";

			// Use Apache Tika to detect the content type
			Tika tika = new Tika();
			String contentType = tika.detect(jobDescriptionBytes);  // Detects the content type of the byte array
			String fileExtension = ""; // Default extension will be empty to handle dynamic extension

			// Based on detected content type, set appropriate file extension
			if (contentType.equals("application/pdf")) {
				fileExtension = ".pdf";
			} else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
				fileExtension = ".docx";
			} else if (contentType.equals("text/plain")) {
				fileExtension = ".txt";
			} else if (contentType.equals("image/png")) {
				fileExtension = ".png";
			} else if (contentType.equals("image/jpeg")) {
				fileExtension = ".jpg";
			} else {
				// Log warning for unknown content types
				logger.warn("Unknown content type detected: {}", contentType);
			}

			// Append the detected file extension to the filename if available
			if (!fileExtension.isEmpty()) {
				filename += fileExtension;
			}

			// Convert the byte array to a ByteArrayResource for downloading
			ByteArrayResource resource = new ByteArrayResource(jobDescriptionBytes);

			// Return the file as a response for download with the proper content type and filename
			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(contentType))  // Set the content type based on detection
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.body(resource);

		} catch (RequirementNotFoundException e) {
			logger.error("Requirement not found: {}", e.getMessage());
			// Return a ResponseBean with the error message
			ResponseBean errorResponse = new ResponseBean(false, e.getMessage(), "Not Found", null);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

		} catch (Exception e) {
			logger.error("Unexpected error while downloading job description for job ID {}: {}", jobId, e.getMessage());
			// Return a ResponseBean with the error message
			ResponseBean errorResponse = new ResponseBean(false, "Unexpected error: " + e.getMessage(), "Internal Server Error", null);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}
	// Helper method to check if the byte array is a PDF (you could improve this with more detailed checks)
	private boolean isPdf(byte[] data) {
		return data != null && data.length > 4 && data[0] == '%'
				&& data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
	}

	// Helper method to check if the byte array is a DOCX file
	private boolean isDocx(byte[] data) {
		return data != null && data.length > 4 && data[0] == (byte) 0x50 && data[1] == (byte) 0x4B
				&& data[2] == (byte) 0x03 && data[3] == (byte) 0x04;
	}

	// Helper method to check if the byte array is a plain text file
	private boolean isTextFile(byte[] data) {
		// You can improve this check further (e.g., by checking for byte patterns in text files)
		return data != null && data.length > 0 && new String(data).matches(".*\\w.*");
	}
	// Helper method to check if the byte array is an XLSX file
	private boolean isXlsx(byte[] data) {
		return data != null && data.length > 4 && data[0] == (byte) 0x50 && data[1] == (byte) 0x4B
				&& data[2] == (byte) 0x03 && data[3] == (byte) 0x04;  // (ZIP signature for XLSX)
	}

	// Helper method to check if the byte array is a PowerPoint file
	private boolean isPptx(byte[] data) {
		return data != null && data.length > 4 && data[0] == (byte) 0x50 && data[1] == (byte) 0x4B
				&& data[2] == (byte) 0x03 && data[3] == (byte) 0x04;  // (ZIP signature for PPTX)
	}


	@GetMapping("/getAssignments")
	public ResponseEntity<?> getRequirements(
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@RequestParam(value = "search", required = false) String search) {
		
		// Validate pagination parameters
		if (page < 0) {
			return ResponseEntity.badRequest().body("Page number cannot be negative");
		}
		if (size <= 0 || size > 100) {
			return ResponseEntity.badRequest().body("Size must be between 1 and 100");
		}
		
		PagedResponse<RequirementsDto> pagedResponse = service.getRequirementsWithPaginationAndSearch(page, size, search);
		
		// Clean up recruiterName field for each requirement
		for (RequirementsDto dto : pagedResponse.getContent()) {
			if (dto.getRecruiterName() != null) {
				Set<String> cleanedNames = dto.getRecruiterName().stream()
						.map(name -> name.replaceAll("[\\[\\]\"]", ""))
						.collect(Collectors.toSet());
				dto.setRecruiterName(cleanedNames);
			}
		}
		
		return ResponseEntity.ok(pagedResponse);
	}

	@GetMapping("/getAllAssignments")
	public ResponseEntity<?> getAllRequirements() {
		List<RequirementsDto> requirements = (List<RequirementsDto>) service.getRequirementsDetails();

		// Clean up recruiterName field
		for (RequirementsDto dto : requirements) {
			if (dto.getRecruiterName() != null) {
				Set<String> cleanedNames = dto.getRecruiterName().stream()
						.map(name -> name.replaceAll("[\\[\\]\"]", ""))
						.collect(Collectors.toSet());
				dto.setRecruiterName(cleanedNames);
			}
		}

		return ResponseEntity.ok(requirements);
	}

	@GetMapping("/filterByDate")
	public ResponseEntity<?> getRequirementsByDateRange(
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@RequestParam(value = "search", required = false) String search) {

		// Validate pagination parameters
		if (page < 0) {
			return ResponseEntity.badRequest().body("Page number cannot be negative");
		}
		if (size <= 0 || size > 100) {
			return ResponseEntity.badRequest().body("Size must be between 1 and 100");
		}

		PagedResponse<RequirementsDto> pagedResponse = service.getRequirementsByDateRangeWithPaginationAndSearch(startDate, endDate, page, size, search);

		// Clean up recruiterName field for each requirement
		for (RequirementsDto dto : pagedResponse.getContent()) {
			if (dto.getRecruiterName() != null) {
				Set<String> cleanedNames = dto.getRecruiterName().stream()
						.map(name -> name.replaceAll("[\\[\\]\"]", ""))
						.collect(Collectors.toSet());
				dto.setRecruiterName(cleanedNames);
			}
		}

		logger.info("✅ Fetched {} requirements between {} and {}", pagedResponse.getContent().size(), startDate, endDate);
		return ResponseEntity.ok(pagedResponse);
	}


	@GetMapping("/get/{jobId}")
	public ResponseEntity<RequirementsDto> getRequirementById(@PathVariable String jobId) {
		return new ResponseEntity<>(service.getRequirementDetailsById(jobId), HttpStatus.OK);
	}

	@PutMapping("/assign")
	public ResponseEntity<?> assignRequirement(@RequestParam String jobId, @RequestParam List<String> recruiterIds) {
		try {
			// Retrieve the job requirement to check if the job exists
			RequirementsModel requirement = requirementsDao.findById(jobId)
					.orElseThrow(() -> new RequirementNotFoundException("Requirement Not Found with Id : " + jobId));

			// Flag to check if any error occurs before sending emails
			boolean allEmailsSentSuccessfully = true;

			// Convert List to Set and clean the recruiter IDs
			Set<String> cleanedRecruiterIds = recruiterIds.stream()
					.map(id -> id.replaceAll("[\"\\[\\]]", "").trim())
					.collect(Collectors.toSet());

			// Assign each recruiter to the job and send emails immediately
			for (String recruiterId : recruiterIds) {
				try {
					// Assign the recruiter and save email to DB
					service.assignToRecruiter(jobId, Collections.singleton(recruiterId));

					// Fetch the recruiter's email and name (assuming these methods exist in your service)
					String recruiterEmail = service.getRecruiterEmail(recruiterId);
					String recruiterName = service.getRecruiterName(recruiterId); // This should be available in the service

					// Log recruiter ID and fetched email
					logger.info("Fetched recruiterId: {} with email: {}", recruiterId, recruiterEmail);

					// Check if the email and name are not null or empty
					if (recruiterEmail == null || recruiterEmail.isEmpty()) {
						throw new RecruiterNotFoundException("Email for recruiter " + recruiterId + " not found");
					}



					// Prepare the email subject and body
					String subject = "New Job Assignment: " + requirement.getJobTitle();

					String body = "Dear " + recruiterName + ",<br><br>" +
							"I hope you are doing well.<br><br>" +
							"You have been assigned a new job requirement. Please find the details below:<br><br>" +
							"▶ <b>Job Title:</b> " + requirement.getJobTitle() + "<br>" +
							"▶ <b>Client:</b> " + requirement.getClientName() + "<br>" +
							"▶ <b>Location:</b> " + requirement.getLocation() + "<br>" +
							"▶ <b>Job Type:</b> " + requirement.getJobType() + "<br>" +
							"▶ <b>Experience Required:</b> " + requirement.getExperienceRequired() + " years<br>" +
							"▶ <b>Assigned By:</b> " + requirement.getAssignedBy() + "<br><br>" +
							"Please review the details and proceed with the necessary actions. Additional information is available on your dashboard.<br><br>" +
							"If you have any questions or need further clarification, feel free to reach out.<br><br>" +
							"Best regards,<br>" +
							"Dataquad";

					// Send email to the recruiter
					emailService.sendEmail(recruiterEmail, subject, body);

					// Log the email sending action
					logger.info("Email sent to recruiter {} at {} for job: {}", recruiterEmail, requirement.getJobTitle());

				} catch (Exception e) {
					// Log the full exception stack trace for better error diagnosis
					logger.error("Failed to send email to recruiter {} for job {}. Error: {}"+e.getMessage(), recruiterId, requirement.getJobTitle(), e.getMessage(), e);
					allEmailsSentSuccessfully = false;
				}
			}

			// Return success response after attempting to assign recruiters and send emails
			if (allEmailsSentSuccessfully) {
				return ResponseEntity.ok(ResponseBean.successResponse("Recruiters assigned and email notifications sent successfully.", null));
			} else {
				return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
						.body(ResponseBean.errorResponse("Some recruiter emails failed to send. Please check the logs for details.", null));
			}

		} catch (Exception e) {
			// Handle errors (e.g., if jobId doesn't exist)
			logger.error("Error assigning recruiters: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().body(ResponseBean.errorResponse("Error assigning recruiters: " + e.getMessage(), e.toString()));
		}
	}



	@PutMapping("/updateStatus")
	public ResponseEntity<Void> updateStatus(@RequestBody StatusDto status) {
		service.statusUpdate(status);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/recruiter/{recruiterId}")
	public ResponseEntity<List<RecruiterRequirementsDto>> getJobsByRecruiter(@PathVariable String recruiterId) {
		return new ResponseEntity<>(service.getJobsAssignedToRecruiter(recruiterId),HttpStatus.OK);
	}

	@GetMapping("/recruiter/{recruiterId}/filterByDate")
	public ResponseEntity<?> getJobsAssignedToRecruiterByDate(
			@PathVariable String recruiterId,
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		try {
			List<RecruiterRequirementsDto> result = service.getJobsAssignedToRecruiterByDate(recruiterId, startDate, endDate);

			// Log success
			logger.info("Fetched {} jobs successfully for recruiterId: {} between {} and {}", result.size(), recruiterId, startDate, endDate);
			return ResponseEntity.ok(result);

		} catch (NoJobsAssignedToRecruiterException ex) {
			// Log and return 404 with error message
			logger.error("No jobs found for recruiterId: {} between {} and {}", recruiterId, startDate, endDate,ex.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Collections.singletonMap("message", ex.getMessage()));

		} catch (Exception ex) {
			// Log and return 500 with generic error message
			logger.error("An error occurred while fetching jobs for recruiterId: {}", recruiterId, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Collections.singletonMap("message", "An internal error occurred while fetching recruiter jobs."+ex.getMessage()));
		}
	}


	@PutMapping("/updateRequirement/{jobId}")
	public ResponseEntity<ResponseBean> updateRequirement(
			@PathVariable String jobId,
			@RequestParam("jobTitle") String jobTitle,
			@RequestParam("clientName") String clientName,
			@RequestParam(value = "jobDescription", required = false) String jobDescription,
			@RequestParam(value = "jobDescriptionFile", required = false) MultipartFile jobDescriptionFile,
			@RequestParam("jobType") String jobType,
			@RequestParam("location") String location,
			@RequestParam("jobMode") String jobMode,
			@RequestParam("status") String status,
			@RequestParam("experienceRequired") String experienceRequired,
			@RequestParam("noticePeriod") String noticePeriod,
			@RequestParam("relevantExperience") String relevantExperience,
			@RequestParam("qualification") String qualification,
			@RequestParam(value = "salaryPackage", required = false) String salaryPackage,
			@RequestParam("noOfPositions") int noOfPositions,
			@RequestParam("recruiterIds") Set<String> recruiterIds,
			@RequestParam(value = "recruiterName", required = false) Set<String> recruiterName,
			@RequestParam("assignedBy") String assignedBy,
			@RequestParam(value = "assignedTo", required = false) String assignedTo
	) throws IOException {
		try {
			// Validate that only one of jobDescription or jobDescriptionFile is provided
			if ((jobDescription != null && !jobDescription.isEmpty()) &&
					(jobDescriptionFile != null && !jobDescriptionFile.isEmpty())) {
				return ResponseEntity.badRequest()
						.body(ResponseBean.errorResponse("You can either provide a job description text or upload a job description file, but not both.", "Bad Request"));
			}

			// Fetch the existing requirement
			RequirementsDto existingRequirement = service.getRequirementDetailsById(jobId);
			if (existingRequirement == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ResponseBean.errorResponse("Requirement not found for the provided jobId", "Not Found"));
			}

			// Debugging: Check the current status
			System.out.println("Existing status before update: " + existingRequirement.getStatus());

			// Set or nullify fields that are not being updated
			if (jobTitle != null && !jobTitle.isEmpty()) existingRequirement.setJobTitle(jobTitle);
			else existingRequirement.setJobTitle(null);

			// Check and update the status
			if (status != null && !status.isEmpty()) {
				existingRequirement.setStatus(status);
				System.out.println("Updated status to: " + status); // Debugging: Log updated status
			} else {
				existingRequirement.setStatus(null);
			}

			// Handle clientName and other fields similarly
			if (clientName != null && !clientName.isEmpty()) existingRequirement.setClientName(clientName);
			else existingRequirement.setClientName(null);

			// Logic to set job description and BLOB
			String finalJobDescription = null;
			byte[] jobDescriptionBlob = null;

			// If jobDescriptionFile is provided, process the file and set the BLOB
			if (jobDescriptionFile != null && !jobDescriptionFile.isEmpty()) {
				jobDescriptionBlob = jobDescriptionFile.getBytes(); // Save the file as a BLOB
				finalJobDescription = null; // Use the file (no text)
			}
			// If jobDescription (text) is provided, use it and set the BLOB to null
			else if (jobDescription != null && !jobDescription.isEmpty()) {
				finalJobDescription = jobDescription;
				jobDescriptionBlob = null; // Use the text (no file)
			}

			// Set the job description (from file or text)
			existingRequirement.setJobDescription(finalJobDescription);
			existingRequirement.setJobDescriptionBlob(jobDescriptionBlob); // Set the BLOB (or null)

			// Set the other fields and nullify any fields that are not being updated
			if (jobType != null && !jobType.isEmpty()) existingRequirement.setJobType(jobType);
			else existingRequirement.setJobType(null);

			if (location != null && !location.isEmpty()) existingRequirement.setLocation(location);
			else existingRequirement.setLocation(null);

			if (jobMode != null && !jobMode.isEmpty()) existingRequirement.setJobMode(jobMode);
			else existingRequirement.setJobMode(null);

			if (experienceRequired != null && !experienceRequired.isEmpty()) existingRequirement.setExperienceRequired(experienceRequired);
			else existingRequirement.setExperienceRequired(null);

			if (noticePeriod != null && !noticePeriod.isEmpty()) existingRequirement.setNoticePeriod(noticePeriod);
			else existingRequirement.setNoticePeriod(null);

			if (relevantExperience != null && !relevantExperience.isEmpty()) existingRequirement.setRelevantExperience(relevantExperience);
			else existingRequirement.setRelevantExperience(null);

			if (qualification != null && !qualification.isEmpty()) existingRequirement.setQualification(qualification);
			else existingRequirement.setQualification(null);

			if (salaryPackage != null && !salaryPackage.isEmpty()) existingRequirement.setSalaryPackage(salaryPackage);
			else existingRequirement.setSalaryPackage(null);

			if (noOfPositions > 0) existingRequirement.setNoOfPositions(noOfPositions);
			else existingRequirement.setNoOfPositions(0); // If noOfPositions is not updated, set to 0

			if (recruiterIds != null && !recruiterIds.isEmpty()) existingRequirement.setRecruiterIds(recruiterIds);
			else existingRequirement.setRecruiterIds(null);

			if (recruiterName != null && !recruiterName.isEmpty()) existingRequirement.setRecruiterName(recruiterName);
			else existingRequirement.setRecruiterName(null);

			// ✅ AssignedBy fallback from assignedTo
			if (assignedTo != null && !assignedTo.isBlank()) {
				existingRequirement.setAssignedBy(assignedTo);
			} else {
				existingRequirement.setAssignedBy(assignedBy);
			}

			// Call the service to update the requirement
			ResponseBean response = service.updateRequirementDetails(existingRequirement);

			// Debugging: Log the updated status after saving
			System.out.println("Status after saving: " + existingRequirement.getStatus());

			// Return success response
			return ResponseEntity.status(HttpStatus.OK).body(ResponseBean.successResponse("Requirement updated successfully", response));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ResponseBean.errorResponse(e.getMessage(), "Bad Request"+e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(ResponseBean.errorResponse("Unexpected error occurred: " + e.getMessage(), "Internal Server Error"+e.getMessage()));
		}
	}


	@DeleteMapping("/deleteRequirement/{jobId}")
	public ResponseEntity<ResponseBean> deleteRequirement(@PathVariable String jobId) {
		// Call the service method to delete the requirement by jobId
		ResponseBean response = service.deleteRequirementDetails(jobId);

		// Return the response entity with the appropriate status code
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	@GetMapping("/{jobId}")
	public ResponseEntity<RequirementDetailsDto> getRequirementDetailsByJobId(@PathVariable String jobId) {
		// Get requirement details by jobId
		RequirementDetailsDto recruiterDetails = service.getRequirementDetailsByJobId(jobId);

		if (recruiterDetails != null && recruiterDetails.getRequirement() != null) {

			// Clean recruiter names (Set<String>)
			if (recruiterDetails.getRequirement().getRecruiterName() != null) {
				Set<String> cleanedRecruiterNames = recruiterDetails.getRequirement().getRecruiterName().stream()
						.map(name -> name.replaceAll("[\\[\\]\"]", "").trim())
						.collect(Collectors.toSet());
				recruiterDetails.getRequirement().setRecruiterName(cleanedRecruiterNames);
			}

			// Clean assignedBy (String)
			if (recruiterDetails.getRequirement().getAssignedBy() != null) {
				String cleanedAssignedBy = recruiterDetails.getRequirement().getAssignedBy()
						.replaceAll("[\\[\\]\"]", "") // remove [ ] and "
						.trim();
				recruiterDetails.getRequirement().setAssignedBy(cleanedAssignedBy);
			}
		}

		if (recruiterDetails != null) {
			return ResponseEntity.ok(recruiterDetails);
		} else {
			return ResponseEntity.notFound().build();
		}
	}



	@GetMapping("/stats")
	public ResponseEntity<CandidateStatsResponse> getCandidateStats() {
		CandidateStatsResponse stats = service.getCandidateStatsLast30Days();
		return ResponseEntity.ok(stats);
	}
	@GetMapping("/coordinatorstats")
	public ResponseEntity<List<Coordinator_DTO>> getCountCoordinatestats(){
		List<Coordinator_DTO> stats= service.getCoordinatorStats();
		return ResponseEntity.ok(stats);
	}
	@GetMapping("/stats/filterByDate")
	public ResponseEntity<CandidateStatsResponse> getCandidateStats(
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
	) {
		CandidateStatsResponse stats = service.getCandidateStatsDateFilter(startDate,endDate);
		return ResponseEntity.ok(stats);
	}


	// Fetch both Submitted Candidates and Scheduled Interviews in one API call
	@GetMapping("/list/{userId}")
	public CandidateResponseDTO getCandidateData(@PathVariable String userId) {
		return service.getCandidateData(userId);
	}




	@GetMapping("/teamleadrequirements/{id}")
	public List<RequirementsDto> getRequirementsByAssignedBy(@PathVariable("id") String id) {
		List<RequirementsDto> requirements = service.getRequirementsByAssignedBy(id);

		// Clean recruiterName (if needed)
		for (RequirementsDto dto : requirements) {
			if (dto.getRecruiterName() != null) {
				Set<String> cleanedNames = dto.getRecruiterName().stream()
						.map(recruiter -> recruiter.replaceAll("[\\[\\]\"]", "").trim())
						.collect(Collectors.toSet());
				dto.setRecruiterName(cleanedNames);
			}
		}

		return requirements;
	}
	@GetMapping("/coordinatorRequirements/{id}")
	public List<RequirementsDto> getCoordinatorsRequirements(@PathVariable("id") String id) {

		// Fetch team assignments
		List<TeamAssignment> assignments = userFeignClient.getUserByUserID(id)
				.getBody()
				.getData()
				.getTeamAssignments();

		// Handle null/empty list
		if (assignments == null || assignments.isEmpty()) {
			return Collections.emptyList();
		}

		// Get first teamLeadId
		String teamLeadId = assignments.get(0).getTeamLeadId();

		// Fetch requirements
		List<RequirementsDto> requirements = service.getRequirementsByAssignedBy(teamLeadId);

		// Clean recruiterName (if needed)
		for (RequirementsDto dto : requirements) {
			if (dto.getRecruiterName() != null) {
				Set<String> cleanedNames = dto.getRecruiterName().stream()
						.map(recruiter -> recruiter.replaceAll("[\\[\\]\"]", "").trim())
						.collect(Collectors.toSet());
				dto.setRecruiterName(cleanedNames);
			}
		}

		return requirements;
	}



	@GetMapping("/teamleadrequirements/{userId}/filterByDate")
	public ResponseEntity<?> getRequirementsByAssignedByAndDateRange(
			@PathVariable String userId,
			@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		try {
			// Validate date range
			if (endDate.isBefore(startDate)) {
				logger.warn("End date {} is before start date {}", endDate, startDate);
				return ResponseEntity.badRequest()
						.body(Collections.singletonMap("message", "End date cannot be before start date"));
			}

			// Call service to get requirements
			List<RequirementsDto> requirements = service.getRequirementsByAssignedByAndDateRange(userId, startDate, endDate);

			// Clean recruiter names
			for (RequirementsDto dto : requirements) {
				if (dto.getRecruiterName() != null) {
					Set<String> cleanedNames = dto.getRecruiterName().stream()
							.map(r -> r.replaceAll("[\\[\\]\"]", "").trim())
							.collect(Collectors.toSet());
					dto.setRecruiterName(cleanedNames);
				}
			}

			// ✅ Log success at the end
			logger.info("Fetched {} requirements successfully for userId: {} between {} and {}", requirements.size(), userId, startDate, endDate);
			return ResponseEntity.ok(requirements);

		} catch (AssignedByNotFoundException ex) {
			logger.error("User ID not found: {} between {} and {}", userId, startDate, endDate,ex.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Collections.singletonMap("message", ex.getMessage()));

		} catch (Exception ex) {
			logger.error("An error occurred while fetching requirements for userId: {}", userId, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Collections.singletonMap("message", "An internal error occurred while fetching requirements."+ex.getMessage()));
		}
	}

	@GetMapping("/list/{userId}/filterByDate")
	public CandidateResponseDTO getCandidateDataWithDateRange(
			@PathVariable String userId,
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (startDate != null && endDate != null) {
			return service.getCandidateDataWithDateRange(userId, startDate, endDate);
		} else {
			return service.getCandidateData(userId);
		}
	}


	@GetMapping("/inprogress")
	public ResponseEntity<List<InProgressRequirementDTO>> getInProgressRequirements(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (startDate == null || endDate == null) {
			LocalDate today = LocalDate.now();
			startDate = today;
			endDate = today;
			logger.info("StartDate or EndDate not provided. Using today's date: {}", today);
		}

		List<InProgressRequirementDTO> result = service.getInProgressRequirements(startDate, endDate);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/inprogress/filterByDate")
	public ResponseEntity<List<InProgressRequirementDTO>> getInProgressRequirementsWithDateRange(
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (startDate != null && endDate != null) {
			logger.info("📅 Fetching In Progress requirements from {} to {}", startDate, endDate);
			return ResponseEntity.ok(service.getInProgressRequirements(startDate, endDate));
		} else {
			// Default to today if no dates provided
			LocalDate today = LocalDate.now();
			logger.info("📅 No date range provided. Fetching for today: {}", today);
			return ResponseEntity.ok(service.getInProgressRequirements(today, today));
		}
	}

	@PostMapping("/sendInprogressEmail/{userId}")
	public ResponseEntity<String> sendEmail(
			@PathVariable String userId,
			@RequestBody(required = false) List<InProgressRequirementDTO> dto) {

		if ("null".equalsIgnoreCase(userId)) {
			// Case 1: userId is "null" → fetch all data internally
			return ResponseEntity.ok(service.sendInProgressEmail(null, null));
		}

		if (dto == null || dto.isEmpty()) {
			return ResponseEntity.badRequest().body("❌ Request body is required for recruiter-specific email.");
		}

		// Case 2: userId present → use provided list
		return ResponseEntity.ok(service.sendInProgressEmail(userId, dto));
	}


}