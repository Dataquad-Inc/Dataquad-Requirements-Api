package com.dataquadinc.client;


import com.dataquadinc.exceptions.ErrorResponseClient;
import com.dataquadinc.exceptions.FeignClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CustomErrorDecoder implements ErrorDecoder {

    private static final Logger logger=LoggerFactory.getLogger(CustomErrorDecoder.class);
    private static final ObjectMapper objectMapper=new ObjectMapper();
    @Override
    public Exception decode(String methodKey, Response response) {

      try{
          logger.info("Feign Error URL : {}",response.request().url());
          logger.info("Feign Error Status : {}",response.status());
          logger.info("Feign Body Is null : {}",response.body()==null);

          ErrorResponseClient errorResponse=objectMapper.readValue(
                  response.body().asInputStream(),
                  ErrorResponseClient.class
          );
          return new FeignClientException(errorResponse.getError().getErrorCode(),errorResponse.getError().getErrorMessage(), errorResponse.getMessage());
      }catch (IOException e){
        throw new RuntimeException("Failed To Parse Error Response",e);
      }
    }
}
