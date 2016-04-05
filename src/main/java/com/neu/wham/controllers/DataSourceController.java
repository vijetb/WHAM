package com.neu.wham.controllers;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


//import com.neu.wham.exceptions.InvalidDateTimeException;
import com.neu.wham.exceptions.LocationException;
import com.neu.wham.model.Event;
import com.neu.wham.services.GetEventService;
//import com.neu.wham.validations.DatesValidation;
import com.neu.wham.validations.KeywordValidation;
import com.neu.wham.validations.LocationValidation;


@Controller
public class DataSourceController {
	@Autowired
	private GetEventService getEventService;
	 
	 @RequestMapping(value = "*", method = RequestMethod.GET)
	 @ResponseStatus(value = HttpStatus.NOT_FOUND)
     public @ResponseBody String secondRequest(){
            System.out.println("Hitting Second Request: not three parameters");
            return "{Error: Coordinates are not valid. Please specify one latitude, one longitude, and one radius}";
        }
	
	@RequestMapping(value = "/datasource/{lat}/{lon}/{rad}", method = RequestMethod.GET)
	public @ResponseBody List<Event> firstRequest(@PathVariable String lat, @PathVariable String lon, 
			@PathVariable String rad, @RequestParam(required=false) String q, 
			@RequestParam(required=false) String start,
			@RequestParam(required=false) String end,
			@RequestParam(required=false) String userId) throws LocationException {
			//@RequestParam(required=false) String end) throws LocationException, InvalidDateTimeException{

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("lat", LocationValidation.validateLatitude(lat));
		params.put("lon", LocationValidation.validateLongitude(lon));
		params.put("rad", LocationValidation.validateRadius(rad));
		params.put("q", KeywordValidation.validateKeyword(q));
		//params.put("statDT", DatesValidation.validateDateTime(start));
		//params.put("endDT", DatesValidation.validateDateTime(end));
		params.put("userId", userId);  // TODO:  validate userId
		
		return getEventService.getEvents(params);
	}
   
}
