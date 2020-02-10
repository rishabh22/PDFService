package nic.oad.pdfservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;


@Controller
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String responseMessage = "!!!Oops Something bad happened, please contact Administrator.";
        int statusCode =HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            logger.debug("Status Code: " + status);

            if (statusCode == HttpStatus.BAD_REQUEST.value()) { //400
                responseMessage = "Invalid Request";
                //return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                //return "errors/error-400";
            } else if (statusCode == HttpStatus.NOT_IMPLEMENTED.value()) { //501

                //return "errors/error-501";
            } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) { //405
                responseMessage = "Invalid Request";
                //return "errors/error";
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) { //404
                responseMessage = "Requested Page Not Found";
                //return "errors/error-404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) { //500
                //return "errors/error-500";
            }
        }
        //return "error";
        return new ResponseEntity<>(responseMessage, HttpStatus.valueOf(statusCode));
    }

    @Override
    public String getErrorPath() {

        return "/error";
    }
}
