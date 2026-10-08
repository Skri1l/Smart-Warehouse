package com.skr1l.backend.vision;

import com.skr1l.backend.vision.dto.VisionResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VisionClient {

  private final RestClient restClient;

  public VisionClient(@Qualifier("visionRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  public VisionResponseDto recognize(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is empty");
    }

    if (file.getSize() > 10L * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 10 MB");
    }

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

    body.add("file", file.getResource());

    VisionResponseDto response;

    try {
      response = restClient.post()
        .uri("/recognize")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(VisionResponseDto.class);
    } catch (RestClientException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Vision service request failed", exception);
    }

    if (response == null || response.count() == null || response.count() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Vision service returned an invalid count");
    }

    return response;
  }
}
