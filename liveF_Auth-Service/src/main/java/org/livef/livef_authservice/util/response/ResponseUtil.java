package org.livef.livef_authservice.util.response;

import org.livef.livef_authservice.util.dto.ResponseData;
import org.springframework.stereotype.Component;


@Component
public class ResponseUtil {
	
	public ResponseData getResponseData(Object data, String message, String code) {
		ResponseData result = ResponseData.builder()
										  .data(data)
										  .message(message)
										  .code(code)
										  .build();
		return result;
	}
	
	
	public ResponseData getResponseData(String message, String code) {
		ResponseData result = ResponseData.builder()
										  .message(message)
										  .code(code)
										  .build();
		return result;
	}
}