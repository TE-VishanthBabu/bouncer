package com.zorsecyber.bouncer.webapp.response;

import org.springframework.http.HttpStatus;

import com.zorsecyber.bouncer.webapp.constant.Constant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponse {
    private String status = Constant.STATUS_SUCCESS;
    private Integer code = HttpStatus.OK.value();
    private String message;
    private Object data;
}
