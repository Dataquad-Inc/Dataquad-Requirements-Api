package com.dataquadinc.client;

import com.dataquadinc.dto.ApiResponse;
import com.dataquadinc.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user",
url = "${user.microservice.url}",
configuration = FeignClientConfiguration.class)
public interface UserFeignClient {


    @GetMapping("/users/allUsers")
    ApiResponse<List<UserDto>> getAllUsers();

    @GetMapping("users/user/{userId}")
    ResponseEntity<ApiResponse<UserDto>> getUserByUserID(@PathVariable String userId);
}
